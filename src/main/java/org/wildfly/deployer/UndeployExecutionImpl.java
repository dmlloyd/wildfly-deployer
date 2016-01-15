/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.deployer;

import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.unpark;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class UndeployExecutionImpl implements UndeployExecution {
    private final AtomicReference<State> stateRef;

    private final Executor executor;
    private final Dependency[] bottoms;
    private final String name;

    UndeployExecutionImpl(final DeployerChainImpl deployerChain, final Executor executor, final Dependency[] bottoms, final String name) {
        this.executor = executor;
        this.bottoms = bottoms;
        this.name = name;
        final int dependentCount = deployerChain.getInitialDeployers().size();
        stateRef = new AtomicReference<>(new RunningState(dependentCount));
    }

    void run() {
        for (Dependency bottom : bottoms) {
            bottom.dependentDone(executor);
        }
    }

    public String getDeploymentName() {
        return name;
    }

    public Executor getExecutor() {
        return executor;
    }

    public boolean isDone() {
        return stateRef.get().isDone();
    }

    public void await() throws InterruptedException {
        final AtomicReference<State> stateRef = this.stateRef;
        State oldState = stateRef.get();
        if (oldState.isDone()) {
            return;
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        final Thread thread = Thread.currentThread();
        if (! oldState.isWaitingFor(thread)) {
            // CAS in our thread
            State newState = new WaitingState(oldState, thread);
            while (! stateRef.compareAndSet(oldState, newState)) {
                oldState = stateRef.get();
                if (oldState.isDone()) {
                    return;
                }
                assert ! oldState.isWaitingFor(thread);
                newState = new WaitingState(oldState, thread);
            }
        }
        park(this);
        oldState = stateRef.get();
        while (! oldState.isDone()) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            park(this);
            oldState = stateRef.get();
        }
    }

    public UndeployResult getUndeployResult() throws IllegalStateException {
        return stateRef.get().getUndeployResult();
    }

    public <T, U> void onComplete(final BiConsumer<T, U> consumer, final T param1, final U param2) {
        final AtomicReference<State> stateRef = this.stateRef;
        State oldState;
        State newState;
        do {
            oldState = stateRef.get();
            if (oldState.isDone()) {
                executor.execute(() -> consumer.accept(param1, param2));
                return;
            }
            newState = new OnCompleteState<>(oldState, consumer, param1, param2);
        } while (! stateRef.compareAndSet(oldState, newState));

    }

    static abstract class State {
        State() {
        }

        void signalCompletion(final Executor executor) {}

        boolean isWaitingFor(final Thread thread) {
            return false;
        }

        UndeployResult getUndeployResult() {
            throw Messages.log.invalidDeploymentExecutionState();
        }

        boolean isDone() {
            return false;
        }

    }

    static final class CompleteState extends State implements UndeployResult {
        private final long duration;

        CompleteState(final long duration) {
            this.duration = duration;
        }

        public long getDuration(final TimeUnit timeUnit) {
            return timeUnit.convert(duration, TimeUnit.MILLISECONDS);
        }

        boolean isDone() {
            return true;
        }

        UndeployResult getUndeployResult() {
            return this;
        }
    }

    final class RunningState extends State implements Dependency {
        private final long startTime;
        private final AtomicInteger dependentsRemaining;

        RunningState(final int dependentCount) {
            startTime = System.nanoTime();
            dependentsRemaining = new AtomicInteger(dependentCount);
        }

        public void dependentDone(final Executor executor) {
            if (dependentsRemaining.decrementAndGet() == 0) {
                // done!
                State oldState;
                final CompleteState completeState = new CompleteState(System.nanoTime() - startTime);
                do {
                    oldState = stateRef.get();
                } while (! stateRef.compareAndSet(oldState, completeState));
                oldState.signalCompletion(executor);
            }
        }
    }

    static final class WaitingState extends State {
        private final State next;
        private final Thread thread;

        WaitingState(final State next, final Thread thread) {
            this.thread = thread;
            this.next = next;
        }

        boolean isWaitingFor(final Thread thread) {
            return thread == this.thread || next.isWaitingFor(thread);
        }

        void signalCompletion(final Executor executor) {
            next.signalCompletion(executor);
            unpark(thread);
        }
    }

    static final class OnCompleteState<T, U> extends State {
        private final State next;
        private final BiConsumer<T, U> consumer;
        private final T param1;
        private final U param2;

        OnCompleteState(final State next, final BiConsumer<T, U> consumer, final T param1, final U param2) {
            this.consumer = consumer;
            this.param1 = param1;
            this.param2 = param2;
            this.next = next;
        }

        boolean isWaitingFor(final Thread thread) {
            return next.isWaitingFor(thread);
        }

        void signalCompletion(final Executor executor) {
            next.signalCompletion(executor);
            try {
                executor.execute(() -> consumer.accept(param1, param2));
            } catch (Throwable ignored) {}
        }
    }
}
