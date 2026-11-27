/*
 * Copyright (c) 2006-2025 the original author or authors.
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
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISING OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.mail;

import org.webharvest.plugin.exceptions.PluginException;

/**
 * Exception for Mail plugin operations.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class MailPluginException extends PluginException {

    /**
     * Best-known disposition of the external SMTP side effect.
     *
     * <p>UNKNOWN means the client cannot establish whether the remote server
     * accepted the message. It must not be interpreted as a safe retry signal.</p>
     */
    public enum DeliveryDisposition {
        NOT_STARTED,
        IN_PROGRESS,
        CONFIRMED,
        FAILED,
        UNKNOWN
    }

    private final DeliveryDisposition deliveryDisposition;

    public MailPluginException(String message) {
        this(message, null, null);
    }

    public MailPluginException(Throwable cause) {
        this(cause == null ? null : cause.getMessage(), null, cause);
    }

    public MailPluginException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public MailPluginException(String message, DeliveryDisposition deliveryDisposition, Throwable cause) {
        super(message, cause);
        this.deliveryDisposition = deliveryDisposition;
    }

    /**
     * Returns the recorded SMTP effect disposition, or null for legacy
     * exceptions created without delivery-phase information.
     */
    public DeliveryDisposition getDeliveryDisposition() {
        return deliveryDisposition;
    }
}














