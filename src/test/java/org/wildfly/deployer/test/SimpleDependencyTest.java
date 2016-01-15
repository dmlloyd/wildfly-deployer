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

package org.wildfly.deployer.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.wildfly.deployer.Deployer;
import org.wildfly.deployer.DeployerBuilder;
import org.wildfly.deployer.DeployerChain;
import org.wildfly.deployer.DeployerChainBuildException;
import org.wildfly.deployer.DeployerChainBuilder;
import org.wildfly.deployer.DeployerExecution;
import org.wildfly.deployer.DeployerExecutionBuilder;
import org.wildfly.deployer.SuccessfulDeploymentResult;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@FixMethodOrder(MethodSorters.DEFAULT)
public class SimpleDependencyTest {

    static volatile ExecutorService executorService;

    static final Logger log = Logger.getLogger(SimpleDependencyTest.class);

    @BeforeClass
    public static void setup() {
        executorService = Executors.newFixedThreadPool(8);
    }

    @Test
    public void emptyTest() throws DeployerChainBuildException, InterruptedException {
        log.info("Before test");
        final DeployerChainBuilder builder = DeployerChain.builder();
        final DeployerChain chain = builder.build();
        final DeployerExecutionBuilder executionBuilder = chain.createExecutionBuilder("my-app.jar");
        final DeployerExecution execution = executionBuilder.execute(executorService);
        assertEquals(DeployerExecution.Status.SUCCESSFUL, execution.await());
        log.info("After test");
    }

    @Test
    public void singleTest() throws DeployerChainBuildException, InterruptedException {
        log.info("Before test");
        final DeployerChainBuilder builder = DeployerChain.builder();
        final DeployerBuilder deployerBuilder = builder.addDeployer(context -> {
            log.info("In deployer now");
            assertTrue(context.setSucceeded());
        });
        deployerBuilder.produces("applesauce");
        final DeployerChain chain = builder.build();
        final DeployerExecutionBuilder executionBuilder = chain.createExecutionBuilder("my-app.jar");
        final DeployerExecution execution = executionBuilder.execute(executorService);
        assertEquals(DeployerExecution.Status.SUCCESSFUL, execution.await());
        log.info("After test");
    }

    @Test
    public void applesauce() throws InterruptedException, DeployerChainBuildException {
        log.info("Before test");
        final DeployerChainBuilder builder = DeployerChain.builder();
        builder.addFinalResource("applesauce");
        final Deployer applePickingDeployer = context -> {
            log.info("Picking an apple");
            context.produce("apples", new Object());
            assertTrue(context.setSucceeded());
        };
        builder.addDeployer(applePickingDeployer).contributesTo("apples");
        builder.addDeployer(applePickingDeployer).contributesTo("apples");
        builder.addDeployer(applePickingDeployer).contributesTo("apples");
        builder.addDeployer(applePickingDeployer).contributesTo("apples");
        builder.addDeployer(applePickingDeployer).contributesTo("apples");
        builder.addDeployer(applePickingDeployer).contributesTo("apples");
        builder.addDeployer(applePickingDeployer).contributesTo("apples");
        final DeployerBuilder masherBuilder = builder.addDeployer(context -> {
            log.info("Mashing the apples");
            assertNotNull(context.consumeMulti("apples"));
            context.produce("mashed apples", new Object());
            assertTrue(context.setSucceeded());
        });
        masherBuilder.consumes("apples");
        masherBuilder.produces("mashed apples");
        builder.addDeployer(context -> {
            log.info("Traveling to the orchard");
            assertTrue(context.setSucceeded());
        }).beforeProduce("apples");
        builder.addDeployer(context -> {
            log.info("Cooking the apples");
            assertNotNull(context.consume("mashed apples"));
            context.produce("applesauce", new Object());
            assertTrue(context.setSucceeded());
        }).consumes("mashed apples").produces("applesauce");
        builder.addDeployer(context -> {
            log.info("Eating the applesauce");
            assertNotNull(context.consume("applesauce"));
            assertTrue(context.setSucceeded());
        }).destroysMandatory("applesauce");
        final DeployerChain chain = builder.build();
        final DeployerExecutionBuilder executionBuilder = chain.createExecutionBuilder("my-app.jar");
        final DeployerExecution execution = executionBuilder.execute(executorService);
        assertEquals(DeployerExecution.Status.SUCCESSFUL, execution.await());
        final SuccessfulDeploymentResult result = execution.getSuccessfulResult();
        assertNotNull(result.consume("applesauce"));
        log.info("After test");
    }

    @AfterClass
    public static void teardown() throws InterruptedException {
        try {
            executorService.shutdown();
            executorService.awaitTermination(10L, TimeUnit.MINUTES);
        } finally {
            executorService = null;
        }
    }
}
