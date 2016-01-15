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

import java.util.HashMap;
import java.util.Map;

import org.wildfly.common.Assert;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeployerBuilderImpl implements DeployerBuilder {
    private final DeployerChainBuilderImpl deployerChainBuilder;
    private final Deployer deployer;
    private final Map<String, Phase> resources = new HashMap<>();

    DeployerBuilderImpl(final DeployerChainBuilderImpl deployerChainBuilder, final Deployer deployer) {
        this.deployerChainBuilder = deployerChainBuilder;
        this.deployer = deployer;
    }

    public DeployerBuilder beforeProduce(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.PRE_PRODUCE, Multiplicity.SYMBOLIC, Mode.OPTIONAL);
        return this;
    }

    public DeployerBuilder beforeConsume(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.PRE_CONSUME, Multiplicity.SYMBOLIC, Mode.OPTIONAL);
        return this;
    }

    public DeployerBuilder beforeConsumeRequired(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.PRE_CONSUME, Multiplicity.SYMBOLIC, Mode.MANDATORY);
        return this;
    }

    public DeployerBuilder afterProduce(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.POST_PRODUCE, Multiplicity.SYMBOLIC, Mode.OPTIONAL);
        return this;
    }

    public DeployerBuilder afterProduceRequired(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.POST_PRODUCE, Multiplicity.SYMBOLIC, Mode.MANDATORY);
        return this;
    }

    public DeployerBuilder afterConsume(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.POST_CONSUME, Multiplicity.SYMBOLIC, Mode.OPTIONAL);
        return this;
    }

    public DeployerBuilder produces(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.PRODUCE, Multiplicity.SINGLE, Mode.OPTIONAL);
        return this;
    }

    public DeployerBuilder contributesTo(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.PRODUCE, Multiplicity.MULTIPLE, Mode.OPTIONAL);
        return this;
    }

    public DeployerBuilder consumes(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.CONSUME, Multiplicity.AUTOMATIC, Mode.MANDATORY);
        return this;
    }

    public DeployerBuilder consumesOptionally(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.CONSUME, Multiplicity.AUTOMATIC, Mode.OPTIONAL);
        return this;
    }

    public DeployerBuilder destroysMandatory(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.DESTROY, Multiplicity.AUTOMATIC, Mode.MANDATORY);
        return this;
    }

    public DeployerBuilder destroys(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.DESTROY, Multiplicity.AUTOMATIC, Mode.OPTIONAL);
        return this;
    }

    public DeployerBuilder transforms(final String name) {
        Assert.checkNotNullParam("name", name);
        addResource(name, Phase.TRANSFORM, Multiplicity.AUTOMATIC, Mode.MANDATORY);
        return this;
    }

    private void addResource(String name, Phase phase, Multiplicity multiplicity, Mode mode) {
        resources.compute(name, (s, existing) -> {
            if (existing != null && existing != phase) {
                throw Messages.log.incompatibleExistingRelationship(name);
            }
            // if this fails then neither this map nor the main map is updated
            deployerChainBuilder.addResource(name, phase, multiplicity, mode, this);
            return phase;
        });
    }

    Map<String, Phase> getResources() {
        return resources;
    }

    Deployer getDeployer() {
        return deployer;
    }
}
