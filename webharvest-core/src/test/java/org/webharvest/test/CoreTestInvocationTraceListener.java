/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.test;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * Emits flushed start/end markers for TestNG test and configuration invocations.
 * This diagnostic listener makes a stalled invocation visible in CI logs.
 */
public final class CoreTestInvocationTraceListener implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        trace("START", method, testResult);
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        trace("END", method, testResult);
    }

    private void trace(String phase, IInvokedMethod method, ITestResult result) {
        String kind = method.isTestMethod() ? "TEST" : "CONFIG";
        String qualifiedName = method.getTestMethod().getQualifiedName();
        System.err.println("[WH-TEST-TRACE] " + phase + " " + kind + " " + qualifiedName
            + " status=" + result.getStatus()
            + " thread=" + Thread.currentThread().getName());
        System.err.flush();
    }
}
