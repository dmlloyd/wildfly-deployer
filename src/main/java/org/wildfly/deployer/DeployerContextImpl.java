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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.wildfly.common.Assert;

/**
 * The implementation of the context for a deployment instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeployerContextImpl implements DeploymentContext, Dependent {
    private final DeployerInfo deployerInfo;
    private final DeployerExecutionImpl execution;
    private final AtomicInteger stateAndCount;
    private final Dependent[] dependents;
    private final ConcurrentStack<Action<?, ?>> undeployActions = new ConcurrentStack<>();
    private final ConcurrentStack<Dependency> dependencies = new ConcurrentStack<>();
    private final CopyOnWriteArraySet<Thread> runningThreads = new CopyOnWriteArraySet<>();

    private static final int STATE_WAITING      = 0;
    private static final int STATE_WAITING_FD   = 1;
    private static final int STATE_WAITING_CD   = 2;
    private static final int STATE_RUNNING      = 3;
    private static final int STATE_RUNNING_CR   = 4;
    private static final int STATE_FAILED       = 5;
    private static final int STATE_CANCELLED    = 6;
    private static final int STATE_DONE         = 7;

    DeployerContextImpl(final DeployerInfo deployerInfo, final DeployerExecutionImpl execution, final Dependent[] dependents) {
        this.deployerInfo = deployerInfo;
        this.execution = execution;
        this.dependents = dependents;
        final Set<DeployerInfo> dependencies = deployerInfo.getDependencies();
        stateAndCount = new AtomicInteger(encodeState(STATE_WAITING, dependencies.size()));
    }

    private static int encodeState(int state, int count) {
        return state << 20 | count;
    }

    private static int getState(int value) {
        return value >> 20;
    }

    private static int getCount(int value) {
        return value & (1 << 20) - 1;
    }

    public String getDeploymentName() {
        return execution.getDeploymentName();
    }

    public void produce(final String name, final Object item) {
        Assert.checkNotNullParam("name", name);
        checkValid();
        final Multiplicity multiplicity = deployerInfo.getProduced().get(name);
        if (multiplicity != null) {
            if (multiplicity == Multiplicity.SYMBOLIC) {
                throw Messages.log.cannotProduce(name);
            } else if (multiplicity == Multiplicity.SINGLE) {
                execution.produceSingle(name, item);
            } else {
                assert multiplicity == Multiplicity.MULTIPLE;
                execution.produceMulti(name, item);
            }
        } else {
            throw Messages.log.cannotProduce(name);
        }
    }

    public Object consume(final String name) {
        Assert.checkNotNullParam("name", name);
        checkValid();
        final Multiplicity multiplicity = deployerInfo.getConsumed().get(name);
        if (multiplicity != null) {
            if (multiplicity != Multiplicity.SINGLE) {
                throw Messages.log.cannotConsumeMultipleResourceAsSingle(name);
            }
            return execution.consume(name);
        } else {
            throw Messages.log.cannotConsume(name);
        }
    }

    public Collection<?> consumeMulti(final String name) throws IllegalArgumentException {
        Assert.checkNotNullParam("name", name);
        checkValid();
        final Multiplicity multiplicity = deployerInfo.getConsumed().get(name);
        if (multiplicity != null) {
            if (multiplicity != Multiplicity.MULTIPLE) {
                throw Messages.log.cannotConsumeSingleResourceAsMultiple(name);
            }
            return execution.consumeMulti(name);
        } else {
            throw Messages.log.cannotConsume(name);
        }
    }

    public boolean isAvailableToConsume(final String name) {
        return deployerInfo.getConsumed().containsKey(name);
    }

    public boolean isRequiredToProduce(final String name) {
        return deployerInfo.getProduced().containsKey(name);
    }

    public <T, U> void addUndeployAction(final BiConsumer<T, U> consumer, final T parameter1, final U parameter2) {
        checkValid();
        undeployActions.push(new Action<>(consumer, parameter1, parameter2));
    }

    public boolean isCancelRequested() {
        final int state = getState(stateAndCount.get());
        return state == STATE_RUNNING_CR || state == STATE_CANCELLED || state == STATE_WAITING_CD;
    }

    public <T, U, R> R applyInterruptibly(final BiFunction<T, U, R> function, final T param1, final U param2) {
        final Thread thread = Thread.currentThread();
        if (runningThreads.add(thread)) try {
            return function.apply(param1, param2);
        } finally {
            runningThreads.remove(thread);
        } else {
            return function.apply(param1, param2);
        }
    }

    public <T, U> void acceptInterruptibly(final BiConsumer<T, U> consumer, final T param1, final U param2) {
        final Thread thread = Thread.currentThread();
        if (runningThreads.add(thread)) try {
            consumer.accept(param1, param2);
        } finally {
            runningThreads.remove(thread);
        } else {
            consumer.accept(param1, param2);
        }
    }

    public boolean setSucceeded() {
        final AtomicInteger stateAndCount = this.stateAndCount;
        int oldVal, oldState;
        do {
            oldVal = stateAndCount.get();
            oldState = getState(oldVal);
            if (oldState != STATE_RUNNING && oldState != STATE_RUNNING_CR) {
                return false;
            }
        } while (! stateAndCount.compareAndSet(oldVal, encodeState(STATE_DONE, 0)));
        sendDependencyDone();
        Messages.log.tracef("Deployer %s succeeded", deployerInfo.getDeployer());
        return true;
    }

    private UndeployerContext createUndeployerContext() {
        return new UndeployerContext(
            undeployActions.popAll(size -> size == 0 ? Action.NO_ACTIONS : new Action<?, ?>[size]),
            dependencies.popAll(size -> size == 0 ? Dependency.NO_DEPENDENCIES : new Dependency[size])
        );
    }

    public boolean setFailed(final DeploymentException reason) {
        final AtomicInteger stateAndCount = this.stateAndCount;
        int oldVal, oldState;
        do {
            oldVal = stateAndCount.get();
            oldState = getState(oldVal);
            if (oldState != STATE_RUNNING && oldState != STATE_RUNNING_CR) {
                return false;
            }
        } while (! stateAndCount.compareAndSet(oldVal, encodeState(STATE_FAILED, 0)));
        Messages.log.tracef("Deployer %s failed", deployerInfo.getDeployer());
        sendDependencyFailed();
        return true;
    }

    public boolean setCancelled() throws IllegalStateException {
        final AtomicInteger stateAndCount = this.stateAndCount;
        int oldVal, oldState;
        do {
            oldVal = stateAndCount.get();
            oldState = getState(oldVal);
            if (oldState == STATE_RUNNING) {
                throw new IllegalStateException();
            } else if (oldState == STATE_FAILED || oldState == STATE_CANCELLED || oldState == STATE_DONE) {
                return false;
            } else {
                assert oldState == STATE_RUNNING_CR;
            }
        } while (! stateAndCount.compareAndSet(oldVal, encodeState(STATE_CANCELLED, 0)));
        Messages.log.tracef("Deployer %s cancelled", deployerInfo.getDeployer());
        sendDependencyCancelled();
        return true;
    }

    public void dependencyDone(final Dependency dependency) {
        dependencies.push(dependency);
        final AtomicInteger stateAndCount = this.stateAndCount;
        int oldVal, oldState, oldCount, newVal;
        do {
            oldVal = stateAndCount.get();
            oldState = getState(oldVal);
            assert oldState == STATE_WAITING || oldState == STATE_WAITING_FD || oldState == STATE_WAITING_CD;
            oldCount = getCount(oldVal);
            if (oldCount == 1) {
                if (oldState == STATE_WAITING) {
                    newVal = encodeState(STATE_RUNNING, 0);
                } else if (oldState == STATE_WAITING_FD) {
                    newVal = encodeState(STATE_FAILED, 0);
                } else {
                    assert oldState == STATE_WAITING_CD;
                    newVal = encodeState(STATE_CANCELLED, 0);
                }
            } else {
                newVal = encodeState(oldState, oldCount - 1);
            }
        } while (! stateAndCount.compareAndSet(oldVal, newVal));
        if (oldCount == 1) {
            int newState = getState(newVal);
            // it was a transition
            if (newState == STATE_RUNNING) {
                run();
            } else if (newState == STATE_FAILED) {
                sendDependencyFailed();
            } else {
                assert newState == STATE_CANCELLED;
                sendDependencyCancelled();
            }
        }
    }

    void start() {
        final AtomicInteger stateAndCount = this.stateAndCount;
        int oldVal, oldState, oldCount, newVal;
        do {
            oldVal = stateAndCount.get();
            oldState = getState(oldVal);
            assert oldState == STATE_WAITING || oldState == STATE_WAITING_FD || oldState == STATE_WAITING_CD;
            oldCount = getCount(oldVal);
            assert oldCount == 0;
            if (oldState == STATE_WAITING) {
                newVal = encodeState(STATE_RUNNING, 0);
            } else if (oldState == STATE_WAITING_FD) {
                newVal = encodeState(STATE_FAILED, 0);
            } else {
                assert oldState == STATE_WAITING_CD;
                newVal = encodeState(STATE_CANCELLED, 0);
            }
        } while (! stateAndCount.compareAndSet(oldVal, newVal));
        int newState = getState(newVal);
        if (newState == STATE_RUNNING) {
            run();
        } else if (newState == STATE_FAILED) {
            sendDependencyFailed();
        } else {
            assert newState == STATE_CANCELLED;
            sendDependencyCancelled();
        }
    }

    public void dependencyFailed(final Dependency dependency) {
        dependencies.push(dependency);
        final AtomicInteger stateAndCount = this.stateAndCount;
        int oldVal, oldState, oldCount, newVal;
        do {
            oldVal = stateAndCount.get();
            oldState = getState(oldVal);
            assert oldState == STATE_WAITING || oldState == STATE_WAITING_FD || oldState == STATE_WAITING_CD;
            oldCount = getCount(oldVal);
            if (oldCount == 1) {
                if (oldState == STATE_WAITING || oldState == STATE_WAITING_FD) {
                    newVal = encodeState(STATE_FAILED, 0);
                } else {
                    assert oldState == STATE_WAITING_CD;
                    newVal = encodeState(STATE_CANCELLED, 0);
                }
            } else {
                if (oldState == STATE_WAITING) {
                    newVal = encodeState(STATE_WAITING_FD, oldCount - 1);
                } else {
                    newVal = encodeState(oldState, oldCount - 1);
                }
            }
        } while (! stateAndCount.compareAndSet(oldVal, newVal));
        if (oldCount == 1) {
            int newState = getState(newVal);
            // it was a transition
            if (newState == STATE_FAILED) {
                sendDependencyFailed();
            } else {
                assert newState == STATE_CANCELLED;
                sendDependencyCancelled();
            }
        }
    }

    public void dependencyCancelled(final Dependency dependency) {
        dependencies.push(dependency);
        final AtomicInteger stateAndCount = this.stateAndCount;
        int oldVal, oldState, oldCount, newVal;
        do {
            oldVal = stateAndCount.get();
            oldState = getState(oldVal);
            assert oldState == STATE_WAITING || oldState == STATE_WAITING_FD || oldState == STATE_WAITING_CD;
            oldCount = getCount(oldVal);
            if (oldCount == 1) {
                newVal = encodeState(STATE_CANCELLED, 0);
            } else {
                newVal = encodeState(STATE_WAITING_CD, oldCount - 1);
            }
        } while (! stateAndCount.compareAndSet(oldVal, newVal));
        if (oldCount == 1) {
            // it was a transition
            assert getState(newVal) == STATE_CANCELLED;
            sendDependencyCancelled();
        }
    }

    public void cancelRequested() {
        final AtomicInteger stateAndCount = this.stateAndCount;
        int oldVal, oldState, oldCount, newVal;
        do {
            oldVal = stateAndCount.get();
            oldState = getState(oldVal);
            oldCount = getCount(oldVal);
            if (oldState == STATE_WAITING_CD || oldState == STATE_RUNNING_CR || oldState == STATE_CANCELLED) {
                return;
            } else if (oldState == STATE_WAITING || oldState == STATE_WAITING_FD) {
                newVal = encodeState(STATE_WAITING_CD, oldCount);
            } else if (oldState == STATE_RUNNING) {
                newVal = encodeState(STATE_RUNNING_CR, oldCount);
            } else {
                assert oldState == STATE_DONE || oldState == STATE_FAILED;
                sendDependencyCancelRequested();
                return;
            }
        } while (! stateAndCount.compareAndSet(oldVal, newVal));
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            AccessController.doPrivileged((PrivilegedAction<Void>) this::interruptRunning);
        } else {
            interruptRunning();
        }
        sendDependencyCancelRequested();
    }

    private Void interruptRunning() {
        for (Thread thread : runningThreads) {
            thread.interrupt();
        }
        return null;
    }

    private void sendDependencyCancelRequested() {
        for (Dependent dependent : dependents) {
            dependent.cancelRequested();
        }
    }

    private void sendDependencyDone() {
        Dependency dependency = createUndeployerContext();
        for (Dependent dependent : dependents) {
            dependent.dependencyDone(dependency);
        }
    }

    private void sendDependencyFailed() {
        Dependency dependency = createUndeployerContext();
        for (Dependent dependent : dependents) {
            dependent.dependencyFailed(dependency);
        }
    }

    private void sendDependencyCancelled() {
        Dependency dependency = createUndeployerContext();
        for (Dependent dependent : dependents) {
            dependent.dependencyCancelled(dependency);
        }
    }

    private void checkValid() {
        final int state = getState(stateAndCount.get());
        if (state != STATE_RUNNING && state != STATE_RUNNING_CR) {
            throw new IllegalStateException();
        }
    }

    void run() {
        Messages.log.tracef("Starting deployer %s", deployerInfo.getDeployer());
        if (deployerInfo.getDeployer() == Deployer.EMPTY) {
            setSucceeded();
            return;
        }
        try {
            execution.getExecutor().execute(() -> {
                try {
                    deployerInfo.getDeployer().deploy(DeployerContextImpl.this);
                } catch (Throwable t) {
                    if (! setFailed(Messages.log.deploymentStepException(t))) {
                        Messages.log.uncaughtException(t);
                    }
                }
            });
        } catch (Throwable t) {
            if (! setFailed(Messages.log.deploymentStepExecuteException(t))) {
                // this should be impossible, but just in case, log it anyway
                Messages.log.uncaughtException(t);
            }
        }
    }
}
