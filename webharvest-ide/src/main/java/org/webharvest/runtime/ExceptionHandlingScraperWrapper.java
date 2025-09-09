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

package org.webharvest.runtime;

import javax.inject.Named;

import org.webharvest.events.ScraperExecutionErrorEvent;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * {@link WebScraper} implementation which is a wrapper for default
 * {@link WebScraper} realization. It handles all {@link RuntimeException}s by
 * sending {@link ScraperExecutionErrorEvent} over {@link EventBus}.
 * 
 * It should be used only in Web-Harvest IDE.
 * 
 * @author mczapiewski
 * @since 2.1-SNAPSHOT
 * @version %I%, %G%
 */
public final class ExceptionHandlingScraperWrapper implements WebScraper {

	private final WebScraper webScraper;
	private final EventBus eventBus;

	/**
	 * Default class constructor which accepts references to default
	 * {@link WebScraper} instance and to {@link EventBus}.
	 * 
	 * @param webScraper
	 *            reference to default {@link WebScraper} instance; must be not
	 *            {@code null}.
	 * @param eventBus
	 *            reference to {@link EventBus}; must be not null.
	 */
	@Inject
	public ExceptionHandlingScraperWrapper(
			final @Named("default") WebScraper webScraper,
			final EventBus eventBus) {
		if (webScraper == null) {
			throw new IllegalArgumentException("WebScraper is mandatory.");
		}
		if (eventBus == null) {
			throw new IllegalArgumentException("EventBus is mandatory.");
		}
		this.webScraper = webScraper;
		this.eventBus = eventBus;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(final DynamicScopeContext context) {
		try {
			webScraper.execute(context);
		} catch (RuntimeException e) {
			eventBus.post(new ScraperExecutionErrorEvent(e));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void informListenersAboutError(final Exception e) {
		webScraper.informListenersAboutError(e);
	}

}
