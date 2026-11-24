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
   following disclaimer, are included in all copies.
 */

package org.webharvest.runtime;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;
import org.webharvest.runtime.session.ExecutionContextCarrier;
import org.webharvest.runtime.session.ExecutionIdentity;
import org.webharvest.runtime.session.ScraperSessionImpl;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.KeyValuePair;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.EventBus;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

import org.webharvest.definition.Config;

public class ScraperContextOwnershipTest {

    @Test
    public void mutableContextIsConfinedToTheBoundWorkerThread() throws InterruptedException {
        ExecutionIdentity identity = new ExecutionIdentity("runtime-a", "session-a", 1);
        EventBus eventBus = new EventBus();
        ScraperSessionImpl session = new ScraperSessionImpl(identity, null, mock(Config.class), eventBus);
        ScraperContext context = new ScraperContext();

        try (ExecutionContextCarrier.ScopeToken ignored = ExecutionContextCarrier.install(identity)) {
            context.setEventBus(eventBus);
            context.setSession(session);
            context.setLocalVar("root", "value");
            assertEquals(context.getVar("root").toString(), "value");
            assertEquals(context.getExecutionIdentity(), identity);
        }

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread otherThread = new Thread(() -> {
            try {
                context.setLocalVar("other", "forbidden");
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        otherThread.start();
        otherThread.join(5000);

        assertFalse(otherThread.isAlive());
        assertTrue(failure.get() instanceof IllegalStateException);
        assertNull(context.getVar("other"));
    }

    @Test
    public void contextRejectsASecondSessionAndASecondEventBus() {
        ExecutionIdentity firstIdentity = new ExecutionIdentity("runtime-a", "session-a", 1);
        ExecutionIdentity secondIdentity = new ExecutionIdentity("runtime-a", "session-a", 2);
        EventBus firstBus = new EventBus();
        EventBus secondBus = new EventBus();
        ScraperSessionImpl firstSession =
            new ScraperSessionImpl(firstIdentity, null, mock(Config.class), firstBus);
        ScraperSessionImpl secondSession =
            new ScraperSessionImpl(secondIdentity, null, mock(Config.class), firstBus);
        ScraperContext context = new ScraperContext();

        try (ExecutionContextCarrier.ScopeToken ignored = ExecutionContextCarrier.install(firstIdentity)) {
            context.setEventBus(firstBus);
            context.setSession(firstSession);

            try {
                context.setSession(secondSession);
                fail("A context cannot be rebound to another execution generation");
            } catch (IllegalStateException expected) {
                assertEquals(context.getExecutionIdentity(), firstIdentity);
            }

            try {
                context.setEventBus(secondBus);
                fail("A context cannot be rebound to another EventBus");
            } catch (IllegalStateException expected) {
                assertSame(context.getEventBus(), firstBus);
            }
        }
    }

    @Test
    public void contextRejectsAThreadScopeWithAnotherExecutionIdentity() {
        ExecutionIdentity owner = new ExecutionIdentity("runtime-a", "session-a", 1);
        ExecutionIdentity other = new ExecutionIdentity("runtime-b", "session-b", 2);
        EventBus eventBus = new EventBus();
        ScraperSessionImpl session = new ScraperSessionImpl(owner, null, mock(Config.class), eventBus);
        ScraperContext context = new ScraperContext();

        try (ExecutionContextCarrier.ScopeToken ignored = ExecutionContextCarrier.install(owner)) {
            context.setEventBus(eventBus);
            context.setSession(session);
        }

        try (ExecutionContextCarrier.ScopeToken ignored = ExecutionContextCarrier.install(other)) {
            try {
                context.getVar("root");
                fail("A context cannot be accessed under another runtime/session identity");
            } catch (IllegalStateException expected) {
                assertEquals(context.getExecutionIdentity(), owner);
            }
        }
    }

    @Test
    public void iteratorReturnsASnapshotNotTheLiveMutableTable() {
        ScraperContext context = new ScraperContext();
        context.setLocalVar("first", "one");
        Iterator<KeyValuePair<Variable>> snapshot = context.iterator();
        context.setLocalVar("second", "two");

        assertTrue(snapshot.hasNext());
        assertEquals(snapshot.next().getKey(), "first");
        assertFalse(snapshot.hasNext());
    }
}
