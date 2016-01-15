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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
enum Phase {
    PRE_PRODUCE,
    PRODUCE,
    POST_PRODUCE,
    PRE_TRANSFORM,
    TRANSFORM,
    POST_TRANSFORM,
    PRE_CONSUME,
    CONSUME,
    POST_CONSUME,
    PRE_DESTROY,
    DESTROY,
    POST_DESTROY,
    ;

    private static final Phase[] VALUES = values();
    private static final List<Phase> FWD_LIST = Arrays.asList(VALUES);
    private static final List<Phase> REV_LIST;
    private static final int fullSize = VALUES.length;

    static {
        List<Phase> l = Arrays.asList(VALUES.clone());
        Collections.reverse(l);
        REV_LIST = l;
    }

    /**
     * Determine whether the given set is fully populated (or "full"), meaning it contains all possible values.
     *
     * @param set the set
     * @return {@code true} if the set is full, {@code false} otherwise
     */
    public static boolean isFull(final EnumSet<Phase> set) {
        return set != null && set.size() == fullSize;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param v1 the first instance
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final Phase v1) {
        return this == v1;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param v1 the first instance
     * @param v2 the second instance
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final Phase v1, final Phase v2) {
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
    public boolean in(final Phase v1, final Phase v2, final Phase v3) {
        return this == v1 || this == v2 || this == v3;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param values the possible values
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final Phase... values) {
        if (values != null) for (Phase value : values) {
            if (this == value) return true;
        }
        return false;
    }

    boolean hasNext() {
        return ordinal() > 0;
    }

    boolean hasPrevious() {
        return ordinal() < fullSize - 1;
    }

    Phase previous() {
        final int ord = ordinal();
        return ord == 0 ? null : VALUES[ord - 1];
    }

    Phase next() {
        final int ord = ordinal();
        return ord == VALUES.length - 1 ? null : VALUES[ord + 1];
    }

    boolean comesAfter(final Phase generation) {
        return this.compareTo(generation) > 0;
    }

    boolean comesBefore(final Phase generation) {
        return this.compareTo(generation) < 0;
    }

    ListIterator<Phase> forwardIterate() {
        return FWD_LIST.listIterator(ordinal());
    }

    ListIterator<Phase> reverseIterate() {
        return REV_LIST.listIterator(ordinal());
    }
}
