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

/**
 * A deployer chain builder.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DeployerChainBuilder {

    /**
     * Add a deployer to the chain.  The configuration in the deployer builder at the time that the chain is built is
     * the configuration that will apply to the deployer in the final chain.  Any subsequent changes will be ignored.
     *
     * @param deployer the deployer instance
     * @return the builder for the deployer
     */
    DeployerBuilder addDeployer(Deployer deployer);

    /**
     * Declare an initial resource that will be provided to deployers in the chain.  Note that if this method is called,
     * no deployers will be allowed to produce this resource.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if the resource name is {@code null}
     */
    DeployerChainBuilder addInitialResource(String name);

    /**
     * Declare an initial resource (with multiplicity) that will be provided to deployers in the chain.  Note that if
     * this method is called, no deployers will be allowed to produce this resource.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if the resource name is {@code null}
     */
    DeployerChainBuilder addInitialMultiResource(String name);

    /**
     * Declare a final resource that will be consumable after the deployer chain completes.  This may be any resource
     * that is produced in the chain.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if the resource name is {@code null}
     */
    DeployerChainBuilder addFinalResource(String name);

    /**
     * Build the deployer chain from the current builder configuration.
     *
     * @return the constructed deployer chain
     * @throws DeployerChainBuildException if the chain could not be built
     */
    DeployerChain build() throws DeployerChainBuildException;
}
