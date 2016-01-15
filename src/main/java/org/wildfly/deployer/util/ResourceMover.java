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

package org.wildfly.deployer.util;

import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.common.Assert;
import org.wildfly.deployer.Deployer;
import org.wildfly.deployer.DeploymentContext;

/**
 * A mover to efficiently bring a single resource from one deployment chain to another.  Multiple resources are not
 * supported; instead, to move multiple items, a single collection object should be moved.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ResourceMover {
    private final Deployer sourceDeployer;
    private final Deployer targetDeployer;

    private static final Object UNSET = new Object();

    /**
     * Construct a new instance.
     *
     * @param sourceResourceName the resource name to consume from the source chain (must not be {@code null})
     * @param targetResourceName the resource name to produce in the target chain (must not be {@code null})
     */
    public ResourceMover(String sourceResourceName, String targetResourceName) {
        Assert.checkNotNullParam("sourceResourceName", sourceResourceName);
        Assert.checkNotNullParam("targetResourceName", targetResourceName);
        final AtomicReference<Object> holder = new AtomicReference<>(UNSET);
        sourceDeployer = context -> {
            final Object resource = context.consume(sourceResourceName);
            Object oldVal, newVal;
            do {
                oldVal = holder.get();
                if (oldVal == UNSET) {
                    newVal = resource;
                } else if (oldVal instanceof DeploymentContext) {
                    final DeploymentContext otherContext = (DeploymentContext) oldVal;
                    otherContext.produce(targetResourceName, resource);
                    otherContext.setSucceeded();
                    holder.set(null);
                    return;
                } else {
                    throw Assert.unreachableCode();
                }
            } while (! holder.compareAndSet(oldVal, newVal));
        };
        targetDeployer = context -> {
            Object oldVal, newVal;
            do {
                oldVal = holder.get();
                if (oldVal == UNSET) {
                    newVal = context;
                } else {
                    newVal = null;
                }
            } while (! holder.compareAndSet(oldVal, newVal));
            if (oldVal != UNSET) {
                context.produce(targetResourceName, oldVal);
                context.setSucceeded();
                holder.set(null);
            }
            // else fall out
        };
    }

    /**
     * Construct a new instance.
     *
     * @param resourceName the resource name to consume from the source chain and produce in the target chain (must not be {@code null})
     */
    public ResourceMover(String resourceName) {
        this(resourceName, resourceName);
    }

    /**
     * Get the source deployer.
     *
     * @return the source deployer (not {@code null})
     */
    public Deployer getSourceDeployer() {
        return sourceDeployer;
    }

    /**
     * Get the target deployer.
     *
     * @return the target deployer (not {@code null})
     */
    public Deployer getTargetDeployer() {
        return targetDeployer;
    }
}
