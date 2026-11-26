package org.webharvest.runtime.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RuntimeResourceManagerTest {

    private static <T extends Throwable> T assertThrows(Class<T> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return expected.cast(actual);
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
    public void concurrentCloseWaitsUntilTheFirstRetirementCompletes() throws Exception {
        RuntimeResourceManager manager = new RuntimeResourceManager();
        CountDownLatch closeStarted = new CountDownLatch(1);
        CountDownLatch releaseClose = new CountDownLatch(1);
        manager.register("blocking", () -> {
            closeStarted.countDown();
            if (!releaseClose.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test release");
            }
        });

        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = callers.submit(manager::close);
            assertTrue(closeStarted.await(2, TimeUnit.SECONDS));
            Future<?> second = callers.submit(manager::close);

            try {
                second.get(100, TimeUnit.MILLISECONDS);
                throw new AssertionError("Concurrent close returned before resource retirement completed");
            } catch (TimeoutException expected) {
                // The second caller must wait for the first caller's close actions.
            }

            releaseClose.countDown();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
            assertTrue(manager.isClosed());
            assertEquals(manager.getRegisteredCount(), 0);
        } finally {
            releaseClose.countDown();
            callers.shutdownNow();
        }
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
