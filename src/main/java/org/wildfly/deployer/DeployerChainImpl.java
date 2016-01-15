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

import java.util.Map;
import java.util.Set;

import org.wildfly.common.Assert;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeployerChainImpl implements DeployerChain {
    private final Map<String, Multiplicity> initialResources;
    private final Set<String> finalResources;
    private final Set<DeployerInfo> initialDeployers;
    private final Set<DeployerInfo> finalDeployers;

    DeployerChainImpl(final Map<String, Multiplicity> initialResources, final Set<String> finalResources, final Set<DeployerInfo> initialDeployers, final Set<DeployerInfo> finalDeployers) {
        this.initialResources = initialResources;
        this.finalResources = finalResources;
        this.initialDeployers = initialDeployers;
        this.finalDeployers = finalDeployers;
    }

    Map<String, Multiplicity> getInitialResources() {
        return initialResources;
    }

    Set<String> getFinalResources() {
        return finalResources;
    }

    Set<DeployerInfo> getInitialDeployers() {
        return initialDeployers;
    }

    Set<DeployerInfo> getFinalDeployers() {
        return finalDeployers;
    }

    public DeployerExecutionBuilder createExecutionBuilder(final String name) {
        Assert.checkNotNullParam("name", name);
        return new DeployerExecutionBuilderImpl(this, name);
    }
}
