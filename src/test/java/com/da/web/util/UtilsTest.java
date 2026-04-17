package com.da.web.util;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Utils 工具类的单元测试类
 * 包含对字符串判断、数组判断、列表判断、类型转换、类加载等功能的测试
 */
public class UtilsTest {

    /**
     * 测试 isNotBlank 方法 - 判断字符串是否不为空
     * 验证正常字符串和空格返回 true，null 和空字符串返回 false
     */
    @Test
    public void testIsNotBlank() {
        // 测试正常字符串
        assertTrue(Utils.isNotBlank("hello"));
        // 测试空格字符串（空格也算不为空）
        assertTrue(Utils.isNotBlank(" "));
        // 测试 null
        assertFalse(Utils.isNotBlank(null));
        // 测试空字符串
        assertFalse(Utils.isNotBlank(""));
    }

    /**
     * 测试 isBlank 方法 - 判断字符串是否为空
     * 与 isNotBlank 相反，验证 null 和空字符串返回 true
     */
    @Test
    public void testIsBlank() {
        // 测试正常字符串
        assertFalse(Utils.isBlank("hello"));
        // 测试空格字符串
        assertFalse(Utils.isBlank(" "));
        // 测试 null
        assertTrue(Utils.isBlank(null));
        // 测试空字符串
        assertTrue(Utils.isBlank(""));
    }

    /**
     * 测试 isArrayNotNull 方法 - 判断数组是否不为空
     * 验证非 null 且长度大于 0 的数组返回 true
     */
    @Test
    public void testIsArrayNotNull() {
        // 测试有容量的数组（即使元素为 null）
        String[] arr = new String[1];
        assertTrue(Utils.isArrayNotNull(arr));
        // 测试 null 数组
        assertFalse(Utils.isArrayNotNull(null));
        // 测试空数组
        assertFalse(Utils.isArrayNotNull(new String[0]));
    }

    /**
     * 测试 isListNotNull 方法 - 判断列表是否不为空
     * 验证非 null 且 size 大于 0 的列表返回 true
     */
    @Test
    public void testIsListNotNull() {
        // 测试有元素的列表
        List<String> list = java.util.Arrays.asList("a", "b");
        assertTrue(Utils.isListNotNull(list));
        // 测试 null 列表
        assertFalse(Utils.isListNotNull(null));
        // 测试空列表
        assertFalse(Utils.isListNotNull(java.util.Collections.emptyList()));
    }

    /**
     * 测试 replace 方法 - 字符串替换功能
     * 验证将字符串中的指定部分替换为新内容
     */
    @Test
    public void testReplace() {
        // 将 "hello world" 中的 "world" 替换为 "java"
        String result = Utils.replace("hello world", "world", "java");
        assertEquals("hello java", result);
    }

    /**
     * 测试 getTypeConv 方法 - 获取类型转换器
     * 验证各种基本类型和包装类都能获取到对应的转换器
     */
    @Test
    public void testGetTypeConv() {
        // 测试字符串类型
        assertNotNull(Utils.getTypeConv("java.lang.String"));
        // 测试基本类型 int
        assertNotNull(Utils.getTypeConv("int"));
        // 测试包装类 Integer
        assertNotNull(Utils.getTypeConv("java.lang.Integer"));
        // 测试基本类型 boolean
        assertNotNull(Utils.getTypeConv("boolean"));
        // 测试基本类型 long
        assertNotNull(Utils.getTypeConv("long"));
        // 测试基本类型 double
        assertNotNull(Utils.getTypeConv("double"));
        // 测试不存在的类型，应返回 null
        assertNull(Utils.getTypeConv("UnknownType"));
    }

    /**
     * 测试 isReadExist 方法 - 判断类是否存在
     * 验证已知类返回 true，不存在的类返回 false
     */
    @Test
    public void testIsReadExist() {
        // 测试存在的类
        assertTrue(Utils.isReadExist("java.lang.String"));
        assertTrue(Utils.isReadExist("java.util.List"));
        // 测试不存在的类
        assertFalse(Utils.isReadExist("com.notexist.FakeClass"));
    }

    /**
     * 测试 getChatInStringNum 方法 - 统计字符在字符串中出现的次数
     * 注意：方法名可能有拼写错误，应为 getCharInStringNum
     */
    @Test
    public void testGetChatInStringNum() {
        // 测试字符 'l' 在 "hello" 中出现 2 次
        assertEquals(2, Utils.getChatInStringNum("hello", 'l'));
        // 测试不存在的字符
        assertEquals(0, Utils.getChatInStringNum("hello", 'x'));
        // 测试出现 1 次的字符
        assertEquals(1, Utils.getChatInStringNum("abc", 'a'));
    }

    /**
     * 测试 getStrIndex 方法 - 查找字符第 n 次出现的位置
     * 验证能正确找到指定字符第几次出现的索引位置
     */
    @Test
    public void testGetStrIndex() {
        String str = "a,b,c,d";
        // 查找逗号第 1 次出现的位置（从 1 开始计数）
        assertEquals(1, Utils.getStrIndex(str, ",", 1));
    }

    /**
     * 测试 loadClass 方法 - 通过类名加载 Class 对象
     * 验证能正确加载已知的类
     */
    @Test
    public void testLoadClass() {
        // 测试加载 String 类
        Class<?> clz = Utils.loadClass("java.lang.String");
        assertEquals(String.class, clz);
        
        // 测试加载 ArrayList 类
        clz = Utils.loadClass("java.util.ArrayList");
        assertEquals(java.util.ArrayList.class, clz);
    }

    /**
     * 测试 isAnnotation 方法 - 判断类上是否有指定注解
     * 验证能正确检测类上的注解
     */
    @Test
    public void testIsAnnotation() {
        // Deprecated 类上有 Retention 注解
        assertTrue(Utils.isAnnotation(java.lang.Deprecated.class, java.lang.annotation.Retention.class));
        // String 类上没有 Deprecated 注解
        assertFalse(Utils.isAnnotation(java.lang.String.class, java.lang.Deprecated.class));
    }

    /**
     * 测试 isInterface 方法 - 判断类是否实现了指定接口
     * 验证能正确检测类实现的接口
     */
    @Test
    public void testIsInterface() {
        // ArrayList 实现了 List 接口
        assertTrue(Utils.isInterface(java.util.ArrayList.class, java.util.List.class));
        // String 没有实现 List 接口
        assertFalse(Utils.isInterface(java.lang.String.class, java.util.List.class));
    }
}