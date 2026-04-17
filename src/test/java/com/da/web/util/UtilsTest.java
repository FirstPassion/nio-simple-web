package com.da.web.util;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void testIsNotBlank() {
        assertTrue(Utils.isNotBlank("hello"));
        assertTrue(Utils.isNotBlank(" "));
        assertFalse(Utils.isNotBlank(null));
        assertFalse(Utils.isNotBlank(""));
    }

    @Test
    public void testIsBlank() {
        assertFalse(Utils.isBlank("hello"));
        assertFalse(Utils.isBlank(" "));
        assertTrue(Utils.isBlank(null));
        assertTrue(Utils.isBlank(""));
    }

    @Test
    public void testIsArrayNotNull() {
        String[] arr = new String[1];
        assertTrue(Utils.isArrayNotNull(arr));
        assertFalse(Utils.isArrayNotNull(null));
        assertFalse(Utils.isArrayNotNull(new String[0]));
    }

    @Test
    public void testIsListNotNull() {
        List<String> list = java.util.Arrays.asList("a", "b");
        assertTrue(Utils.isListNotNull(list));
        assertFalse(Utils.isListNotNull(null));
        assertFalse(Utils.isListNotNull(java.util.Collections.emptyList()));
    }

    @Test
    public void testReplace() {
        String result = Utils.replace("hello world", "world", "java");
        assertEquals("hello java", result);
    }

    @Test
    public void testGetTypeConv() {
        assertNotNull(Utils.getTypeConv("java.lang.String"));
        assertNotNull(Utils.getTypeConv("int"));
        assertNotNull(Utils.getTypeConv("java.lang.Integer"));
        assertNotNull(Utils.getTypeConv("boolean"));
        assertNotNull(Utils.getTypeConv("long"));
        assertNotNull(Utils.getTypeConv("double"));
        assertNull(Utils.getTypeConv("UnknownType"));
    }

    @Test
    public void testIsReadExist() {
        assertTrue(Utils.isReadExist("java.lang.String"));
        assertTrue(Utils.isReadExist("java.util.List"));
        assertFalse(Utils.isReadExist("com.notexist.FakeClass"));
    }

@Test
    public void testGetChatInStringNum() {
        assertEquals(2, Utils.getChatInStringNum("hello", 'l'));
        assertEquals(0, Utils.getChatInStringNum("hello", 'x'));
        assertEquals(1, Utils.getChatInStringNum("abc", 'a'));
    }

    @Test
    public void testGetStrIndex() {
        String str = "a,b,c,d";
        assertEquals(1, Utils.getStrIndex(str, ",", 1));
    }

    @Test
    public void testLoadClass() {
        Class<?> clz = Utils.loadClass("java.lang.String");
        assertEquals(String.class, clz);
        
        clz = Utils.loadClass("java.util.ArrayList");
        assertEquals(java.util.ArrayList.class, clz);
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(Utils.isAnnotation(java.lang.Deprecated.class, java.lang.annotation.Retention.class));
        assertFalse(Utils.isAnnotation(java.lang.String.class, java.lang.Deprecated.class));
    }

    @Test
    public void testIsInterface() {
        assertTrue(Utils.isInterface(java.util.ArrayList.class, java.util.List.class));
        assertFalse(Utils.isInterface(java.lang.String.class, java.util.List.class));
    }
}