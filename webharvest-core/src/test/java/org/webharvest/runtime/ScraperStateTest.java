package org.webharvest.runtime;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ScraperStateTest {

    @Test
    public void testEnumValues() {
        ScraperState[] values = ScraperState.values();
        
        assertNotNull(values);
        assertEquals(values.length, 8);
        
        // Check that all expected values are present
        assertTrue(containsValue(values, ScraperState.READY));
        assertTrue(containsValue(values, ScraperState.RUNNING));
        assertTrue(containsValue(values, ScraperState.PAUSED));
        assertTrue(containsValue(values, ScraperState.FINISHED));
        assertTrue(containsValue(values, ScraperState.STOPPED));
        assertTrue(containsValue(values, ScraperState.ERROR));
        assertTrue(containsValue(values, ScraperState.EXIT));
        assertTrue(containsValue(values, ScraperState.UNKNOWN));
    }

    @Test
    public void testValueOf() {
        assertEquals(ScraperState.valueOf("READY"), ScraperState.READY);
        assertEquals(ScraperState.valueOf("RUNNING"), ScraperState.RUNNING);
        assertEquals(ScraperState.valueOf("PAUSED"), ScraperState.PAUSED);
        assertEquals(ScraperState.valueOf("FINISHED"), ScraperState.FINISHED);
        assertEquals(ScraperState.valueOf("STOPPED"), ScraperState.STOPPED);
        assertEquals(ScraperState.valueOf("ERROR"), ScraperState.ERROR);
        assertEquals(ScraperState.valueOf("EXIT"), ScraperState.EXIT);
        assertEquals(ScraperState.valueOf("UNKNOWN"), ScraperState.UNKNOWN);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValueOfWithInvalidName() {
        ScraperState.valueOf("INVALID_STATE");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testValueOfWithNull() {
        ScraperState.valueOf(null);
    }

    @Test
    public void testOrdinal() {
        assertEquals(ScraperState.READY.ordinal(), 0);
        assertEquals(ScraperState.RUNNING.ordinal(), 1);
        assertEquals(ScraperState.PAUSED.ordinal(), 2);
        assertEquals(ScraperState.FINISHED.ordinal(), 3);
        assertEquals(ScraperState.STOPPED.ordinal(), 4);
        assertEquals(ScraperState.ERROR.ordinal(), 5);
        assertEquals(ScraperState.EXIT.ordinal(), 6);
        assertEquals(ScraperState.UNKNOWN.ordinal(), 7);
    }

    @Test
    public void testName() {
        assertEquals(ScraperState.READY.name(), "READY");
        assertEquals(ScraperState.RUNNING.name(), "RUNNING");
        assertEquals(ScraperState.PAUSED.name(), "PAUSED");
        assertEquals(ScraperState.FINISHED.name(), "FINISHED");
        assertEquals(ScraperState.STOPPED.name(), "STOPPED");
        assertEquals(ScraperState.ERROR.name(), "ERROR");
        assertEquals(ScraperState.EXIT.name(), "EXIT");
        assertEquals(ScraperState.UNKNOWN.name(), "UNKNOWN");
    }

    @Test
    public void testToString() {
        assertEquals(ScraperState.READY.toString(), "READY");
        assertEquals(ScraperState.RUNNING.toString(), "RUNNING");
        assertEquals(ScraperState.PAUSED.toString(), "PAUSED");
        assertEquals(ScraperState.FINISHED.toString(), "FINISHED");
        assertEquals(ScraperState.STOPPED.toString(), "STOPPED");
        assertEquals(ScraperState.ERROR.toString(), "ERROR");
        assertEquals(ScraperState.EXIT.toString(), "EXIT");
        assertEquals(ScraperState.UNKNOWN.toString(), "UNKNOWN");
    }

    @Test
    public void testEquals() {
        assertEquals(ScraperState.READY, ScraperState.READY);
        assertEquals(ScraperState.RUNNING, ScraperState.RUNNING);
        assertFalse(ScraperState.READY.equals(ScraperState.RUNNING));
        assertFalse(ScraperState.FINISHED.equals(ScraperState.ERROR));
    }

    @Test
    public void testHashCode() {
        assertEquals(ScraperState.READY.hashCode(), ScraperState.READY.hashCode());
        assertEquals(ScraperState.RUNNING.hashCode(), ScraperState.RUNNING.hashCode());
        assertFalse(ScraperState.READY.hashCode() == ScraperState.RUNNING.hashCode());
    }

    @Test
    public void testCompareTo() {
        assertTrue(ScraperState.READY.compareTo(ScraperState.RUNNING) < 0);
        assertTrue(ScraperState.RUNNING.compareTo(ScraperState.READY) > 0);
        assertEquals(ScraperState.READY.compareTo(ScraperState.READY), 0);
        assertTrue(ScraperState.FINISHED.compareTo(ScraperState.ERROR) < 0);
    }

    @Test
    public void testGetDeclaringClass() {
        assertEquals(ScraperState.READY.getDeclaringClass(), ScraperState.class);
        assertEquals(ScraperState.RUNNING.getDeclaringClass(), ScraperState.class);
        assertEquals(ScraperState.UNKNOWN.getDeclaringClass(), ScraperState.class);
    }

    @Test
    public void testDeprecatedAnnotation() throws NoSuchFieldException {
        // Check that UNKNOWN is marked as deprecated
        assertTrue(ScraperState.UNKNOWN.getClass().getField("UNKNOWN").isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void testEnumSwitchStatement() {
        String result = getStateDescription(ScraperState.READY);
        assertEquals(result, "Ready to execute");
        
        result = getStateDescription(ScraperState.RUNNING);
        assertEquals(result, "Currently running");
        
        result = getStateDescription(ScraperState.FINISHED);
        assertEquals(result, "Successfully finished");
        
        result = getStateDescription(ScraperState.ERROR);
        assertEquals(result, "Finished with error");
        
        result = getStateDescription(ScraperState.UNKNOWN);
        assertEquals(result, "Unknown state");
    }

    private boolean containsValue(ScraperState[] values, ScraperState target) {
        for (ScraperState state : values) {
            if (state == target) {
                return true;
            }
        }
        return false;
    }

    private String getStateDescription(ScraperState state) {
        switch (state) {
            case READY:
                return "Ready to execute";
            case RUNNING:
                return "Currently running";
            case PAUSED:
                return "Paused";
            case FINISHED:
                return "Successfully finished";
            case STOPPED:
                return "Stopped by user";
            case ERROR:
                return "Finished with error";
            case EXIT:
                return "Stopped by exit command";
            case UNKNOWN:
            default:
                return "Unknown state";
        }
    }
}
