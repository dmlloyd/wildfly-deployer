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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFDPLY", length = 5)
interface Messages extends BasicLogger {
    Messages log = Logger.getMessageLogger(Messages.class, "org.wildfly.deployer");

    // Version information

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "WildFly Deployer version %s")
    void logVersion(String version);

    // General validation

    @Message(id = 1, value = "Invalid deployment execution state")
    IllegalStateException invalidDeploymentExecutionState();

    // Deployer chain integrity messages

    @Message(id = 100, value = "Single consumer found for multiply-produced resource in deployer \"%s\" (resource \"%s\")")
    DeployerChainBuildException singleProducerForMultipleResource(Deployer deployer, String resource);

    // no consumer

    @Message(id = 101, value = "No pre-consumer found for mandatory producer of resource \"%s\"")
    DeployerChainBuildException noPreConsumerForMandatoryInitial(String resource);

    @Message(id = 102, value = "No consumer found for mandatory producer of resource \"%s\"")
    DeployerChainBuildException noConsumerForMandatoryProducer(String resource);

    @Message(id = 103, value = "No destructor found for must-be-destroyed resource \"%s\"")
    DeployerChainBuildException noDestructorForMandatoryDestroy(String resource);

    // -- leave some space for future generations --

    // no producer

    @Message(id = 110, value = "No producer found for mandatory initializer of resource \"%s\"")
    DeployerChainBuildException noProducerForMandatoryInitializer(String resource);

    @Message(id = 111, value = "No producer found for mandatory transformer of resource \"%s\"")
    DeployerChainBuildException noProducerForMandatoryTransformer(String resource);

    @Message(id = 112, value = "No producer found for mandatory consumer of resource \"%s\"")
    DeployerChainBuildException noProducerForMandatoryConsumer(String resource);

    @Message(id = 113, value = "No producer found for mandatory destructor of resource \"%s\"")
    DeployerChainBuildException noProducerForMandatoryDestructor(String resource);

    // -- leave some space for future generations --

    @Message(id = 121, value = "Deployer chain loop detected in deployer \"%s\"")
    DeployerChainBuildException loopDetected(Deployer deployer);

    @Message(id = 122, value = "Deployment step failed")
    DeploymentException deploymentStepException(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id = 123, value = "A deployment step threw an uncaught exception")
    void uncaughtException(@Cause Throwable cause);

    @Message(id = 124, value = "A deployment step failed to execute")
    DeploymentException deploymentStepExecuteException(@Cause Throwable t);

    @LogMessage(level = ERROR)
    @Message(id = 125, value = "An action failed")
    void actionFailed(@Cause Throwable t);

    @Message(id = 126, value = "An incompatible relationship already exists for the resource named \"%s\"")
    IllegalArgumentException incompatibleExistingRelationship(String name);

    @Message(id = 127, value = "Missing a required initial resource named \"%s\"")
    IllegalArgumentException missingRequiredInitialResource(String name);

    @Message(id = 128, value = "Cannot produce a resource \"%s\" that is consumed by the same deployer")
    IllegalArgumentException cannotProduceAndConsume(String name);

    @Message(id = 129, value = "Cannot produce the same resource \"%s\" multiple times from a single deployer")
    IllegalArgumentException cannotProduceMultipleTimes(String name);

    @Message(id = 130, value = "Cannot consume the same resource \"%s\" multiple times from a single deployer")
    IllegalArgumentException cannotConsumeMultipleTimes(String name);

    @Message(id = 131, value = "Invalid attempt to define resource \"%s\" as both single and multiple")
    IllegalArgumentException changeBetweenSingleAndMultiple(String name);

    @Message(id = 132, value = "Cannot undeploy because an undeployment was already initiated")
    IllegalStateException alreadyUndeployed();

    @LogMessage(level = INFO)
    @Message(id = 200, value = "Deployer chain compiled successfully: %d resources defined for %d deployers in %dms")
    void constructed(int resources, int deployers, long millis);

    @Message(id = 300, value = "Cannot produce resource \"%s\" from here")
    IllegalArgumentException cannotProduce(String name);

    @Message(id = 301, value = "Cannot consume resource \"%s\" from here")
    IllegalArgumentException cannotConsume(String name);

    @Message(id = 302, value = "Cannot consume multiple resource \"%s\" as a single resource")
    IllegalArgumentException cannotConsumeMultipleResourceAsSingle(String name);

    @Message(id = 303, value = "Cannot consume single resource \"%s\" as a multiple resource")
    IllegalArgumentException cannotConsumeSingleResourceAsMultiple(String name);

    @LogMessage(level = INFO)
    @Message(id = 304, value = "Deployment of \"%s\" completed successfully in %dms")
    void executionComplete(String name, long duration);

    @LogMessage(level = INFO)
    @Message(id = 305, value = "Deployment of \"%s\" failed in %dms")
    void executionFailed(String name, long duration);
}
