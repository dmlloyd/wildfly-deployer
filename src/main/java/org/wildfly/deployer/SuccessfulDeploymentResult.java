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

import org.wildfly.common.Assert;

/**
 * The final result of a successful deployment operation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface SuccessfulDeploymentResult extends DeploymentResult {
    /**
     * Consume the value produced for the named resource, casting the result to the given type.
     *
     * @param name the resource name (must not be {@code null})
     * @param type the resource type (must not be {@code null})
     * @return the produced resource (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code name}, or if {@code name} or {@code type} is {@code null}
     * @throws ClassCastException if the cast failed
     */
    default <T> T consume(Class<T> type, String name) {
        Assert.checkNotNullParam("type", type);
        return type.cast(consume(name));
    }

    /**
     * Consume the value produced for the named resource.
     *
     * @param name the resource name (must not be {@code null})
     * @return the produced resource (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code name}, or if {@code name} is {@code null}
     */
    Object consume(String name);

    /**
     * Consume all of the values produced for the named resource, casting the collection to the given type.
     *
     * @param name the resource name (must not be {@code null})
     * @param type the resource element type (must not be {@code null})
     * @return the produced resources (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code name}, or if {@code name} or {@code type} is {@code null}
     */
    @SuppressWarnings("unchecked")
    default <T> Collection<T> consumeMulti(String name, Class<T> type) {
        Assert.checkNotNullParam("type", type);
        return (Collection<T>) consumeMulti(name);
    }

    /**
     * Consume all of the values produced for the named resource.
     *
     * @param name the resource name (must not be {@code null})
     * @return the produced resources (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code name}, or if {@code name} is {@code null}
     */
    Collection<?> consumeMulti(String name) throws IllegalArgumentException;
}
