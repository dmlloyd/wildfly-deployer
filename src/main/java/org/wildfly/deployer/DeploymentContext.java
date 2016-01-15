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

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.wildfly.common.Assert;

/**
 * The context passed to a deployer's operation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DeploymentContext {
    /**
     * Get the name of this deployment.  The resultant string is useful for diagnostic messages and does not have
     * any other significance.
     *
     * @return the name of this deployment (not {@code null})
     */
    String getDeploymentName();

    /**
     * Produce the given item.  If the {@code name} refers to a resource which is declared with multiplicity, then this
     * method can be called more than once for the given {@code name}, otherwise it must be called no more than once.
     *
     * @param name the resource name (must not be {@code null})
     * @param item the resource value (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to produce {@code name}, or if {@code name} is {@code null}, or if
     *      the resource does not allow multiplicity but this method is called more than one time
     */
    void produce(String name, Object item);

    /**
     * Consume the value produced for the named resource, casting the result to the given type.
     *
     * @param name the resource name (must not be {@code null})
     * @param type the resource type (must not be {@code null})
     * @return the produced resource (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code name}, or if {@code name} or {@code type} is {@code null}
     * @throws ClassCastException if the cast failed
     */
    default <T> T consume(Class<T> type, String name) {
        Assert.checkNotNullParam("type", type);
        return type.cast(consume(name));
    }

    /**
     * Consume the value produced for the named resource.
     *
     * @param name the resource name (must not be {@code null})
     * @return the produced resource (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code name}, or if {@code name} is {@code null}
     */
    Object consume(String name);

    /**
     * Consume all of the values produced for the named resource, casting the collection to the given type.
     *
     * @param name the resource name (must not be {@code null})
     * @param type the resource element type (must not be {@code null})
     * @return the produced resources (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code name}, or if {@code name} or {@code type} is {@code null}
     */
    @SuppressWarnings("unchecked")
    default <T> Collection<T> consumeMulti(String name, Class<T> type) {
        Assert.checkNotNullParam("type", type);
        return (Collection<T>) consumeMulti(name);
    }

    /**
     * Consume all of the values produced for the named resource.
     *
     * @param name the resource name (must not be {@code null})
     * @return the produced resources (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code name}, or if {@code name} is {@code null}
     */
    Collection<?> consumeMulti(String name) throws IllegalArgumentException;

    /**
     * Determine if a (likely optional) resource was produced and is therefore available to be {@linkplain #consume(String) consumed}.
     *
     * @param name the resource name (must not be {@code null})
     * @return {@code true} if the resource was produced and is available, {@code false} if it was not or if this deployer does
     *  not consume the named resource
     */
    boolean isAvailableToConsume(String name);

    /**
     * Determine if a (likely optional) resource will be consumed and is therefore required to be {@linkplain #produce(String,Object) produced}.
     *
     * @param name the resource name (must not be {@code null})
     * @return {@code true} if the resource will be consumed, {@code false} if it will not be or if this deployer does
     *  not produce the named resource
     */
    boolean isRequiredToProduce(String name);

    /**
     * Add an undeploy action for this deployer.  The given action should <b>not</b> retain references to intermediate
     * deployment data, as doing so may result in excessive memory consumption at run time.
     *
     * @param runnable the runnable action (must not be {@code null})
     */
    default void addUndeployAction(Runnable runnable) {
        addUndeployAction(Runnable::run, runnable);
    }

    /**
     * Add an undeploy action for this deployer.  The given action should <b>not</b> retain references to intermediate
     * deployment data, as doing so may result in excessive memory consumption at run time.
     *
     * @param consumer the consumer action (must not be {@code null})
     * @param parameter the parameter to pass to the consumer
     */
    default <T> void addUndeployAction(Consumer<T> consumer, T parameter) {
        addUndeployAction(Consumer::accept, consumer, parameter);
    }

    /**
     * Add an undeploy action for this deployer.  The given action should <b>not</b> retain references to intermediate
     * deployment data, as doing so may result in excessive memory consumption at run time.
     *
     * @param consumer the consumer action (must not be {@code null})
     * @param parameter1 the first parameter to pass to the consumer
     * @param parameter2 the second parameter to pass to the consumer
     */
    <T, U> void addUndeployAction(BiConsumer<T, U> consumer, T parameter1, U parameter2);

    /**
     * Determine whether the current deployment operation was requested to be cancelled.  Once this flag is set to
     * {@code true}, it will never return to {@code false}.
     *
     * @return {@code true} if cancel was requested, {@code false} otherwise
     */
    boolean isCancelRequested();

    /**
     * Do something interruptibly.  The operation will be interrupted if cancel was requested at any time during
     * execution, unblocking any blocking operations.
     *
     * @param function the interruptible operation to execute
     * @param param1 the first parameter to pass to the function
     * @param param2 the second parameter to pass to the function
     * @param <T> the type of the first parameter
     * @param <U> the type of the second parameter
     * @param <R> the type of the return value
     * @return the return value
     */
    <T, U, R> R applyInterruptibly(BiFunction<T, U, R> function, T param1, U param2);

    /**
     * Do something interruptibly.  The operation will be interrupted if cancel was requested at any time during
     * execution, unblocking any blocking operations.
     *
     * @param function the interruptible operation to execute
     * @param param the parameter to pass to the function
     * @param <T> the type of the parameter
     * @param <R> the type of the return value
     * @return the return value
     */
    default <T, R> R applyInterruptibly(Function<T, R> function, T param) {
        return applyInterruptibly(Function::apply, function, param);
    }

    /**
     * Do something interruptibly.  The operation will be interrupted if cancel was requested at any time during
     * execution, unblocking any blocking operations.
     *
     * @param supplier the interruptible operation to execute
     * @param <R> the type of the return value
     * @return the return value
     */
    default <R> R getInterruptibly(Supplier<R> supplier) {
        return applyInterruptibly(Supplier::get, supplier);
    }

    /**
     * Do something interruptibly.  The operation will be interrupted if cancel was requested at any time during
     * execution, unblocking any blocking operations.
     *
     * @param consumer the interruptible operation to execute
     * @param param1 the first parameter to pass to the function
     * @param param2 the second parameter to pass to the function
     * @param <T> the type of the first parameter
     * @param <U> the type of the second parameter
     */
    <T, U> void acceptInterruptibly(BiConsumer<T, U> consumer, T param1, U param2);

    /**
     * Do something interruptibly.  The operation will be interrupted if cancel was requested at any time during
     * execution, unblocking any blocking operations.
     *
     * @param consumer the interruptible operation to execute
     * @param param the parameter to pass to the function
     * @param <T> the type of the parameter
     */
    default <T> void acceptInterruptibly(Consumer<T> consumer, T param) {
        acceptInterruptibly(Consumer::accept, consumer, param);
    }

    /**
     * Do something interruptibly.  The operation will be interrupted if cancel was requested at any time during
     * execution, unblocking any blocking operations.
     *
     * @param runnable the interruptible operation to execute
     */
    default void runInterruptibly(Runnable runnable) {
        acceptInterruptibly(Runnable::run, runnable);
    }

    /**
     * Indicate that the deployment operation has completed successfully.  If any required resources were not produced,
     * the deployment will fail with a descriptive error message.  If the deployment was already marked as complete,
     * then this method returns {@code false} and no action is taken.  The return value need only be referenced in
     * special situations, and is safely ignored in deployers with a simple static or predictable structure.  Only
     * successful deployment operations will be undone by undeployment operations.
     *
     * @return {@code true} if the deployment was marked complete, or {@code false} if it was already complete
     */
    boolean setSucceeded();

    /**
     * Indicate that the deployment operation has completed and failed.  No resources will be produced and no dependent
     * deployers invoked, and undeploy will not occur (it is expected that the deployer clean any intermediate state).
     * If the deployment was already marked as complete, then this method returns {@code false} and
     * no action is taken.  The return value need only be referenced in special situations, and is safely ignored
     * in deployers with a simple static or predictable structure.
     *
     * @param reason the reason of the failure
     * @return {@code true} if the deployment was marked complete, or {@code false} if it was already complete
     */
    boolean setFailed(DeploymentException reason);

    /**
     * Indicate that the deployment operation has been cancelled in response to user request.  No resources will be
     * produced and no dependent deployers invoked.  If the deployment was already marked as complete, then this method
     * returns {@code false} and no action is taken.  If no cancellation was {@linkplain #isCancelRequested() requested},
     * this method throws {@code IllegalStateException} and no action is taken.  The return value need only be referenced
     * in special situations, and is safely ignored in deployers with a simple static or predictable structure.
     *
     * @return {@code true} if the deployment was marked complete, or {@code false} if it was already complete
     * @throws IllegalStateException if no cancel was requested at the time this method was invoked
     */
    boolean setCancelled() throws IllegalStateException;
}
