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
 * Optional execution-tracking view implemented by sessions managed by the
 * built-in WebHarvest service.
 *
 * <p>Business completion ({@link ScraperSession#awaitCompletion()}) and worker
 * quiescence are separate facts. This interface exposes the latter without
 * changing the contract required of third-party {@link ScraperSession}
 * implementations.</p>
 */
public interface ExecutionTrackedSession extends ScraperSession {

    /** Returns the immutable runtime/session/generation owner identity. */
    ExecutionOwner getExecutionOwner();

    /** Returns the latest observed worker execution state. */
    ExecutionState getExecutionState();

    /** Returns the termination disposition, independently of business outcome. */
    TerminationDisposition getTerminationDisposition();

    /** Blocks until the worker is known to have exited. */
    void awaitExecutionCompletion() throws InterruptedException;

    /**
     * Blocks until worker exit is observed or the timeout expires.
     *
     * @return true only when worker exit has been observed
     */
    boolean awaitExecutionCompletion(long timeoutMillis) throws InterruptedException;
}
