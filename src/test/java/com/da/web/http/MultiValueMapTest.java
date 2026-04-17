package com.da.web.http;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class MultiValueMapTest {

    @Test
    public void testAddAndGet() {
        MultiValueMap map = new MultiValueMap();
        map.add("Content-Type", "application/json");
        
        assertEquals("application/json", map.get("Content-Type"));
        assertEquals("application/json", map.get("content-type"));
    }

    @Test
    public void testAddMultipleValues() {
        MultiValueMap map = new MultiValueMap();
        map.add("Set-Cookie", "cookie1");
        map.add("Set-Cookie", "cookie2");
        
        List<String> values = map.getAll("Set-Cookie");
        assertEquals(2, values.size());
        assertEquals("cookie1", values.get(0));
        assertEquals("cookie2", values.get(1));
    }

    @Test
    public void testContainsKey() {
        MultiValueMap map = new MultiValueMap();
        map.add("Host", "localhost");
        
        assertTrue(map.containsKey("Host"));
        assertTrue(map.containsKey("host"));
        assertFalse(map.containsKey("Missing"));
    }

    @Test
    public void testKeySet() {
        MultiValueMap map = new MultiValueMap();
        map.add("Host", "localhost");
        map.add("Content-Type", "application/json");
        
        assertEquals(2, map.keySet().size());
        assertTrue(map.keySet().contains("Host"));
        assertTrue(map.keySet().contains("Content-Type"));
    }

    @Test
    public void testSize() {
        MultiValueMap map = new MultiValueMap();
        assertEquals(0, map.size());
        
        map.add("Header1", "value1");
        assertEquals(1, map.size());
        
        map.add("Header2", "value2");
        assertEquals(2, map.size());
    }

    @Test
    public void testIsEmpty() {
        MultiValueMap map = new MultiValueMap();
        assertTrue(map.isEmpty());
        
        map.add("Header", "value");
        assertFalse(map.isEmpty());
    }

    @Test
    public void testGetNonExistent() {
        MultiValueMap map = new MultiValueMap();
        assertNull(map.get("Missing"));
    }

    @Test
    public void testToSingleValueMap() {
        MultiValueMap map = new MultiValueMap();
        map.add("Set-Cookie", "cookie1");
        map.add("Set-Cookie", "cookie2");
        
        java.util.Map<String, String> singleMap = map.toSingleValueMap();
        assertEquals("cookie1", singleMap.get("Set-Cookie"));
    }

    @Test
    public void testCaseInsensitiveOrder() {
        MultiValueMap map = new MultiValueMap();
        map.add("content-type", "application/json");
        
        assertEquals("application/json", map.get("Content-Type"));
    }
}