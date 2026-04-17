package com.da.web.http;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public class JsonParserTest {

    @Test
    public void testParseEmptyObject() {
        JsonParser parser = new JsonParser("{}");
        Map<String, Object> result = (Map<String, Object>) parser.parse();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseSimpleObject() {
        JsonParser parser = new JsonParser("{\"name\":\"test\",\"age\":123}");
        Map<String, Object> result = (Map<String, Object>) parser.parse();
        assertEquals("test", result.get("name"));
        assertEquals(123L, ((Number)result.get("age")).longValue());
    }

    @Test
    public void testParseNestedObject() {
        JsonParser parser = new JsonParser("{\"user\":{\"name\":\"John\",\"age\":30}}");
        Map<String, Object> result = (Map<String, Object>) parser.parse();
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals("John", user.get("name"));
        assertEquals(30L, ((Number)user.get("age")).longValue());
    }

    @Test
    public void testParseArray() {
        JsonParser parser = new JsonParser("[1,2,3]");
        java.util.List<Object> result = (java.util.List<Object>) parser.parse();
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    public void testParseArrayOfObjects() {
        JsonParser parser = new JsonParser("[{\"a\":1},{\"b\":2}]");
        java.util.List<Object> result = (java.util.List<Object>) parser.parse();
        assertEquals(2, result.size());
        Map<String, Object> first = (Map<String, Object>) result.get(0);
        assertEquals(1L, ((Number)first.get("a")).longValue());
    }

    @Test
    public void testParseBoolean() {
        JsonParser parserTrue = new JsonParser("true");
        assertEquals(true, parserTrue.parse());
        
        JsonParser parserFalse = new JsonParser("false");
        assertEquals(false, parserFalse.parse());
    }

    @Test
    public void testParseNull() {
        JsonParser parser = new JsonParser("null");
        assertNull(parser.parse());
    }

    @Test
    public void testParseNumber() {
        JsonParser parserInt = new JsonParser("123");
        assertEquals(123L, ((Number)parserInt.parse()).longValue());
        
        JsonParser parserDouble = new JsonParser("123.45");
        assertEquals(123.45, parserDouble.parse());
        
        JsonParser parserNegative = new JsonParser("-456");
        assertEquals(-456L, ((Number)parserNegative.parse()).longValue());
    }

    @Test
    public void testParseString() {
        JsonParser parser = new JsonParser("\"hello world\"");
        assertEquals("hello world", parser.parse());
    }

    @Test
    public void testParseStringWithEscapes() {
        JsonParser parser = new JsonParser("\"hello\\nworld\\t\"");
        assertEquals("hello\nworld\t", parser.parse());
    }

    @Test
    public void testParseStringWithUnicode() {
        JsonParser parser = new JsonParser("\"\\u0041\"");
        assertEquals("A", parser.parse());
    }

    @Test(expected = RuntimeException.class)
    public void testParseInvalidJson() {
        JsonParser parser = new JsonParser("{invalid}");
        parser.parse();
    }

    @Test
    public void testParseObjectWithWhitespace() {
        JsonParser parser = new JsonParser("  {  \"key\"  :  \"value\"  }  ");
        Map<String, Object> result = (Map<String, Object>) parser.parse();
        assertEquals("value", result.get("key"));
    }

    @Test
    public void testParseScientificNotation() {
        JsonParser parser = new JsonParser("1.5e10");
        Number result = (Number) parser.parse();
        assertEquals(1.5e10, result.doubleValue(), 0.0001);
    }
}