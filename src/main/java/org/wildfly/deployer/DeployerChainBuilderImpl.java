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

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeployerChainBuilderImpl implements DeployerChainBuilder {
    static {
        Messages.log.logVersion(Version.getVersion());
    }

    private final Map<String, ResourceInfo> allResources;
    private final List<DeployerBuilderImpl> allDeployerBuilders;
    private final Set<String> initialResources;
    private final Set<String> finalResources;

    DeployerChainBuilderImpl() {
        finalResources = newSetFromMap(new ConcurrentHashMap<>());
        initialResources = newSetFromMap(new ConcurrentHashMap<>());
        allDeployerBuilders = synchronizedList(new ArrayList<>());
        allResources = new ConcurrentHashMap<>();
    }

    public DeployerBuilder addDeployer(final Deployer deployer) {
        final DeployerBuilderImpl deployerBuilder = new DeployerBuilderImpl(this, deployer);
        allDeployerBuilders.add(deployerBuilder);
        return deployerBuilder;
    }

    public DeployerChainBuilder addInitialResource(final String name) {
        allResources.computeIfAbsent(name, s -> new ResourceInfo(name)).setMultiplicity(Multiplicity.SINGLE);
        initialResources.add(name);
        return this;
    }

    public DeployerChainBuilder addInitialMultiResource(final String name) {
        allResources.computeIfAbsent(name, s -> new ResourceInfo(name)).setMultiplicity(Multiplicity.MULTIPLE);
        initialResources.add(name);
        return this;
    }

    public DeployerChainBuilder addFinalResource(final String name) {
        allResources.computeIfAbsent(name, s -> new ResourceInfo(name));
        finalResources.add(name);
        return this;
    }

    ResourceInfo addResource(final String name, final Phase phase, final Multiplicity multiplicity, final Mode mode, final DeployerBuilderImpl builder) {
        return allResources.computeIfAbsent(name, s -> new ResourceInfo(name)).setMultiplicity(multiplicity).add(phase, mode, builder);
    }

    private boolean requiredFailed(ResourceInfo resourceInfo, Phase source, Phase target) {
        return resourceInfo.getMode(source) == Mode.MANDATORY && resourceInfo.getDeployersByPhase(target).isEmpty();
    }

    @SuppressWarnings("serial")
    public DeployerChain build() throws DeployerChainBuildException {
        final long startTime = System.nanoTime();
        Map<String, ResourceInfo> allResources = this.allResources;

        // match producers with consumers

        for (ResourceInfo resourceInfo : allResources.values()) {
            final String name = resourceInfo.getName();

            // Ensure there's a producer for every required producer predecessor
            if (requiredFailed(resourceInfo, Phase.PRE_PRODUCE, Phase.PRODUCE)) {
                throw Messages.log.noProducerForMandatoryInitializer(name);
            }

            // Ensure there's a producer for every required transformer
            if (requiredFailed(resourceInfo, Phase.TRANSFORM, Phase.PRODUCE)) {
                throw Messages.log.noProducerForMandatoryTransformer(name);
            }

            // Ensure there's a consumer for every required producer
            if (requiredFailed(resourceInfo, Phase.PRODUCE, Phase.CONSUME)) {
                throw Messages.log.noConsumerForMandatoryProducer(name);
            }

            // Ensure there's a consumer for every required transformer producer
            if (requiredFailed(resourceInfo, Phase.TRANSFORM, Phase.CONSUME)) {
                throw Messages.log.noProducerForMandatoryTransformer(name);
            }

            // Ensure there's a producer for every required consumer
            if (requiredFailed(resourceInfo, Phase.CONSUME, Phase.PRODUCE)) {
                throw Messages.log.noProducerForMandatoryConsumer(name);
            }
        }

        // Construct deployer infos, checking for loops and optimizing dependencies where possible

        final Set<DeployerInfo> initialDeployers = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<DeployerInfo> finalDeployers = Collections.newSetFromMap(new IdentityHashMap<>());

        final Map<DeployerBuilderImpl, DeployerInfo> cachedDeployerInfos = new IdentityHashMap<>(allDeployerBuilders.size());
        final Set<DeployerBuilderImpl> inProgress = Collections.newSetFromMap(new IdentityHashMap<>());

        for (final DeployerBuilderImpl builder : allDeployerBuilders) {
            createOne(builder, inProgress, cachedDeployerInfos);
        }

        for (DeployerInfo deployerInfo : cachedDeployerInfos.values()) {
            if (deployerInfo.getDependents().isEmpty()) {
                finalDeployers.add(deployerInfo);
            }
            if (deployerInfo.getDependencies().isEmpty()) {
                initialDeployers.add(deployerInfo);
            }
        }

        Messages.log.constructed(allResources.size(), allDeployerBuilders.size(), Math.max(0L, System.nanoTime() - startTime) / 1000000L);

        Map<String, Multiplicity> initialResourcesMap = new HashMap<>(initialResources.size());
        for (String name : initialResources) {
            initialResourcesMap.put(name, allResources.get(name).getMultiplicity());
        }

        return new DeployerChainImpl(initialResourcesMap, finalResources, initialDeployers, finalDeployers);
    }

    private DeployerInfo createOne(final DeployerBuilderImpl builder, final Set<DeployerBuilderImpl> inProgress, final Map<DeployerBuilderImpl, DeployerInfo> cachedDeployerInfos) throws DeployerChainBuildException {
        final DeployerInfo cached = cachedDeployerInfos.get(builder);
        if (cached != null) {
            return cached;
        }
        if (! inProgress.add(builder)) {
            throw Messages.log.loopDetected(builder.getDeployer());
        }
        final DeployerInfo deployerInfo;
        final Set<DeployerInfo> dependencies;
        try {

            // optimize for empty dependencies/dependents
            dependencies = new HashSet<>();

            // construct the (unwired) deployer info

            Map<String, Multiplicity> produces = emptyMap();
            Map<String, Multiplicity> consumes = emptyMap();

            final Map<String, Phase> resources = builder.getResources();
            for (Map.Entry<String, Phase> entry : resources.entrySet()) {
                final String name = entry.getKey();
                final Phase phase = entry.getValue();
                final Multiplicity multiplicity = allResources.get(name).getMultiplicity();
                // calculate produces / consumes
                if (multiplicity != Multiplicity.SYMBOLIC) {
                    if (phase == Phase.CONSUME || phase == Phase.DESTROY) {
                        if (consumes.isEmpty()) {
                            consumes = singletonMap(name, multiplicity);
                        } else if (consumes.size() == 1) {
                            consumes = new HashMap<>(consumes);
                            consumes.put(name, multiplicity);
                        } else {
                            consumes.put(name, multiplicity);
                        }
                    } else if (phase == Phase.PRODUCE) {
                        assert multiplicity != Multiplicity.AUTOMATIC; // should be impossible
                        if (produces.isEmpty()) {
                            produces = singletonMap(name, multiplicity);
                        } else if (produces.size() == 1) {
                            produces = new HashMap<>(produces);
                            produces.put(name, multiplicity);
                        } else {
                            produces.put(name, multiplicity);
                        }
                    }
                }
                // calculate dependencies
                ResourceInfo resourceInfo = allResources.get(name);
                for (Phase targetPhase = phase.previous(); targetPhase != null; targetPhase = targetPhase.previous()) {
                    addPhaseDependencies(resourceInfo, targetPhase, dependencies, inProgress, cachedDeployerInfos);
                }
            }

            deployerInfo = new DeployerInfo(
                builder.getDeployer(),
                consumes,
                produces,
                dependencies,
                new HashSet<>()
            );

            for (DeployerInfo dependency : dependencies) {
                dependency.getDependents().add(deployerInfo);
            }

            cachedDeployerInfos.put(builder, deployerInfo);
        } finally {
            inProgress.remove(builder);
        }

        for (DeployerInfo dependency : dependencies) {
            dependency.getDependents().add(deployerInfo);
        }

        return deployerInfo;
    }

    private void addPhaseDependencies(final ResourceInfo resourceInfo, final Phase dependencyPhase, final Set<DeployerInfo> dependencies, final Set<DeployerBuilderImpl> inProgress, final Map<DeployerBuilderImpl, DeployerInfo> cachedDeployerInfos) throws DeployerChainBuildException {
        for (DeployerBuilderImpl deployerBuilder : resourceInfo.getDeployersByPhase(dependencyPhase)) {
            final DeployerInfo newDependency = createOne(deployerBuilder, inProgress, cachedDeployerInfos);
            addDependency(newDependency, dependencies);
        }
    }

    private static void addDependency(final DeployerInfo newDependency, final Set<DeployerInfo> dependencies) {
        final Iterator<DeployerInfo> iterator = dependencies.iterator();
        while (iterator.hasNext()) {
            final DeployerInfo myDependency = iterator.next();
            if (myDependency.implies(newDependency)) {
                // an existing dependency already implies the new dependency
                return;
            } else if (newDependency.implies(myDependency)) {
                // the new dependency is better because it also implies the existing one
                iterator.remove();
            }
        }
        dependencies.add(newDependency);
    }
}
