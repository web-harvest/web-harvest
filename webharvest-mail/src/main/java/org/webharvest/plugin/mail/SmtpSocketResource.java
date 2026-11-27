/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.plugin.mail;

import java.io.IOException;
import java.util.UUID;

import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.resource.ExecutionResourceAccess;

/**
 * Binds one SMTP socket factory to the lifetime of a single mail send.
 */
final class SmtpSocketResource implements AutoCloseable {

    static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    static final int READ_TIMEOUT_MILLIS = 30_000;
    static final int WRITE_TIMEOUT_MILLIS = 30_000;

    private final TrackingSmtpSocketFactory socketFactory;
    private final ExecutionResourceAccess.OperationResource operation;
    private final ExecutionResourceAccess.Lease lease;

    private SmtpSocketResource(TrackingSmtpSocketFactory socketFactory,
                               ExecutionResourceAccess.OperationResource operation,
                               ExecutionResourceAccess.Lease lease) {
        this.socketFactory = socketFactory;
        this.operation = operation;
        this.lease = lease;
    }

    static SmtpSocketResource open(DynamicScopeContext context,
                                  TrackingSmtpSocketFactory socketFactory) {
        ExecutionResourceAccess access = context == null ? null : context.getResourceAccess();
        if (access == null) {
            return new SmtpSocketResource(socketFactory, null, null);
        }

        ExecutionResourceAccess.OperationResource operation;
        try {
            operation = access.registerOperationResource(
                "smtp/send/" + UUID.randomUUID(),
                socketFactory::closeAll,
                socketFactory::closeAll);
        } catch (RuntimeException | Error failure) {
            closeAfterAdmissionFailure(socketFactory, failure);
            throw failure;
        }

        ExecutionResourceAccess.Lease lease;
        try {
            lease = operation.acquire();
        } catch (RuntimeException | Error failure) {
            operation.close();
            throw failure;
        }
        return new SmtpSocketResource(socketFactory, operation, lease);
    }

    @Override
    public void close() throws IOException {
        if (lease != null) {
            lease.close();
        }
        if (operation != null) {
            operation.close();
            return;
        }
        socketFactory.closeAll();
    }

    private static void closeAfterAdmissionFailure(TrackingSmtpSocketFactory socketFactory,
                                                    Throwable failure) {
        try {
            socketFactory.closeAll();
        } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
