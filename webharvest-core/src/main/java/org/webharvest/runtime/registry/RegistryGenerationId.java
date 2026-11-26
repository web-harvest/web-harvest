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

package org.webharvest.runtime.registry;

import java.util.Objects;

/**
 * Stable identity of one immutable registry generation within one runtime.
 *
 * <p>The opaque value identifies one generation lifetime and must be unique
 * among generations that may coexist in the same runtime. It may contain a
 * snapshot digest, but a digest alone is insufficient if the same snapshot can
 * be published with a distinct live provider/loader lifecycle. It is deliberately
 * distinct from an execution generation, which identifies one session/task owner.</p>
 */
public final class RegistryGenerationId {

    private final String runtimeId;
    private final String value;

    public RegistryGenerationId(String runtimeId, String value) {
        if (runtimeId == null || runtimeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Runtime ID cannot be null or empty");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Registry generation value cannot be null or empty");
        }
        this.runtimeId = runtimeId;
        this.value = value;
    }

    public String getRuntimeId() {
        return runtimeId;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RegistryGenerationId)) {
            return false;
        }
        RegistryGenerationId that = (RegistryGenerationId) other;
        return runtimeId.equals(that.runtimeId) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runtimeId, value);
    }

    @Override
    public String toString() {
        return "RegistryGenerationId{runtimeId='" + runtimeId + "', value='" + value + "'}";
    }
}
