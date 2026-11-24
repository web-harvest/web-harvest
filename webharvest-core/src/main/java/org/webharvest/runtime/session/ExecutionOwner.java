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

import java.util.Objects;

/**
 * Immutable identity for one execution owner within a WebHarvest runtime.
 *
 * <p>The generation distinguishes successive execution ownership even when
 * external systems reuse a session identifier. It is not a mutable lifecycle
 * or health record.</p>
 */
public final class ExecutionOwner {

    private final String runtimeId;
    private final String sessionId;
    private final long executionGeneration;

    public ExecutionOwner(String runtimeId, String sessionId, long executionGeneration) {
        if (runtimeId == null || runtimeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Runtime ID cannot be null or empty");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (executionGeneration < 1) {
            throw new IllegalArgumentException("Execution generation must be positive");
        }
        this.runtimeId = runtimeId;
        this.sessionId = sessionId;
        this.executionGeneration = executionGeneration;
    }

    public String getRuntimeId() {
        return runtimeId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getExecutionGeneration() {
        return executionGeneration;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExecutionOwner)) {
            return false;
        }
        ExecutionOwner that = (ExecutionOwner) other;
        return executionGeneration == that.executionGeneration
            && runtimeId.equals(that.runtimeId)
            && sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runtimeId, sessionId, executionGeneration);
    }

    @Override
    public String toString() {
        return "ExecutionOwner{runtimeId='" + runtimeId + "', sessionId='"
            + sessionId + "', executionGeneration=" + executionGeneration + '}';
    }
}
