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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeployerExecutionBuilderImpl implements DeployerExecutionBuilder {
    private final DeployerChainImpl deployerChain;
    private final String name;
    private final Map<String, Object> initialSingleResources = new HashMap<>();
    private final Map<String, List<Object>> initialMultiResources = new HashMap<>();

    DeployerExecutionBuilderImpl(final DeployerChainImpl deployerChain, final String name) {
        this.deployerChain = deployerChain;
        this.name = name;
    }

    public String getDeploymentName() {
        return name;
    }

    public DeployerExecutionBuilder provide(final String name, final Object item) {
        final Map<String, Multiplicity> initialResources = deployerChain.getInitialResources();
        final Multiplicity multiplicity = initialResources.get(name);
        if (multiplicity != null) {
            if (multiplicity == Multiplicity.MULTIPLE) {
                initialMultiResources.computeIfAbsent(name, ignored -> new ArrayList<>()).add(item);
            } else if (multiplicity == Multiplicity.SINGLE) {
                if (initialSingleResources.putIfAbsent(name, item) != null) {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
        return this;
    }

    public DeployerExecutionBuilder provideAll(final String name, final Collection<?> items) {
        final Map<String, Multiplicity> initialResources = deployerChain.getInitialResources();
        final Multiplicity multiplicity = initialResources.get(name);
        if (multiplicity != null) {
            if (multiplicity == Multiplicity.MULTIPLE) {
                initialMultiResources.computeIfAbsent(name, ignored -> new ArrayList<>()).addAll(items);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
        return this;
    }

    public DeployerExecutionBuilder provideAll(final String name, final Object... items) {
        provideAll(name, Arrays.asList(items));
        return this;
    }

    public DeployerExecution execute(final Executor executor) {
        final Map<String, Multiplicity> initialResources = deployerChain.getInitialResources();
        for (Map.Entry<String, Multiplicity> entry : initialResources.entrySet()) {
            final String name = entry.getKey();
            final Multiplicity multiplicity = entry.getValue();
            if (multiplicity == Multiplicity.MULTIPLE) {
                if (! initialMultiResources.containsKey(name)) {
                    throw Messages.log.missingRequiredInitialResource(name);
                }
            } else if (multiplicity == Multiplicity.SINGLE) {
                if (! initialSingleResources.containsKey(name)) {
                    throw Messages.log.missingRequiredInitialResource(name);
                }
            }
        }
        final DeployerExecutionImpl deployerExecution = new DeployerExecutionImpl(deployerChain, executor, initialSingleResources, initialMultiResources, name);
        deployerExecution.run();
        return deployerExecution;
    }
}
