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
 * A single atomic unit of deployment work.  A deployer either succeeds or it does not, with no intermediate states
 * possible.  A deployer must eventually report a result to the deployment context in order for deployment to complete.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@FunctionalInterface
public interface Deployer {
    /**
     * Execute a deploy step.  The deploy step <em>must</em> eventually call one of the following methods:
     * <ul>
     *     <li>{@link DeploymentContext#setSucceeded()}</li>
     *     <li>{@link DeploymentContext#setFailed(DeploymentException)}</li>
     *     <li>{@link DeploymentContext#setCancelled()}</li>
     * </ul>
     *
     * @param context the context of the deploy operation (not {@code null})
     */
    void deploy(DeploymentContext context);

    /**
     * The empty deployer, which immediately succeeds.
     */
    Deployer EMPTY = DeploymentContext::setSucceeded;
}
