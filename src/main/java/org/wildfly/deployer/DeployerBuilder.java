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
 * A builder for deployer chains.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DeployerBuilder {

    /**
     * This deployer comes before any deployers which produce or contribute to the given resource {@code name}.  If no
     * such deployers exist, no ordering constraint is enacted.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder beforeProduce(String name);

    /**
     * This deployer comes before any deployers which consume the given resource {@code name}.  If no such deployers
     * exist, no ordering constraint is enacted.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder beforeConsume(String name);

    /**
     * This deployer comes before any (mandatorily existent) deployers which consume the given resource {@code name}.
     * If no such deployers exist, the chain will not be constructed; instead, an error will be raised.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder beforeConsumeRequired(String name);

    /**
     * This deployer comes after any deployers which produce the given resource {@code name}.  If no such deployers
     * exist, no ordering constraint is enacted.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder afterProduce(String name);

    /**
     * This deployer comes after any (mandatorily existent) deployers which produce the given resource {@code name}.
     * If no such deployers exist, the chain will not be constructed; instead, an error will be raised.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder afterProduceRequired(String name);

    /**
     * This deployer comes after any deployers which consume the given resource {@code name}.  If no such deployers
     * exist, no ordering constraint is enacted.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder afterConsume(String name);

    /**
     * Similarly to {@link #beforeConsume(String)}, establish that this deployer must come before the consumer(s) of the
     * given resource {@code name}; however, only one {@code producer} may exist for the given name.  In addition, the
     * deployer may produce an actual value for this name, which will be shared to all consumers during deployment.
     *
     * @param name the name of the produced resource (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder produces(String name);

    /**
     * Similar to {@link #produces(String)} except that multiple values may be produced for the given resource name.
     *
     * @param name the name of the contributed-to resource (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder contributesTo(String name);

    /**
     * This deployer consumes the given produced resource.  The resource must be produced somewhere in the chain.  If
     * no such producer exists, the chain will not be constructed; instead, an error will be raised.
     *
     * @param name the name of the consumed resource (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder consumes(String name);

    /**
     * Similar to {@link #consumes(String)} except that if the resource is not produced, the chain will still be
     * constructed, but the deployer will only receive {@code null} for the value of the resource.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder consumesOptionally(String name);

    /**
     * Similar to {@link #destroysMandatory(String)} except that if the resource is not produced, no error is raised.
     *
     * @param name the name of the resource to destroy (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder destroys(String name);

    /**
     * This deployer cleans up/destroys the given produced resource.  The resource must be produced somewhere in the chain.
     * If no such producer exists, or another consumer is present for the resource {@code name}, the chain will not be
     * constructed; instead, an error will be raised.  Only one destructor can be registered for a resource.  The
     * destructor is called after all consumers of the given resource are complete.
     *
     * @param name the name of the resource to destroy (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder destroysMandatory(String name);

    /**
     * Declare that this deployer transforms the resource of the given {@code name}.  The resource is both consumed
     * and produced.  At present only one transformer may be specified per resource; this restriction may be lifted
     * in the future.
     *
     * @param name the resource name (must not be {@code null})
     * @return this builder
     */
    DeployerBuilder transforms(String name);
}
