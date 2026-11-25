/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * Tracks TestNG invocation progress and emits a thread dump only if the fork stalls.
 * The low-volume watchdog avoids flooding CI logs while preserving the last
 * entered invocation and all JVM thread stacks for diagnosis.
 */
public final class CoreTestInvocationTraceListener implements IInvokedMethodListener {

    private static final long STALL_THRESHOLD_NANOS = TimeUnit.SECONDS.toNanos(90);
    private static final AtomicLong LAST_ACTIVITY_NANOS = new AtomicLong(System.nanoTime());
    private static final AtomicReference<String> LAST_INVOCATION =
        new AtomicReference<String>("TestNG invocation has not started");
    private static final AtomicBoolean STALL_REPORTED = new AtomicBoolean();

    private static final ScheduledExecutorService WATCHDOG =
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "core-test-invocation-watchdog");
            thread.setDaemon(true);
            return thread;
        });

    static {
        WATCHDOG.scheduleAtFixedRate(
            CoreTestInvocationTraceListener::reportIfStalled, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        markActivity("START " + describe(method, testResult));
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        markActivity("END " + describe(method, testResult) + " status=" + testResult.getStatus());
    }

    private static void markActivity(String invocation) {
        LAST_INVOCATION.set(invocation);
        LAST_ACTIVITY_NANOS.set(System.nanoTime());
        STALL_REPORTED.set(false);
    }

    private static String describe(IInvokedMethod method, ITestResult result) {
        String kind = method.isTestMethod() ? "TEST" : "CONFIG";
        String className = result.getTestClass() == null
            ? "<unknown>" : result.getTestClass().getName();
        String methodName = method.getTestMethod() == null
            ? "<unknown>" : method.getTestMethod().getMethodName();
        return kind + " " + className + "#" + methodName;
    }

    private static void reportIfStalled() {
        long idleNanos = System.nanoTime() - LAST_ACTIVITY_NANOS.get();
        if (idleNanos < STALL_THRESHOLD_NANOS || !STALL_REPORTED.compareAndSet(false, true)) {
            return;
        }

        System.err.println("[WH-TEST-STALL] No TestNG invocation callback for "
            + TimeUnit.NANOSECONDS.toSeconds(idleNanos) + " seconds; last invocation: "
            + LAST_INVOCATION.get());
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadBean.dumpAllThreads(true, true);
            for (ThreadInfo threadInfo : threadInfos) {
                if (threadInfo != null) {
                    System.err.println(threadInfo);
                }
            }
        } catch (Throwable dumpFailure) {
            System.err.println("[WH-TEST-STALL] Thread dump failed");
            dumpFailure.printStackTrace(System.err);
        }
        System.err.flush();
    }
}
