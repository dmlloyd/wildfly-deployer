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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@SuppressWarnings("serial")
final class ConcurrentStack<E> extends AtomicReference<ConcurrentStack.Node<E>> {

    void push(E value) {
        Node<E> oldVal, newVal;
        do {
            oldVal = get();
            newVal = new Node<>(oldVal == null ? 1 : oldVal.length + 1, oldVal, value);
        } while (! compareAndSet(oldVal, newVal));
    }

    E pop() {
        Node<E> oldVal, newVal;
        do {
            oldVal = get();
            if (oldVal == null) {
                return null;
            }
            newVal = oldVal.next;
        } while (! compareAndSet(oldVal, newVal));
        return oldVal.value;
    }

    E[] popAll(IntFunction<E[]> arrayCreator) {
        Node<E> stack = getAndSet(null);
        if (stack == null) {
            return arrayCreator.apply(0);
        }
        final E[] array = arrayCreator.apply(stack.length);
        final int len = stack.length;
        for (int i = 0; i < len; i ++) {
            array[i] = stack.value;
            stack = stack.next;
        }
        assert stack == null;
        return array;
    }

    int length() {
        final Node<E> node = get();
        return node == null ? 0 : node.length;
    }

    static final class Node<E> {
        final int length;
        final Node<E> next;
        final E value;

        Node(final int length, final Node<E> next, final E value) {
            this.length = length;
            this.next = next;
            this.value = value;
        }
    }
}
