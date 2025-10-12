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

package org.webharvest.runtime.tracking;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable snapshot of resource usage for token tracking.
 * 
 * <p>This class provides a point-in-time view of resource consumption
 * across different resource types. Instances are immutable and thread-safe.</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * TokenUsage usage = tracker.getUsage();
 * 
 * System.out.println("HTTP Requests: " + usage.getHttpRequests());
 * System.out.println("Bytes Downloaded: " + usage.getHttpBytes());
 * System.out.println("CPU Time: " + usage.getCpuTimeMillis() + "ms");
 * System.out.println("Peak Memory: " + usage.getMemoryPeakBytes() + " bytes");
 * 
 * // Generic access
 * long requests = usage.get(ResourceType.HTTP_REQUEST);
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see TokenTracker
 * @see ResourceType
 */
public final class TokenUsage {
    
    private final Map<ResourceType, Long> usage;
    
    /**
     * Creates a token usage snapshot from a map of resource values.
     * 
     * @param usage map of resource types to their usage values
     */
    public TokenUsage(Map<ResourceType, Long> usage) {
        this.usage = Collections.unmodifiableMap(
            new EnumMap<>(usage != null ? usage : Collections.emptyMap())
        );
    }
    
    /**
     * Returns the usage value for a specific resource type.
     * 
     * @param type the resource type
     * @return the usage value, or 0 if not tracked
     */
    public long get(ResourceType type) {
        return usage.getOrDefault(type, 0L);
    }
    
    /**
     * Returns the number of HTTP requests made.
     * 
     * @return HTTP request count
     */
    public long getHttpRequests() {
        return get(ResourceType.HTTP_REQUEST);
    }
    
    /**
     * Returns the total bytes downloaded via HTTP.
     * 
     * @return bytes downloaded
     */
    public long getHttpBytes() {
        return get(ResourceType.HTTP_BYTES);
    }
    
    /**
     * Returns the total CPU time consumed in milliseconds.
     * 
     * @return CPU time in milliseconds
     */
    public long getCpuTimeMillis() {
        return get(ResourceType.CPU_TIME);
    }
    
    /**
     * Returns the peak memory usage in bytes.
     * 
     * @return peak memory in bytes
     */
    public long getMemoryPeakBytes() {
        return get(ResourceType.MEMORY_PEAK);
    }
    
    /**
     * Returns all tracked usage as an immutable map.
     * 
     * @return unmodifiable map of all usage values
     */
    public Map<ResourceType, Long> getAll() {
        return usage;
    }
    
    /**
     * Checks if any resources have been consumed.
     * 
     * @return true if all usage values are zero; false otherwise
     */
    public boolean isEmpty() {
        return usage.values().stream().allMatch(v -> v == 0);
    }
    
    @Override
    public String toString() {
        return "TokenUsage{" +
                "httpRequests=" + getHttpRequests() +
                ", httpBytes=" + getHttpBytes() +
                ", cpuTimeMs=" + getCpuTimeMillis() +
                ", memoryPeakBytes=" + getMemoryPeakBytes() +
                '}';
    }
}


