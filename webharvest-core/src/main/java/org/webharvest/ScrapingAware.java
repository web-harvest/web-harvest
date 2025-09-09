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

package org.webharvest;

/**
 * Interface to be implemented by any object that wishes to be notified of
 * scraping scope possibly it runs in.
 * Currently it is the only solution to receive notification about scope's
 * state changes.
 *
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 * @version %I%, %G%
 */
// FIXME rbala What will happen to an instance created in a scope? How can we
// unregister it?
public interface ScrapingAware {

    /**
     * Receive notification about new scope initialized for particular
     * {@link Harvester} object. This notification is sent just after the scope
     * has been created, but usually before any bean is created as a part of it.
     *
     * @param harvester
     *            the reference to {@link Harvester} for which the scope has
     *            been created.
     */
    void onBeforeScraping(Harvester harvester);

    /**
     * Receive notification about the scope for particular {@link Harvester}
     * that is about to be tore down. This notification is sent just before
     * destroying the scope and its all associated beans.
     *
     * @param harvester
     *            the reference to {@link Harvester} for which the scope is
     *            about to be tore down.
     */
    void onAfterScraping(Harvester harvester);

}
