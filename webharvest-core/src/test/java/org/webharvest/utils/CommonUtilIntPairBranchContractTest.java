package org.webharvest.utils;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.utils.CommonUtil.IntPair;

/**
 * Contracts for {@link CommonUtil.IntPair#defineFromString(String, char, int)}.
 */
public class CommonUtilIntPairBranchContractTest {

    @Test
    public void defineFromStringParsesPairAndSingleValue() {
        IntPair pair = new IntPair();
        pair.defineFromString("3:8", ':', 10);
        assertEquals(pair.x, 3);
        assertEquals(pair.y, 8);

        pair.defineFromString("4", ':', 20);
        assertEquals(pair.x, 4);
        assertEquals(pair.y, 4);
    }

    @Test
    public void defineFromStringHandlesOpenEndedRanges() {
        IntPair pair = new IntPair();
        pair.defineFromString(":7", ':', 20);
        assertEquals(pair.x, 1);
        assertEquals(pair.y, 7);

        pair.defineFromString("5:", ':', 20);
        assertEquals(pair.x, 5);
        assertEquals(pair.y, 20);

        pair.defineFromString(":", ':', 9);
        assertEquals(pair.x, 1);
        assertEquals(pair.y, 9);
    }
}
