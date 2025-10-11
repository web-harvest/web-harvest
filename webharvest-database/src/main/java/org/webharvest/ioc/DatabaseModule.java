/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.ioc;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import org.webharvest.runtime.database.ConnectionFactory;
import org.webharvest.runtime.database.JNDIConnectionFactory;
import org.webharvest.runtime.database.StandaloneConnectionPool;

/**
 * Guice module for database-specific bindings.
 * This module should be used together with ScraperModule from webharvest-core.
 * 
 * @author WebHarvest Team
 * @since 2.2.0
 */
public class DatabaseModule extends AbstractModule {

    @Override
    protected void configure() {
        // Database connection factory bindings
        bind(ConnectionFactory.class).to(StandaloneConnectionPool.class).in(ScrapingScope.class);
        
        bind(ConnectionFactory.class).annotatedWith(Names.named("standalone"))
           .to(StandaloneConnectionPool.class).in(ScrapingScope.class);
           
        bind(ConnectionFactory.class).annotatedWith(Names.named("jndi"))
           .to(JNDIConnectionFactory.class).in(Singleton.class);
    }
}

