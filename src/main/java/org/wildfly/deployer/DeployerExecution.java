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
 * An execution of a deployer chain.  If the deployment fails, the failed deployment will remain partially deployed until
 * it is undeployed.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface DeployerExecution extends Execution {

    void cancel();

    Status getStatus();

    Status await() throws InterruptedException;

    /**
     * Get the successful deployment result.  This method should only be called if {@link #getStatus()}
     * has returned {@link Status#SUCCESSFUL}.
     *
     * @return the deployment result
     * @throws IllegalStateException if the deployment operation is not yet complete or has failed
     */
    SuccessfulDeploymentResult getSuccessfulResult() throws IllegalStateException;

    /**
     * Get the failed deployment result.  This method should only be called if {@link #getStatus()}
     * has returned {@link Status#FAILED}.
     *
     * @return the deployment result
     * @throws IllegalStateException if the deployment operation is not yet complete or did not fail
     */
    FailedDeploymentResult getFailedResult() throws IllegalStateException;

    /**
     * Possible statuses of an ongoing deployment operation.
     */
    enum Status {
        /**
         * The deployment is presently executing.
         */
        EXECUTING,
        /**
         * The deployment was successfully cancelled.
         */
        CANCELLED,
        /**
         * The deployment operation has failed.
         */
        FAILED,
        /**
         * The deployment operation has succeeded.
         */
        SUCCESSFUL,
        ;
    }
}
