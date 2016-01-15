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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Information about a single deployer in a chain.  Only one of these exists per deployment per chain, and is shared
 * among all deployment executions.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class DeployerInfo {
    private final Deployer deployer;
    // consumed resources, not counting succeeds
    private final Map<String, Multiplicity> consumed;
    // produced resources, not counting precedes
    private final Map<String, Multiplicity> produced;
    private final Set<DeployerInfo> dependencies;
    private final Set<DeployerInfo> dependents;

    DeployerInfo(final Deployer deployer, final Map<String, Multiplicity> consumed, final Map<String, Multiplicity> produced, final Set<DeployerInfo> dependencies, final Set<DeployerInfo> dependents) {
        this.deployer = deployer;
        this.consumed = consumed;
        this.produced = produced;
        this.dependencies = dependencies;
        this.dependents = dependents;
    }

    Deployer getDeployer() {
        return deployer;
    }

    Map<String, Multiplicity> getProduced() {
        return produced;
    }

    Map<String, Multiplicity> getConsumed() {
        return consumed;
    }

    Set<DeployerInfo> getDependencies() {
        return dependencies;
    }

    Set<DeployerInfo> getDependents() {
        return dependents;
    }

    private static final ThreadLocal<HashSet<DeployerInfo>> visited = ThreadLocal.withInitial(HashSet::new);

    boolean implies(DeployerInfo other) {
        HashSet<DeployerInfo> visitedSet = getVisitedSetCache();
        try {
            return implies(other, visitedSet);
        } finally {
            visitedSet.clear();
        }
    }

    static HashSet<DeployerInfo> getVisitedSetCache() {
        return visited.get();
    }

    boolean implies(DeployerInfo other, HashSet<DeployerInfo> visitedSet) {
        if (visitedSet.add(this)) {
            if (dependencies.contains(other)) {
                return true;
            }
            for (DeployerInfo dependency : dependencies) {
                if (dependency.implies(other, visitedSet)) {
                    return true;
                }
            }
        }
        return false;
    }
}
