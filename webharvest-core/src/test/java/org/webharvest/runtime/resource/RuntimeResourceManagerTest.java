package org.webharvest.runtime.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RuntimeResourceManagerTest {

    private static void assertThrows(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError("Unexpected exception type: " + actual.getClass().getName(), actual);
        }
        throw new AssertionError("Expected exception type: " + expected.getName());
    }

    @Test
    public void closesResourcesInReverseRegistrationOrderExactlyOnce() {
        RuntimeResourceManager manager = new RuntimeResourceManager();
        List<String> closed = new ArrayList<>();
        manager.register("first", () -> closed.add("first"));
        manager.register("second", () -> closed.add("second"));

        manager.close();
        manager.close();

        assertEquals(closed, Arrays.asList("second", "first"));
        assertEquals(manager.getRegisteredCount(), 0);
        assertTrue(manager.isClosed());
    }

    @Test
    public void rejectsDuplicateIdsAndDuplicateResourceInstances() {
        RuntimeResourceManager manager = new RuntimeResourceManager();
        AutoCloseable first = () -> { };
        manager.register("first", first);

        assertThrows(IllegalStateException.class, () -> manager.register("first", () -> { }));
        assertThrows(IllegalStateException.class, () -> manager.register("second", first));
        assertEquals(manager.getRegisteredCount(), 1);
        manager.close();
    }

    @Test
    public void closesLateResourceImmediatelyAndRejectsRegistration() {
        RuntimeResourceManager manager = new RuntimeResourceManager();
        manager.close();
        AtomicBoolean closed = new AtomicBoolean();

        assertThrows(IllegalStateException.class,
            () -> manager.register("late", () -> closed.set(true)));

        assertTrue(closed.get());
        assertTrue(manager.isClosed());
        assertEquals(manager.getRegisteredCount(), 0);
    }

    @Test
    public void attemptsEveryCloseAndRetainsAggregateFailure() {
        RuntimeResourceManager manager = new RuntimeResourceManager();
        AtomicBoolean secondClosed = new AtomicBoolean();
        manager.register("first", () -> {
            throw new IllegalStateException("first close failed");
        });
        manager.register("second", () -> secondClosed.set(true));

        RuntimeException failure = assertThrows(IllegalStateException.class, manager::close);
        assertTrue(secondClosed.get());
        assertEquals(failure.getSuppressed().length, 1);
        RuntimeException repeated = assertThrows(IllegalStateException.class, manager::close);
        assertSame(repeated, failure);
    }
}
