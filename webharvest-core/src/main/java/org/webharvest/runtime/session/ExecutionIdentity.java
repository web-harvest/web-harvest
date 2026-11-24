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

/**
 * Immutable identity of one accepted execution generation inside a runtime.
 *
 * <p>Generation zero is reserved for legacy sessions that do not have an
 * explicit runtime composition root. Runtime-managed executions must use a
 * positive generation.</p>
 */
public final class ExecutionIdentity {

    private final String runtimeId;
    private final String sessionId;
    private final long executionGeneration;

    public ExecutionIdentity(String runtimeId, String sessionId, long executionGeneration) {
        if (runtimeId == null || runtimeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Runtime ID is mandatory.");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID is mandatory.");
        }
        if (executionGeneration < 0) {
            throw new IllegalArgumentException("Execution generation cannot be negative.");
        }
        this.runtimeId = runtimeId;
        this.sessionId = sessionId;
        this.executionGeneration = executionGeneration;
    }

    /**
     * Compatibility identity for third-party ScraperSession implementations.
     * Runtime-managed sessions must override this with an explicit identity.
     */
    public static ExecutionIdentity legacy(String sessionId) {
        return new ExecutionIdentity("legacy", sessionId, 0);
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

    public boolean isLegacy() {
        return executionGeneration == 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExecutionIdentity)) {
            return false;
        }
        ExecutionIdentity that = (ExecutionIdentity) other;
        return executionGeneration == that.executionGeneration
            && runtimeId.equals(that.runtimeId)
            && sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        int result = runtimeId.hashCode();
        result = 31 * result + sessionId.hashCode();
        result = 31 * result + Long.hashCode(executionGeneration);
        return result;
    }

    @Override
    public String toString() {
        return "ExecutionIdentity{runtimeId='" + runtimeId + "', sessionId='" + sessionId
            + "', executionGeneration=" + executionGeneration + '}';
    }
}
