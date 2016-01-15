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
import java.util.concurrent.Executor;

/**
 * A builder for a deployer execution.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DeployerExecutionBuilder {

    /**
     * Get the name of this deployment.  The resultant string is useful for diagnostic messages and does not have
     * any other significance.
     *
     * @return the name of this deployment (not {@code null})
     */
    String getDeploymentName();

    /**
     * Provide an initial resource item.
     *
     * @param name the resource name (must not be {@code null})
     * @param item the resource value
     * @return this builder
     * @throws IllegalArgumentException if this deployer chain was not declared to initially produce {@code name},
     *      or if {@code name} is {@code null}, or if the resource does not allow multiplicity but this method is called
     *      more than one time
     */
    DeployerExecutionBuilder provide(String name, Object item);

    /**
     * Provide multiple initial resource items.
     *
     * @param name the resource name (must not be {@code null})
     * @param items the resource values (may be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if this deployer chain was not declared to initially produce {@code name},
     *      or if {@code name} is {@code null}, or if the resource does not allow multiplicity but this method is called
     *      to provide more than one item
     */
    DeployerExecutionBuilder provideAll(String name, Collection<?> items);

    /**
     * Provide multiple initial resource items.
     *
     * @param name the resource name (must not be {@code null})
     * @param items the resource values (may be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if this deployer chain was not declared to initially produce {@code name},
     *      or if {@code name} is {@code null}, or if the resource does not allow multiplicity but this method is called
     *      to provide more than one item
     */
    DeployerExecutionBuilder provideAll(String name, Object... items);

    /**
     * Construct and run the execution.
     *
     * @param executor the executor to run on (must not be {@code null})
     * @return the execution
     */
    DeployerExecution execute(Executor executor);
}
