/*
 * JBoss, Home of Professional Open Source.
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

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class UndeployerContext implements Dependency {
    private static final int STATE_WAITING      = 0;
    private static final int STATE_RUNNING      = 1;
    private static final int STATE_DONE         = 2;

    private final AtomicInteger stateRef = new AtomicInteger();
    private final Action<?, ?>[] actions;
    private final Dependency[] dependencies;

    UndeployerContext(final Action<?, ?>[] actions, final Dependency[] dependencies) {
        this.actions = actions;
        this.dependencies = dependencies;
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

    public void dependentDone(Executor executor) {
        final AtomicInteger stateRef = this.stateRef;
        int oldVal, newVal, oldCount, newState;
        do {
            oldVal = stateRef.get();
            assert getState(oldVal) == STATE_WAITING;
            oldCount = getCount(oldVal);
            if (oldCount == 1) {
                final int length = actions.length;
                if (length == 0) {
                    newVal = encodeState(newState = STATE_DONE, 0);
                } else {
                    newVal = encodeState(newState = STATE_RUNNING, length);
                }
            } else {
                newVal = encodeState(newState = STATE_WAITING, oldCount - 1);
            }
        } while (! stateRef.compareAndSet(oldVal, newVal));
        if (newState == STATE_RUNNING) {
            // time to run it!
            for (Action<?, ?> action : actions) {
                executor.execute(() -> {
                    try {
                        action.run();
                    } finally {
                        taskDone(executor);
                    }
                });
            }
        } else if (newState == STATE_DONE) {
            allDone(executor);
        }
    }

    void taskDone(final Executor executor) {
        final AtomicInteger stateRef = this.stateRef;
        int oldVal, newVal, oldCount;
        do {
            oldVal = stateRef.get();
            assert getState(oldVal) == STATE_RUNNING;
            oldCount = getCount(oldVal);
            if (oldCount == 1) {
                newVal = encodeState(STATE_DONE, 0);
            } else {
                newVal = encodeState(STATE_RUNNING, oldCount - 1);
            }
        } while (! stateRef.compareAndSet(oldVal, newVal));
        if (oldCount == 1) {
            allDone(executor);
        }
    }

    private void allDone(final Executor executor) {
        for (Dependency dependency : dependencies) {
            dependency.dependentDone(executor);
        }
    }
}
