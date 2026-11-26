/*
 * Copyright (c) 2006-2026 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.runtime.resource;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Owns runtime-lifetime closeable resources shared by accepted executions.
 *
 * <p>The owning {@code WebHarvestService} closes this registry only after all
 * accepted workers, execution-resource drains and asynchronous abort callbacks
 * have quiesced. Resources are closed in reverse registration order.</p>
 */
public final class RuntimeResourceManager implements AutoCloseable {

    private final Map<String, AutoCloseable> resources = new LinkedHashMap<>();
    private final IdentityHashMap<AutoCloseable, String> resourceIds = new IdentityHashMap<>();
    private boolean closed;
    private RuntimeException closeFailure;

    /**
     * Registers one runtime-owned resource.
     *
     * @param resourceId stable non-empty runtime-local identity
     * @param resource resource to close during runtime retirement
     */
    public void register(String resourceId, AutoCloseable resource) {
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Runtime resource ID cannot be null or empty");
        }
        Objects.requireNonNull(resource, "Runtime resource cannot be null");

        boolean closeImmediately;
        synchronized (this) {
            closeImmediately = closed;
            if (!closeImmediately) {
                if (resources.containsKey(resourceId)) {
                    throw new IllegalStateException("Runtime resource ID is already registered: " + resourceId);
                }
                if (resourceIds.containsKey(resource)) {
                    throw new IllegalStateException("Runtime resource instance is already registered as: "
                        + resourceIds.get(resource));
                }
                resources.put(resourceId, resource);
                resourceIds.put(resource, resourceId);
            }
        }

        if (closeImmediately) {
            RuntimeException failure = closeOne(resourceId, resource);
            IllegalStateException rejected = new IllegalStateException(
                "Runtime resource manager is already closed: " + resourceId);
            if (failure != null) {
                rejected.addSuppressed(failure);
            }
            throw rejected;
        }
    }

    /** Returns the number of resources still owned by this manager. */
    public synchronized int getRegisteredCount() {
        return resources.size();
    }

    /** Returns whether runtime retirement has started. */
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Closes every registered resource once, in reverse registration order.
     * All close actions are attempted even when an earlier resource fails.
     */
    @Override
    public void close() {
        List<Map.Entry<String, AutoCloseable>> snapshot;
        synchronized (this) {
            if (closed) {
                if (closeFailure != null) {
                    throw closeFailure;
                }
                return;
            }
            closed = true;
            snapshot = new ArrayList<>(resources.entrySet());
            resources.clear();
            resourceIds.clear();
        }

        RuntimeException failure = null;
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Map.Entry<String, AutoCloseable> entry = snapshot.get(i);
            RuntimeException closeError = closeOne(entry.getKey(), entry.getValue());
            if (closeError != null) {
                if (failure == null) {
                    failure = new IllegalStateException("One or more runtime resources failed to close");
                }
                failure.addSuppressed(closeError);
            }
        }

        synchronized (this) {
            closeFailure = failure;
        }
        if (failure != null) {
            throw failure;
        }
    }

    private RuntimeException closeOne(String resourceId, AutoCloseable resource) {
        try {
            resource.close();
            return null;
        } catch (Throwable failure) {
            return new IllegalStateException("Failed to close runtime resource: " + resourceId, failure);
        }
    }
}
