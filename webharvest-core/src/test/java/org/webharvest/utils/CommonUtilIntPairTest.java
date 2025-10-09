package org.webharvest.utils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Test for CommonUtil.IntPair.
 */
public class CommonUtilIntPairTest {

    private CommonUtil.IntPair intPair;

    @BeforeMethod
    public void setUp() {
        intPair = new CommonUtil.IntPair();
    }

    @Test
    public void testDefaultConstructor() {
        CommonUtil.IntPair pair = new CommonUtil.IntPair();
        assertEquals(pair.x, 0);
        assertEquals(pair.y, 0);
    }

    @Test
    public void testConstructorWithValues() {
        CommonUtil.IntPair pair = new CommonUtil.IntPair(5, 10);
        assertEquals(pair.x, 5);
        assertEquals(pair.y, 10);
    }

    @Test
    public void testToString() {
        intPair.x = 3;
        intPair.y = 7;
        String result = intPair.toString();
        assertEquals(result, "x = 3, y = 7");
    }

    @Test
    public void testToStringWithZeroValues() {
        intPair.x = 0;
        intPair.y = 0;
        String result = intPair.toString();
        assertEquals(result, "x = 0, y = 0");
    }

    @Test
    public void testToStringWithNegativeValues() {
        intPair.x = -5;
        intPair.y = -10;
        String result = intPair.toString();
        assertEquals(result, "x = -5, y = -10");
    }

    @Test
    public void testDefineFromStringSingleValue() {
        intPair.defineFromString("5", ':', 100);
        assertEquals(intPair.x, 5);
        assertEquals(intPair.y, 5);
    }

    @Test
    public void testDefineFromStringWithSeparator() {
        intPair.defineFromString("3:7", ':', 100);
        assertEquals(intPair.x, 3);
        assertEquals(intPair.y, 7);
    }

    @Test
    public void testDefineFromStringStartingWithSeparator() {
        intPair.defineFromString(":7", ':', 100);
        assertEquals(intPair.x, 1);
        assertEquals(intPair.y, 7);
    }

    @Test
    public void testDefineFromStringEndingWithSeparator() {
        intPair.defineFromString("3:", ':', 100);
        assertEquals(intPair.x, 3);
        assertEquals(intPair.y, 100);
    }

    @Test
    public void testDefineFromStringWithEmptyFirstPart() {
        intPair.defineFromString(":7", ':', 100);
        assertEquals(intPair.x, 1);
        assertEquals(intPair.y, 7);
    }

    @Test
    public void testDefineFromStringWithEmptySecondPart() {
        intPair.defineFromString("3:", ':', 100);
        assertEquals(intPair.x, 3);
        assertEquals(intPair.y, 100);
    }

    @Test
    public void testDefineFromStringWithEmptySecondPartAfterSeparator() {
        intPair.defineFromString("3:", ':', 100);
        assertEquals(intPair.x, 3);
        assertEquals(intPair.y, 100);
    }

    @Test
    public void testDefineFromStringWithEmptyFirstPartBeforeSeparator() {
        intPair.defineFromString(":7", ':', 100);
        assertEquals(intPair.x, 1);
        assertEquals(intPair.y, 7);
    }

    @Test
    public void testDefineFromStringWithDifferentSeparator() {
        intPair.defineFromString("5-10", '-', 100);
        assertEquals(intPair.x, 5);
        assertEquals(intPair.y, 10);
    }

    @Test
    public void testDefineFromStringWithCommaSeparator() {
        intPair.defineFromString("2,8", ',', 100);
        assertEquals(intPair.x, 2);
        assertEquals(intPair.y, 8);
    }

    @Test
    public void testDefineFromStringWithMaxValue() {
        intPair.defineFromString(":", ':', 50);
        assertEquals(intPair.x, 1);
        assertEquals(intPair.y, 50);
    }

    @Test
    public void testDefineFromStringWithMaxValueAtEnd() {
        intPair.defineFromString("5:", ':', 50);
        assertEquals(intPair.x, 5);
        assertEquals(intPair.y, 50);
    }

    @Test
    public void testDefineFromStringWithMaxValueAtStart() {
        intPair.defineFromString(":5", ':', 50);
        assertEquals(intPair.x, 1);
        assertEquals(intPair.y, 5);
    }

    @Test
    public void testDefineFromStringWithZeroValues() {
        intPair.defineFromString("0:0", ':', 100);
        assertEquals(intPair.x, 0);
        assertEquals(intPair.y, 0);
    }

    @Test
    public void testDefineFromStringWithNegativeValues() {
        intPair.defineFromString("-5:-10", ':', 100);
        assertEquals(intPair.x, -5);
        assertEquals(intPair.y, -10);
    }

    @Test
    public void testDefineFromStringWithLargeValues() {
        intPair.defineFromString("999:1000", ':', 1000);
        assertEquals(intPair.x, 999);
        assertEquals(intPair.y, 1000);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testDefineFromStringWithInvalidNumber() {
        intPair.defineFromString("abc:def", ':', 100);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testDefineFromStringWithInvalidFirstNumber() {
        intPair.defineFromString("abc:5", ':', 100);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testDefineFromStringWithInvalidSecondNumber() {
        intPair.defineFromString("5:abc", ':', 100);
    }
}



















