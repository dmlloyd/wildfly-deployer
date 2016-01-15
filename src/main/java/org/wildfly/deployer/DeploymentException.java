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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DeploymentException extends Exception {
    private static final long serialVersionUID = - 2190774463525631311L;

    private final Location location;

    /**
     * Constructs a new {@code DeploymentException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     * @param location the location of the error
     */
    public DeploymentException(final String msg, final Location location) {
        super(msg);
        this.location = location;
    }

    /**
     * Constructs a new {@code DeploymentException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     * @param location the location of the error
     */
    public DeploymentException(final String msg, final Throwable cause, final Location location) {
        super(msg, cause);
        this.location = location;
    }

    /**
     * Constructs a new {@code DeploymentException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public DeploymentException(final String msg) {
        this(msg, (Location) null);
    }

    /**
     * Constructs a new {@code DeploymentException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public DeploymentException(final String msg, final Throwable cause) {
        this(msg, cause, null);
    }

    /**
     * Get the location at which the problem occurred, or {@code null} if there is no relevant location.
     *
     * @return the location, or {@code null} if there was none
     */
    public Location getLocation() {
        return location;
    }
}
