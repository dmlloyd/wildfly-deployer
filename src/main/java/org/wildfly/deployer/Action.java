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

import java.util.function.BiConsumer;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class Action<T, U> implements Runnable {
    private final BiConsumer<T, U> consumer;
    private final T parameter1;
    private final U parameter2;

    Action(final BiConsumer<T, U> consumer, final T parameter1, final U parameter2) {
        this.consumer = consumer;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
    }

    public void run() {
        try {
            consumer.accept(parameter1, parameter2);
        } catch (Throwable t) {
            Messages.log.actionFailed(t);
        }
    }

    BiConsumer<T, U> getConsumer() {
        return consumer;
    }

    T getParameter1() {
        return parameter1;
    }

    U getParameter2() {
        return parameter2;
    }

    static final Action<?, ?>[] NO_ACTIONS = new Action[0];
}
