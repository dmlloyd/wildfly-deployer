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

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.unpark;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeployerExecutionImpl implements DeployerExecution {
    private final AtomicReference<State> stateRef;
    private final String name;

    private final DeployerChainImpl deployerChain;
    private final ConcurrentMap<String, Object> singleResources;
    private final ConcurrentMap<String, List<Object>> multiResources;
    private final Executor executor;
    private final ConcurrentMap<DeployerInfo, DeployerContextImpl> deployers = new ConcurrentHashMap<>();
    private final ConcurrentStack<DeploymentException> problems = new ConcurrentStack<>();

    DeployerExecutionImpl(final DeployerChainImpl deployerChain, final Executor executor, final Map<String, Object> initialSingleResources, final Map<String, List<Object>> initialMultiResources, final String name) {
        this.deployerChain = deployerChain;
        this.executor = executor;
        singleResources = new ConcurrentHashMap<>(initialSingleResources);
        multiResources = new ConcurrentHashMap<>(initialMultiResources);
        final int size = deployerChain.getFinalDeployers().size();
        if (size == 0) {
            stateRef = new AtomicReference<>(new SuccessState(deployerChain, singleResources, multiResources, 0L, Dependency.NO_DEPENDENCIES, name));
            Messages.log.executionComplete(name, 0);
        } else {
            stateRef = new AtomicReference<>(new RunningState(size));
        }
        this.name = name;
    }

    Dependent[] getBottomArray() {
        return stateRef.get().dependentArray();
    }

    public String getDeploymentName() {
        return name;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void cancel() {
        stateRef.get().requestCancel();
    }

    public Status getStatus() {
        return stateRef.get().getStatus();
    }

    public Status await() throws InterruptedException {
        final AtomicReference<State> stateRef = this.stateRef;
        State oldState = stateRef.get();
        Status status = oldState.getStatus();
        if (status == Status.EXECUTING) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            final Thread thread = Thread.currentThread();
            if (! oldState.isWaitingFor(thread)) {
                // CAS in our thread
                State newState = new WaitingState(oldState, thread);
                while (! stateRef.compareAndSet(oldState, newState)) {
                    oldState = stateRef.get();
                    status = oldState.getStatus();
                    if (status != Status.EXECUTING) {
                        return status;
                    }
                    assert ! oldState.isWaitingFor(thread);
                    newState = new WaitingState(oldState, thread);
                }
            }
            park(this);
            oldState = stateRef.get();
            status = oldState.getStatus();
            while (status == Status.EXECUTING) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                park(this);
                oldState = stateRef.get();
                status = oldState.getStatus();
            }
        }
        return status;
    }

    public SuccessfulDeploymentResult getSuccessfulResult() throws IllegalStateException {
        return stateRef.get().getSuccessfulResult();
    }

    public FailedDeploymentResult getFailedResult() throws IllegalStateException {
        return stateRef.get().getFailedResult();
    }

    public <T, U> void onComplete(final BiConsumer<T, U> consumer, final T param1, final U param2) {
        final AtomicReference<State> stateRef = this.stateRef;
        State oldState;
        State newState;
        Status status;
        do {
            oldState = stateRef.get();
            status = oldState.getStatus();
            if (status != Status.EXECUTING) {
                executor.execute(() -> consumer.accept(param1, param2));
                return;
            }
            newState = new OnCompleteState<>(oldState, consumer, param1, param2);
        } while (! stateRef.compareAndSet(oldState, newState));
    }

    void run() {
        for (DeployerInfo deployerInfo : deployerChain.getInitialDeployers()) {
            getOrAddDeployer(deployerInfo).start();
        }
    }

    DeployerContextImpl getOrAddDeployer(final DeployerInfo info) {
        final ConcurrentMap<DeployerInfo, DeployerContextImpl> deployers = this.deployers;
        DeployerContextImpl context = deployers.get(info);
        if (context != null) {
            return context;
        }
        context = computeDeployerContextImpl(info);
        DeployerContextImpl appearing = deployers.putIfAbsent(info, context);
        return appearing != null ? appearing : context;
    }

    private DeployerContextImpl computeDeployerContextImpl(final DeployerInfo di) {
        final Set<DeployerInfo> dependentInfos = di.getDependents();
        final int size = dependentInfos.size();
        final Dependent[] dependents;
        if (size == 0) {
            dependents = getBottomArray();
        } else {
            dependents = new Dependent[size];
            int i = 0;
            for (DeployerInfo dependentInfo : dependentInfos) {
                dependents[i++] = getOrAddDeployer(dependentInfo);
            }
        }
        return new DeployerContextImpl(di, this, dependents);
    }

    static <T> ArrayList<T> newArrayList(String ignored) {
        return new ArrayList<>();
    }

    void produceSingle(final String name, final Object item) {
        singleResources.putIfAbsent(name, item);
    }

    void produceMulti(final String name, final Object item) {
        multiResources.computeIfAbsent(name, DeployerExecutionImpl::newArrayList).add(item);
    }

    Object consume(final String name) {
        return singleResources.get(name);
    }

    Collection<?> consumeMulti(final String name) {
        return multiResources.getOrDefault(name, Collections.emptyList());
    }

    static final int FLAG_CANCELLED = 1 << 30;
    static final int FLAG_FAILED = 1 << 29;

    static final int FLAG_MASK = FLAG_FAILED | FLAG_CANCELLED;

    static abstract class State {

        private static final State CANCELLED = new State() {
            Status getStatus() {
                return Status.CANCELLED;
            }

            boolean isWaitingFor(final Thread thread) {
                return false;
            }
        };

        State() {
        }

        abstract Status getStatus();

        void signalCompletion() {}

        void requestCancel() {}

        boolean isWaitingFor(final Thread thread) {
            return false;
        }

        SuccessfulDeploymentResult getSuccessfulResult() {
            throw Messages.log.invalidDeploymentExecutionState();
        }

        FailedDeploymentResult getFailedResult() {
            throw Messages.log.invalidDeploymentExecutionState();
        }

        Dependent[] dependentArray() {
            throw Messages.log.invalidDeploymentExecutionState();
        }
    }

    final class RunningState extends State implements Dependent {
        private final AtomicInteger state;
        private final ConcurrentStack<Dependency> dependencies = new ConcurrentStack<>();
        private final long start = System.nanoTime();
        private final Dependent[] array = new Dependent[] { this };

        RunningState(int dependencyCount) {
            state = new AtomicInteger(dependencyCount);
        }

        Status getStatus() {
            return Status.EXECUTING;
        }

        Dependent[] dependentArray() {
            return array;
        }

        public void dependencyDone(final Dependency dependency) {
            dependencies.push(dependency);
            int oldVal, newVal;
            do {
                oldVal = state.get();
                assert (oldVal & ~(FLAG_CANCELLED | FLAG_FAILED)) > 0;
                newVal = oldVal - 1;
            } while (! state.compareAndSet(oldVal, newVal));
            if ((oldVal & FLAG_MASK) == 0) {
                finish();
            }
        }

        public void dependencyFailed(final Dependency dependency) {
            dependencies.push(dependency);
            int oldVal, newVal;
            do {
                oldVal = state.get();
                assert (oldVal & ~(FLAG_CANCELLED | FLAG_FAILED)) > 0;
                newVal = oldVal - 1 | FLAG_FAILED;
            } while (! state.compareAndSet(oldVal, newVal));
            if ((oldVal & FLAG_MASK) == 0) {
                finish();
            }
        }

        public void dependencyCancelled(final Dependency dependency) {
            dependencies.push(dependency);
            int oldVal, newVal;
            do {
                oldVal = state.get();
                assert (oldVal & ~(FLAG_CANCELLED | FLAG_FAILED)) > 0;
                newVal = oldVal - 1 | FLAG_CANCELLED;
            } while (! state.compareAndSet(oldVal, newVal));
            if ((oldVal & FLAG_MASK) == 0) {
                finish();
            }
        }

        public void cancelRequested() {
            // ignored
        }

        private void finish() {
            final Dependency[] dependencies = this.dependencies.popAll(Dependency[]::new);
            final int val = state.get();
            final long duration = max(0L, System.nanoTime() - start);
            State oldState, newState;
            if ((val & FLAG_CANCELLED) != 0) {
                newState = State.CANCELLED;
            } else if ((val & FLAG_FAILED) != 0) {
                newState = new FailureState(deployerChain, asList(problems.popAll(DeploymentException[]::new)), duration, dependencies, name);
                Messages.log.executionFailed(name, duration / 1000000L);
            } else {
                newState = new SuccessState(deployerChain, singleResources, multiResources, duration, dependencies, name);
                Messages.log.executionComplete(name, duration / 1000000L);
            }
            do {
                oldState = stateRef.get();
            } while (! stateRef.compareAndSet(oldState, newState));
            oldState.signalCompletion();
        }
    }

    static abstract class ResultState extends State implements DeploymentResult {
        private final AtomicReference<DeployerChainImpl> deployerChainRef;
        private final Dependency[] dependencies;
        private final String name;
        private final long duration;

        ResultState(final DeployerChainImpl deployerChain, final Dependency[] dependencies, final long duration, final String name) {
            this.deployerChainRef = new AtomicReference<>(deployerChain);
            this.dependencies = dependencies;
            this.duration = duration;
            this.name = name;
        }

        public final UndeployExecution undeploy(final Executor executor) {
            final DeployerChainImpl deployerChain = deployerChainRef.getAndSet(null);
            if (deployerChain == null) {
                throw Messages.log.alreadyUndeployed();
            }
            final UndeployExecutionImpl undeployExecution = new UndeployExecutionImpl(deployerChain, executor, dependencies, name);
            undeployExecution.run();
            return undeployExecution;
        }

        public final long getDuration(final TimeUnit timeUnit) {
            return timeUnit.convert(duration, TimeUnit.MILLISECONDS);
        }
    }

    static final class FailureState extends ResultState implements FailedDeploymentResult {
        private final Collection<DeploymentException> problems;

        FailureState(final DeployerChainImpl deployerChain, final Collection<DeploymentException> problems, final long duration, final Dependency[] dependencies, final String name) {
            super(deployerChain, dependencies, duration, name);
            this.problems = problems;
        }

        Status getStatus() {
            return Status.FAILED;
        }

        public Collection<DeploymentException> getExceptions() {
            return problems;
        }

        FailedDeploymentResult getFailedResult() {
            return this;
        }
    }

    static final class SuccessState extends ResultState implements SuccessfulDeploymentResult {
        private final Map<String, Object> singleResources;
        private final Map<String, List<Object>> multiResources;

        SuccessState(final DeployerChainImpl deployerChain, final ConcurrentMap<String, Object> singleResources, final ConcurrentMap<String, List<Object>> multiResources, final long duration, final Dependency[] dependencies, final String name) {
            super(deployerChain, dependencies, duration, name);
            this.singleResources = singleResources;
            this.multiResources = multiResources;
        }

        Status getStatus() {
            return Status.SUCCESSFUL;
        }

        public Object consume(final String name) {
            return singleResources.get(name);
        }

        public Collection<?> consumeMulti(final String name) throws IllegalArgumentException {
            return multiResources.get(name);
        }

        SuccessfulDeploymentResult getSuccessfulResult() {
            return this;
        }
    }

    static final class WaitingState extends State {
        private final State next;
        private final Thread thread;

        WaitingState(final State next, final Thread thread) {
            this.next = next;
            this.thread = thread;
        }

        Status getStatus() {
            return next.getStatus();
        }

        boolean isWaitingFor(final Thread thread) {
            return thread == this.thread || next.isWaitingFor(thread);
        }

        void signalCompletion() {
            next.signalCompletion();
            unpark(thread);
        }
    }

    final class OnCompleteState<T, U> extends State {
        private final State next;
        private final BiConsumer<T, U> consumer;
        private final T param1;
        private final U param2;

        OnCompleteState(final State next, final BiConsumer<T, U> consumer, final T param1, final U param2) {
            this.next = next;
            this.consumer = consumer;
            this.param1 = param1;
            this.param2 = param2;
        }

        Status getStatus() {
            return next.getStatus();
        }

        boolean isWaitingFor(final Thread thread) {
            return next.isWaitingFor(thread);
        }

        void signalCompletion() {
            next.signalCompletion();
            try {
                executor.execute(() -> consumer.accept(param1, param2));
            } catch (Throwable ignored) {}
        }
    }
}
