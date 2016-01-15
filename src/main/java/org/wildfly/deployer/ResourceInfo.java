/*
 * JBoss, Home of Professional Open Source.
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ResourceInfo {
    private final String name;
    private Multiplicity multiplicity = Multiplicity.SYMBOLIC;
    private final Map<Phase, Mode> modeMap = new EnumMap<>(Phase.class);
    private final Map<Phase, List<DeployerBuilderImpl>> resourceUsers = new EnumMap<>(Phase.class);

    ResourceInfo(final String name) {
        this.name = name;
    }

    synchronized Multiplicity getMultiplicity() {
        return multiplicity;
    }

    synchronized ResourceInfo setMultiplicity(final Multiplicity multiplicity) {
        this.multiplicity = this.multiplicity.combineWith(name, multiplicity);
        return this;
    }

    synchronized ResourceInfo add(Phase phase, final Mode mode, DeployerBuilderImpl builderImpl) {
        modeMap.compute(phase, (p, m) -> mode.max(m));
        resourceUsers.compute(phase, (p, deployerBuilders) -> {
            if (deployerBuilders == null) {
                return singletonList(builderImpl);
            }
            assert ! deployerBuilders.isEmpty();
            if (deployerBuilders.size() == 1) {
                ArrayList<DeployerBuilderImpl> l = new ArrayList<>();
                l.add(deployerBuilders.get(0));
                l.add(builderImpl);
                return l;
            } else {
                deployerBuilders.add(builderImpl);
                return deployerBuilders;
            }
        });
        return this;
    }

    synchronized List<DeployerBuilderImpl> getDeployersByPhase(Phase phase) {
        return resourceUsers.getOrDefault(phase, emptyList());
    }

    String getName() {
        return name;
    }

    synchronized Mode getMode(final Phase phase) {
        return modeMap.getOrDefault(phase, Mode.NONE);
    }
}
