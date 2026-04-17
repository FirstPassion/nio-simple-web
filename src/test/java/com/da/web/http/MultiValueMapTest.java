package com.da.web.http;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * MultiValueMap 的单元测试类
 * MultiValueMap 是一个支持多值的 Map 实现，主要用于存储 HTTP 头部
 * 特点：键名不区分大小写，同一个键可以存储多个值
 */
public class MultiValueMapTest {

    /**
     * 测试 add 和 get 方法 - 添加和获取单个值
     * 验证键名不区分大小写的特性
     */
    @Test
    public void testAddAndGet() {
        MultiValueMap map = new MultiValueMap();
        // 添加一个头部
        map.add("Content-Type", "application/json");
        
        // 验证能正确获取值
        assertEquals("application/json", map.get("Content-Type"));
        // 验证键名不区分大小写
        assertEquals("application/json", map.get("content-type"));
    }

    /**
     * 测试添加多个值的功能
     * 验证同一个键可以存储多个值，并能正确获取所有值
     */
    @Test
    public void testAddMultipleValues() {
        MultiValueMap map = new MultiValueMap();
        // 添加两个同名的 Cookie
        map.add("Set-Cookie", "cookie1");
        map.add("Set-Cookie", "cookie2");
        
        // 获取所有值
        List<String> values = map.getAll("Set-Cookie");
        // 验证有两个值
        assertEquals(2, values.size());
        // 验证值的顺序
        assertEquals("cookie1", values.get(0));
        assertEquals("cookie2", values.get(1));
    }

    /**
     * 测试 containsKey 方法 - 检查是否包含某个键
     * 验证键名不区分大小写的特性
     */
    @Test
    public void testContainsKey() {
        MultiValueMap map = new MultiValueMap();
        map.add("Host", "localhost");
        
        // 验证存在该键
        assertTrue(map.containsKey("Host"));
        // 验证键名不区分大小写
        assertTrue(map.containsKey("host"));
        // 验证不存在的键
        assertFalse(map.containsKey("Missing"));
    }

    /**
     * 测试 keySet 方法 - 获取所有键的集合
     * 验证能正确返回所有已添加的键
     */
    @Test
    public void testKeySet() {
        MultiValueMap map = new MultiValueMap();
        map.add("Host", "localhost");
        map.add("Content-Type", "application/json");
        
        // 验证键的数量
        assertEquals(2, map.keySet().size());
        // 验证包含指定的键
        assertTrue(map.keySet().contains("Host"));
        assertTrue(map.keySet().contains("Content-Type"));
    }

    /**
     * 测试 size 方法 - 获取 Map 的大小（键的数量）
     * 验证添加不同键时大小的变化
     */
    @Test
    public void testSize() {
        MultiValueMap map = new MultiValueMap();
        // 初始为空
        assertEquals(0, map.size());
        
        // 添加第一个键
        map.add("Header1", "value1");
        assertEquals(1, map.size());
        
        // 添加第二个不同的键
        map.add("Header2", "value2");
        assertEquals(2, map.size());
    }

    /**
     * 测试 isEmpty 方法 - 判断 Map 是否为空
     * 验证添加元素前后状态的变化
     */
    @Test
    public void testIsEmpty() {
        MultiValueMap map = new MultiValueMap();
        // 初始为空
        assertTrue(map.isEmpty());
        
        // 添加元素后不为空
        map.add("Header", "value");
        assertFalse(map.isEmpty());
    }

    /**
     * 测试获取不存在的键
     * 验证返回 null
     */
    @Test
    public void testGetNonExistent() {
        MultiValueMap map = new MultiValueMap();
        // 获取不存在的键应返回 null
        assertNull(map.get("Missing"));
    }

    /**
     * 测试 toSingleValueMap 方法 - 转换为普通 Map
     * 验证当有多个值时，只取第一个值
     */
    @Test
    public void testToSingleValueMap() {
        MultiValueMap map = new MultiValueMap();
        map.add("Set-Cookie", "cookie1");
        map.add("Set-Cookie", "cookie2");
        
        // 转换为单值 Map
        java.util.Map<String, String> singleMap = map.toSingleValueMap();
        // 验证只取第一个值
        assertEquals("cookie1", singleMap.get("Set-Cookie"));
    }

    /**
     * 测试大小写不敏感的键匹配
     * 验证无论用什么大小写添加或获取，都能正确匹配
     */
    @Test
    public void testCaseInsensitiveOrder() {
        MultiValueMap map = new MultiValueMap();
        // 用小写添加
        map.add("content-type", "application/json");
        
        // 用混合大小写获取
        assertEquals("application/json", map.get("Content-Type"));
    }
}