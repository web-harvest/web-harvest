package org.webharvest.runtime.session;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link SessionStatus} enum.
 * 
 * <p>Tests all utility methods that determine session state characteristics.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class SessionStatusTest {
    
    @Test(groups = {"unit", "session"})
    public void testIsTerminal_CompletedStatus() {
        assertTrue(SessionStatus.COMPLETED.isTerminal(),
            "COMPLETED should be terminal");
    }
    
    @Test(groups = {"unit", "session"})
    public void testIsTerminal_FailedStatus() {
        assertTrue(SessionStatus.FAILED.isTerminal(),
            "FAILED should be terminal");
    }
    
    @Test(groups = {"unit", "session"})
    public void testIsTerminal_CancelledStatus() {
        assertTrue(SessionStatus.CANCELLED.isTerminal(),
            "CANCELLED should be terminal");
    }
    
    @Test(groups = {"unit", "session"})
    public void testIsTerminal_PendingStatus() {
        assertFalse(SessionStatus.PENDING.isTerminal(),
            "PENDING should not be terminal");
    }
    
    @Test(groups = {"unit", "session"})
    public void testIsTerminal_RunningStatus() {
        assertFalse(SessionStatus.RUNNING.isTerminal(),
            "RUNNING should not be terminal");
    }
    
    @Test(groups = {"unit", "session"})
    public void testIsSuccessful_OnlyCompletedIsSuccessful() {
        assertTrue(SessionStatus.COMPLETED.isSuccessful());
        assertFalse(SessionStatus.PENDING.isSuccessful());
        assertFalse(SessionStatus.RUNNING.isSuccessful());
        assertFalse(SessionStatus.FAILED.isSuccessful());
        assertFalse(SessionStatus.CANCELLED.isSuccessful());
    }
    
    @Test(groups = {"unit", "session"})
    public void testIsActive_PendingAndRunningAreActive() {
        assertTrue(SessionStatus.PENDING.isActive(),
            "PENDING should be active");
        assertTrue(SessionStatus.RUNNING.isActive(),
            "RUNNING should be active");
    }
    
    @Test(groups = {"unit", "session"})
    public void testIsActive_TerminalStatesNotActive() {
        assertFalse(SessionStatus.COMPLETED.isActive(),
            "COMPLETED should not be active");
        assertFalse(SessionStatus.FAILED.isActive(),
            "FAILED should not be active");
        assertFalse(SessionStatus.CANCELLED.isActive(),
            "CANCELLED should not be active");
    }
    
    @Test(groups = {"unit", "session"})
    public void testAllStatusValues() {
        // Verify all expected enum values exist
        SessionStatus[] values = SessionStatus.values();
        assertEquals(values.length, 5,
            "Should have exactly 5 status values");
        
        // Verify specific values
        assertNotNull(SessionStatus.valueOf("PENDING"));
        assertNotNull(SessionStatus.valueOf("RUNNING"));
        assertNotNull(SessionStatus.valueOf("COMPLETED"));
        assertNotNull(SessionStatus.valueOf("FAILED"));
        assertNotNull(SessionStatus.valueOf("CANCELLED"));
    }
}

