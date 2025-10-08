/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.utils;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Extended tests for Assert utility
 */
public class AssertExtendedTest {

    @Test
    public void testNotNullWithValidObject() {
        String obj = "test";
        org.webharvest.utils.Assert.notNull(obj, "Object is null");
        // Should not throw exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNotNullWithNull() {
        org.webharvest.utils.Assert.notNull(null, "Object is null");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNotNullWithNullAndMessage() {
        org.webharvest.utils.Assert.notNull(null, "Custom message: {0}", "param");
    }

    @Test
    public void testIsTrueWithTrue() {
        org.webharvest.utils.Assert.isTrue(true, "Should be true");
        // Should not throw exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIsTrueWithFalse() {
        org.webharvest.utils.Assert.isTrue(false, "Value is false");
    }

    @Test
    public void testIsFalseWithFalse() {
        org.webharvest.utils.Assert.isFalse(false, "Should be false");
        // Should not throw exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIsFalseWithTrue() {
        org.webharvest.utils.Assert.isFalse(true, "Value is true");
    }
}

