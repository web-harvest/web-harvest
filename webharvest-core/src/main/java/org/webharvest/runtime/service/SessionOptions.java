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

package org.webharvest.runtime.service;

import java.time.Duration;

/**
 * Configuration options for session execution.
 * 
 * <p>Immutable configuration object created using builder pattern.</p>
 * 
 * <p><strong>Current limitations:</strong> The built-in service enforces the timeout
 * option. The token-tracking flag does not currently connect runtime events to
 * {@code TokenTracker} (see <a href="https://github.com/web-harvest/web-harvest/issues/168">issue #168</a>),
 * and {@code maxElements} is stored but not enforced (see
 * <a href="https://github.com/web-harvest/web-harvest/issues/307">issue #307</a>).</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * SessionOptions options = SessionOptions.builder()
 *     .timeout(Duration.ofMinutes(30))
 *     .trackTokens(true)
 *     .maxElements(10000)
 *     .build();
 * 
 * ScraperSession session = service.executeAsync(config, client, options);
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 */
public final class SessionOptions {
    
    private final Duration timeout;
    private final boolean trackTokens;
    private final long maxElements;
    
    /**
     * Default options with sensible defaults.
     */
    public static final SessionOptions DEFAULT = builder().build();
    
    private SessionOptions(Builder builder) {
        this.timeout = builder.timeout;
        this.trackTokens = builder.trackTokens;
        this.maxElements = builder.maxElements;
    }
    
    /**
     * Returns the execution timeout.
     * 
     * @return timeout duration, or null for unlimited
     */
    public Duration getTimeout() {
        return timeout;
    }
    
    /**
     * Checks if timeout is set.
     * 
     * @return true if timeout is configured; false for unlimited execution
     */
    public boolean hasTimeout() {
        return timeout != null;
    }
    
    /**
     * Returns whether token tracking was requested.
     * 
     * <p>The built-in service currently does not record runtime resource events
     * into the tracker; see issue #168.</p>
     * 
     * @return true if token tracking was requested
     */
    public boolean isTrackTokens() {
        return trackTokens;
    }
    
    /**
     * Returns the configured element-limit request.
     * 
     * <p>The built-in service currently does not enforce this value; see issue #307.</p>
     * 
     * @return configured maximum, or 0 for unlimited
     */
    public long getMaxElements() {
        return maxElements;
    }
    
    /**
     * Checks if element limit is set.
     * 
     * @return true if max elements is configured; false for unlimited
     */
    public boolean hasMaxElements() {
        return maxElements > 0;
    }
    
    /**
     * Creates a new builder.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return "SessionOptions{" +
                "timeout=" + timeout +
                ", trackTokens=" + trackTokens +
                ", maxElements=" + maxElements +
                '}';
    }
    
    /**
     * Builder for {@link SessionOptions}.
     */
    public static final class Builder {
        private Duration timeout = null;  // No timeout by default
        private boolean trackTokens = true;  // Track by default
        private long maxElements = 0;  // No limit by default
        
        private Builder() {}
        
        /**
         * Sets the execution timeout.
         * 
         * @param timeout the timeout duration, or null for unlimited
         * @return this builder
         * @throws IllegalArgumentException if timeout is negative or zero
         */
        public Builder timeout(Duration timeout) {
            if (timeout != null && (timeout.isNegative() || timeout.isZero())) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            this.timeout = timeout;
            return this;
        }
        
        /**
         * Requests token tracker initialization.
         * 
         * <p>Runtime resource-event recording is not yet connected; see issue #168.</p>
         * 
         * @param trackTokens true to request tracker initialization
         * @return this builder
         */
        public Builder trackTokens(boolean trackTokens) {
            this.trackTokens = trackTokens;
            return this;
        }
        
        /**
         * Sets the requested maximum number of elements.
         * 
         * <p>The built-in service currently stores but does not enforce this value;
         * see issue #307.</p>
         * 
         * @param maxElements maximum elements, or 0 for unlimited
         * @return this builder
         * @throws IllegalArgumentException if maxElements is negative
         */
        public Builder maxElements(long maxElements) {
            if (maxElements < 0) {
                throw new IllegalArgumentException("Max elements cannot be negative");
            }
            this.maxElements = maxElements;
            return this;
        }
        
        /**
         * Builds the options.
         * 
         * @return a new immutable SessionOptions instance
         */
        public SessionOptions build() {
            return new SessionOptions(this);
        }
    }
}






