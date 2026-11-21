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

/**
 * Resource categories supported by {@link TokenTracker}.
 * 
 * <p>These values define counter names and units only. They do not imply that the
 * built-in runtime measures each resource, and they do not define billing rates,
 * pricing, or quota policy. Runtime event integration remains tracked by issue #168.</p>
 * 
 * <h2>Resource Types:</h2>
 * <ul>
 *   <li><b>HTTP_REQUEST</b> - Number of HTTP requests made</li>
 *   <li><b>HTTP_BYTES</b> - Total bytes downloaded via HTTP</li>
 *   <li><b>CPU_TIME</b> - CPU time consumed (milliseconds)</li>
 *   <li><b>MEMORY_PEAK</b> - Peak memory usage (bytes)</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * TokenTracker tracker = new TokenTrackerImpl("session-123", "client-123");
 * 
 * // Record HTTP request
 * tracker.record(ResourceType.HTTP_REQUEST, 1);
 * tracker.record(ResourceType.HTTP_BYTES, 52428);
 * 
 * // Record CPU time
 * long start = System.currentTimeMillis();
 * // ... processing ...
 * long duration = System.currentTimeMillis() - start;
 * tracker.record(ResourceType.CPU_TIME, duration);
 * 
 * // Get usage summary
 * TokenUsage usage = tracker.getUsage();
 * System.out.println("HTTP Requests: " + usage.get(ResourceType.HTTP_REQUEST));
 * }</pre>
 * 

 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see TokenTracker
 * @see TokenUsage
 */
public enum ResourceType {
    
    /**
     * Number of HTTP requests executed.
     * Unit: count
     */
    HTTP_REQUEST("http_requests", "count"),
    
    /**
     * Total bytes downloaded via HTTP.
     * Unit: bytes
     */
    HTTP_BYTES("http_bytes", "bytes"),
    
    /**
     * Total CPU time consumed during execution.
     * Unit: milliseconds
     */
    CPU_TIME("cpu_time_ms", "milliseconds"),
    
    /**
     * Peak memory usage during execution.
     * Unit: bytes
     */
    MEMORY_PEAK("memory_peak_bytes", "bytes");
    
    private final String metricName;
    private final String unit;
    
    ResourceType(String metricName, String unit) {
        this.metricName = metricName;
        this.unit = unit;
    }
    
    /**
     * Returns the metric name suitable for monitoring systems (e.g., Prometheus).
     * 
     * @return the metric name in snake_case format
     */
    public String getMetricName() {
        return metricName;
    }
    
    /**
     * Returns the unit of measurement for this resource type.
     * 
     * @return the unit (e.g., "count", "bytes", "milliseconds")
     */
    public String getUnit() {
        return unit;
    }
    
    /**
     * Returns a human-readable description of this resource type.
     * 
     * @return the description
     */
    public String getDescription() {
        switch (this) {
            case HTTP_REQUEST:
                return "Number of HTTP requests";
            case HTTP_BYTES:
                return "Bytes downloaded via HTTP";
            case CPU_TIME:
                return "CPU time consumed";
            case MEMORY_PEAK:
                return "Peak memory usage";
            default:
                return name();
        }
    }
}






