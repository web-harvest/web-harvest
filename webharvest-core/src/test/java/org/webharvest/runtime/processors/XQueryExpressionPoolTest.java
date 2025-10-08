package org.webharvest.runtime.processors;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;

/**
 * Test class for XQueryExpressionPool.
 */
public class XQueryExpressionPoolTest {

    private XQueryExpressionPool pool;
    private StaticQueryContext sqc;

    @BeforeMethod
    public void setUp() {
        sqc = mock(StaticQueryContext.class);
        pool = new XQueryExpressionPool(sqc);
    }

    @Test
    public void testConstructor() {
        // When
        XQueryExpressionPool newPool = new XQueryExpressionPool(sqc);
        
        // Then
        assertNotNull(newPool);
    }

    @Test
    public void testGetCompiledExpressionNewQuery() throws XPathException {
        // Given
        String query = "for $x in 1 to 10 return $x";
        XQueryExpression expectedExpression = mock(XQueryExpression.class);
        when(sqc.compileQuery(query)).thenReturn(expectedExpression);
        
        // When
        XQueryExpression result = pool.getCompiledExpression(query);
        
        // Then
        assertEquals(result, expectedExpression);
        verify(sqc).compileQuery(query);
    }

    @Test
    public void testGetCompiledExpressionCachedQuery() throws XPathException {
        // Given
        String query = "for $x in 1 to 10 return $x";
        XQueryExpression expectedExpression = mock(XQueryExpression.class);
        when(sqc.compileQuery(query)).thenReturn(expectedExpression);
        
        // First call
        XQueryExpression firstResult = pool.getCompiledExpression(query);
        
        // When - Second call should return cached expression
        XQueryExpression secondResult = pool.getCompiledExpression(query);
        
        // Then
        assertEquals(firstResult, secondResult);
        assertEquals(secondResult, expectedExpression);
        verify(sqc, times(1)).compileQuery(query); // Should only be called once
    }

    @Test
    public void testGetCompiledExpressionWithDifferentQueries() throws XPathException {
        // Given
        String query1 = "for $x in 1 to 10 return $x";
        String query2 = "for $y in 1 to 5 return $y * 2";
        XQueryExpression expression1 = mock(XQueryExpression.class);
        XQueryExpression expression2 = mock(XQueryExpression.class);
        when(sqc.compileQuery(query1)).thenReturn(expression1);
        when(sqc.compileQuery(query2)).thenReturn(expression2);
        
        // When
        XQueryExpression result1 = pool.getCompiledExpression(query1);
        XQueryExpression result2 = pool.getCompiledExpression(query2);
        
        // Then
        assertEquals(result1, expression1);
        assertEquals(result2, expression2);
        assertFalse(result1.equals(result2));
        verify(sqc).compileQuery(query1);
        verify(sqc).compileQuery(query2);
    }

    @Test
    public void testGetCompiledExpressionWithNullQuery() throws XPathException {
        // When & Then
        try {
            pool.getCompiledExpression(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void testGetCompiledExpressionWithEmptyQuery() throws XPathException {
        // Given
        String emptyQuery = "";
        XQueryExpression expectedExpression = mock(XQueryExpression.class);
        when(sqc.compileQuery(emptyQuery)).thenReturn(expectedExpression);
        
        // When
        XQueryExpression result = pool.getCompiledExpression(emptyQuery);
        
        // Then
        assertEquals(result, expectedExpression);
        verify(sqc).compileQuery(emptyQuery);
    }

    @Test
    public void testGetCompiledExpressionWithCompilationException() throws XPathException {
        // Given
        String invalidQuery = "invalid xquery syntax";
        when(sqc.compileQuery(invalidQuery)).thenThrow(new XPathException("Invalid syntax"));
        
        // When & Then
        try {
            pool.getCompiledExpression(invalidQuery);
            fail("Expected XPathException");
        } catch (XPathException e) {
            assertEquals(e.getMessage(), "Invalid syntax");
        }
    }

    @Test
    public void testConcurrentAccess() throws XPathException {
        // Given
        String query = "for $x in 1 to 10 return $x";
        XQueryExpression expectedExpression = mock(XQueryExpression.class);
        when(sqc.compileQuery(query)).thenReturn(expectedExpression);
        
        // When - Multiple calls accessing the same query
        XQueryExpression result1 = pool.getCompiledExpression(query);
        XQueryExpression result2 = pool.getCompiledExpression(query);
        XQueryExpression result3 = pool.getCompiledExpression(query);
        
        // Then
        assertEquals(result1, expectedExpression);
        assertEquals(result2, expectedExpression);
        assertEquals(result3, expectedExpression);
        verify(sqc, times(1)).compileQuery(query); // Should only be called once due to caching
    }
}
