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

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An in-progress deployment-related operation execution.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Execution {
    /**
     * Get the name of this deployment.  The resultant string is useful for diagnostic messages and does not have
     * any other significance.
     *
     * @return the name of this deployment (not {@code null})
     */
    String getDeploymentName();

    /**
     * Get the executor being used for this execution.  The executor must remain valid for the duration of the execution
     * without rejecting tasks or else deployment operations may fail catastrophically.
     *
     * @return the executor (not {@code null})
     */
    Executor getExecutor();

    /**
     * Call the action once this execution is complete.  Note that calling this method after an execution is complete
     * will invoke the action immediately; if the execution's executor has been shut down, a {@link RejectedExecutionException}
     * will result.
     *
     * @param runnable the action to execute
     * @throws RejectedExecutionException if the execution was complete and executor did not accept the task
     */
    default void onComplete(Runnable runnable) throws RejectedExecutionException {
        onComplete(Runnable::run, runnable);
    }

    /**
     * Call the action once this execution is complete.  Note that calling this method after an execution is complete
     * will invoke the action immediately; if the execution's executor has been shut down, a {@link RejectedExecutionException}
     * will result.
     *
     * @param consumer the action to execute
     * @param param a parameter to pass in
     * @param <T> the parameter type
     * @throws RejectedExecutionException if the execution was complete and executor did not accept the task
     */
    default <T> void onComplete(Consumer<T> consumer, T param) throws RejectedExecutionException {
        onComplete(Consumer::accept, consumer, param);
    }

    /**
     * Call the action once this execution is complete.  Note that calling this method after an execution is complete
     * will invoke the action immediately; if the execution's executor has been shut down, a {@link RejectedExecutionException}
     * will result.
     *
     * @param consumer the action to execute
     * @param param1 a parameter to pass in
     * @param param2 a parameter to pass in
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @throws RejectedExecutionException if the execution was complete and executor did not accept the task
     */
    <T, U> void onComplete(BiConsumer<T, U> consumer, T param1, U param2) throws RejectedExecutionException;
}
