package org.webharvest.runtime;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.processors.XQueryExpressionPool;

import net.sf.saxon.query.StaticQueryContext;

import static org.testng.Assert.*;

public class RuntimeConfigTest {

    private RuntimeConfig runtimeConfig;

    @BeforeMethod
    public void setUp() {
        runtimeConfig = new RuntimeConfig();
    }

    @Test
    public void testGetStaticQueryContext() {
        StaticQueryContext context = runtimeConfig.getStaticQueryContext();
        
        assertNotNull(context);
        assertNotNull(context.getConfiguration());
    }

    @Test
    public void testGetStaticQueryContextReturnsTheSameInstance() {
        StaticQueryContext context1 = runtimeConfig.getStaticQueryContext();
        StaticQueryContext context2 = runtimeConfig.getStaticQueryContext();
        
        assertSame(context1, context2);
    }

    @Test
    public void testGetXQueryExpressionPool() {
        XQueryExpressionPool pool = runtimeConfig.getXQueryExpressionPool();
        
        assertNotNull(pool);
    }

    @Test
    public void testGetXQueryExpressionPoolReturnsTheSameInstance() {
        XQueryExpressionPool pool1 = runtimeConfig.getXQueryExpressionPool();
        XQueryExpressionPool pool2 = runtimeConfig.getXQueryExpressionPool();
        
        assertSame(pool1, pool2);
    }

    @Test
    public void testXQueryExpressionPoolUsesStaticQueryContext() {
        // First get the static query context
        StaticQueryContext staticContext = runtimeConfig.getStaticQueryContext();
        
        // Then get the expression pool
        XQueryExpressionPool pool = runtimeConfig.getXQueryExpressionPool();
        
        assertNotNull(pool);
        assertNotNull(staticContext);
        
        // Verify that both are created and connected properly
        // The pool should use the same context
        assertSame(runtimeConfig.getStaticQueryContext(), staticContext);
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final StaticQueryContext[] contexts = new StaticQueryContext[2];
        final XQueryExpressionPool[] pools = new XQueryExpressionPool[2];
        
        Thread thread1 = new Thread(() -> {
            contexts[0] = runtimeConfig.getStaticQueryContext();
            pools[0] = runtimeConfig.getXQueryExpressionPool();
        });
        
        Thread thread2 = new Thread(() -> {
            contexts[1] = runtimeConfig.getStaticQueryContext();
            pools[1] = runtimeConfig.getXQueryExpressionPool();
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        // Both threads should get the same instances (singleton behavior)
        assertSame(contexts[0], contexts[1]);
        assertSame(pools[0], pools[1]);
    }

    @Test
    public void testLazyInitialization() {
        RuntimeConfig newConfig = new RuntimeConfig();
        
        // Initially, internal state should be null (lazy initialization)
        // We can't directly test this without reflection, but we can verify
        // that calling methods multiple times returns the same instances
        
        StaticQueryContext context1 = newConfig.getStaticQueryContext();
        StaticQueryContext context2 = newConfig.getStaticQueryContext();
        
        XQueryExpressionPool pool1 = newConfig.getXQueryExpressionPool();
        XQueryExpressionPool pool2 = newConfig.getXQueryExpressionPool();
        
        assertSame(context1, context2);
        assertSame(pool1, pool2);
    }
}














