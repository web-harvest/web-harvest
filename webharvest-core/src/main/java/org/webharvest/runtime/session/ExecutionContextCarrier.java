/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.runtime.session;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Explicit per-thread execution identity carrier.
 *
 * <p>The stack supports nested runtimes/executions. Scope tokens are
 * thread-bound and LIFO; the ThreadLocal is removed as soon as the stack is
 * empty. Async work must capture a {@link ContextToken} and install it on the
 * destination thread instead of relying on implicit inheritance.</p>
 */
public final class ExecutionContextCarrier {

    private static final ThreadLocal<Deque<ExecutionIdentity>> CURRENT = new ThreadLocal<>();

    private ExecutionContextCarrier() {
    }

    public static ScopeToken install(ExecutionIdentity identity) {
        if (identity == null) {
            throw new IllegalArgumentException("Execution identity is mandatory.");
        }
        Deque<ExecutionIdentity> stack = CURRENT.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            CURRENT.set(stack);
        }
        stack.push(identity);
        return new ScopeToken(stack, identity, Thread.currentThread());
    }

    public static Optional<ExecutionIdentity> currentIdentity() {
        Deque<ExecutionIdentity> stack = CURRENT.get();
        if (stack == null || stack.isEmpty()) {
            CURRENT.remove();
            return Optional.empty();
        }
        return Optional.of(stack.peek());
    }

    public static ContextToken capture() {
        ExecutionIdentity identity = currentIdentity().orElseThrow(
            () -> new IllegalStateException("No execution identity is installed on this thread."));
        return new ContextToken(identity);
    }

    public static final class ContextToken {
        private final ExecutionIdentity identity;

        private ContextToken(ExecutionIdentity identity) {
            this.identity = identity;
        }

        public ExecutionIdentity getIdentity() {
            return identity;
        }

        public ScopeToken install() {
            return ExecutionContextCarrier.install(identity);
        }
    }

    public static final class ScopeToken implements AutoCloseable {
        private final Deque<ExecutionIdentity> stack;
        private final ExecutionIdentity identity;
        private final Thread ownerThread;
        private boolean closed;

        private ScopeToken(Deque<ExecutionIdentity> stack,
                           ExecutionIdentity identity,
                           Thread ownerThread) {
            this.stack = stack;
            this.identity = identity;
            this.ownerThread = ownerThread;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Thread.currentThread() != ownerThread) {
                throw new IllegalStateException("Execution context scope must close on its owner thread.");
            }
            Deque<ExecutionIdentity> current = CURRENT.get();
            if (current != stack || current.isEmpty() || !identity.equals(current.peek())) {
                throw new IllegalStateException("Execution context scopes must close in LIFO order.");
            }
            current.pop();
            if (current.isEmpty()) {
                CURRENT.remove();
            }
            closed = true;
        }
    }
}
