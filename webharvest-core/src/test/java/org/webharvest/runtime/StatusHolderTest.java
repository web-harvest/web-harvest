package org.webharvest.runtime;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class StatusHolderTest {

    @Test
    public void testStatusHolderInterface() {
        // Test that StatusHolder is an interface
        assertTrue(StatusHolder.class.isInterface());
    }

    @Test
    public void testGetStatusMethod() throws NoSuchMethodException {
        // Test that getStatus method exists and has correct signature
        assertNotNull(StatusHolder.class.getMethod("getStatus"));
        assertEquals(StatusHolder.class.getMethod("getStatus").getReturnType(), ScraperState.class);
        assertEquals(StatusHolder.class.getMethod("getStatus").getParameterCount(), 0);
    }

    @Test
    public void testStatusHolderImplementation() {
        // Create a simple implementation for testing
        StatusHolder statusHolder = new StatusHolder() {
            @Override
            public ScraperState getStatus() {
                return ScraperState.READY;
            }
        };

        assertNotNull(statusHolder);
        assertEquals(statusHolder.getStatus(), ScraperState.READY);
    }

    @Test
    public void testStatusHolderWithDifferentStates() {
        // Test implementation that returns different states
        StatusHolder readyHolder = new StatusHolder() {
            @Override
            public ScraperState getStatus() {
                return ScraperState.READY;
            }
        };

        StatusHolder runningHolder = new StatusHolder() {
            @Override
            public ScraperState getStatus() {
                return ScraperState.RUNNING;
            }
        };

        StatusHolder finishedHolder = new StatusHolder() {
            @Override
            public ScraperState getStatus() {
                return ScraperState.FINISHED;
            }
        };

        assertEquals(readyHolder.getStatus(), ScraperState.READY);
        assertEquals(runningHolder.getStatus(), ScraperState.RUNNING);
        assertEquals(finishedHolder.getStatus(), ScraperState.FINISHED);
    }

    @Test
    public void testStatusHolderWithErrorState() {
        StatusHolder errorHolder = new StatusHolder() {
            @Override
            public ScraperState getStatus() {
                return ScraperState.ERROR;
            }
        };

        assertEquals(errorHolder.getStatus(), ScraperState.ERROR);
    }

    @Test
    public void testStatusHolderWithDeprecatedState() {
        StatusHolder unknownHolder = new StatusHolder() {
            @Override
            public ScraperState getStatus() {
                return ScraperState.UNKNOWN;
            }
        };

        assertEquals(unknownHolder.getStatus(), ScraperState.UNKNOWN);
    }

    @Test
    public void testStatusHolderDynamicState() {
        // Test implementation that can change state
        final ScraperState[] currentState = {ScraperState.READY};
        
        StatusHolder dynamicHolder = new StatusHolder() {
            @Override
            public ScraperState getStatus() {
                return currentState[0];
            }
        };

        assertEquals(dynamicHolder.getStatus(), ScraperState.READY);
        
        // Change state
        currentState[0] = ScraperState.RUNNING;
        assertEquals(dynamicHolder.getStatus(), ScraperState.RUNNING);
        
        currentState[0] = ScraperState.FINISHED;
        assertEquals(dynamicHolder.getStatus(), ScraperState.FINISHED);
    }

    @Test
    public void testStatusHolderWithNullState() {
        // Test implementation that returns null (edge case)
        StatusHolder nullHolder = new StatusHolder() {
            @Override
            public ScraperState getStatus() {
                return null;
            }
        };

        assertNull(nullHolder.getStatus());
    }

    @Test
    public void testStatusHolderMethodCallMultipleTimes() {
        TestStatusHolder statusHolder = new TestStatusHolder();
        
        assertEquals(statusHolder.getStatus(), ScraperState.READY);
        assertEquals(statusHolder.getCallCount(), 1);
        
        assertEquals(statusHolder.getStatus(), ScraperState.READY);
        assertEquals(statusHolder.getCallCount(), 2);
    }

    // Helper class for testing
    private static class TestStatusHolder implements StatusHolder {
        private int callCount = 0;
        
        @Override
        public ScraperState getStatus() {
            callCount++;
            return ScraperState.READY;
        }
        
        public int getCallCount() {
            return callCount;
        }
    }
}
