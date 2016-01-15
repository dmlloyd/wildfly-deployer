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

import java.util.EnumSet;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
enum Multiplicity {
    SYMBOLIC,
    AUTOMATIC, // no producers yet declared
    SINGLE,
    MULTIPLE,
    ;

    private static final int fullSize = values().length;

    /**
     * Determine whether the given set is fully populated (or "full"), meaning it contains all possible values.
     *
     * @param set the set
     * @return {@code true} if the set is full, {@code false} otherwise
     */
    public static boolean isFull(final EnumSet<Multiplicity> set) {
        return set != null && set.size() == fullSize;
    }

    Multiplicity combineWith(final String name, final Multiplicity newVal) {
        if (this == newVal) {
            return this;
        }
        if (newVal == SYMBOLIC) {
            // symbolic always loses
            return this;
        }
        assert newVal.in(AUTOMATIC, SINGLE, MULTIPLE);
        if (this.in(SYMBOLIC, AUTOMATIC)) {
            // auto/single/multiple beats sym/auto
            return newVal;
        }
        assert this.in(SINGLE, MULTIPLE);
        if (newVal == AUTOMATIC) {
            // single/multi beats automatic
            return this;
        }
        assert newVal.in(SINGLE, MULTIPLE);
        // attempt to change between single & multiple
        throw Messages.log.changeBetweenSingleAndMultiple(name);
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param v1 the first instance
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final Multiplicity v1) {
        return this == v1;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param v1 the first instance
     * @param v2 the second instance
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final Multiplicity v1, final Multiplicity v2) {
        return this == v1 || this == v2;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param v1 the first instance
     * @param v2 the second instance
     * @param v3 the third instance
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final Multiplicity v1, final Multiplicity v2, final Multiplicity v3) {
        return this == v1 || this == v2 || this == v3;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param values the possible values
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final Multiplicity... values) {
        if (values != null) for (Multiplicity value : values) {
            if (this == value) return true;
        }
        return false;
    }

}
