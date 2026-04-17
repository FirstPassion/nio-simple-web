package com.da.web.http;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * JsonParser 的单元测试类
 * JsonParser 是一个简单的 JSON 解析器，使用递归下降法实现
 * 支持解析对象、数组、字符串、数字、布尔值和 null
 */
public class JsonParserTest {

    /**
     * 测试解析空对象
     * 验证能正确解析 "{}" 并返回空 Map
     */
    @Test
    public void testParseEmptyObject() {
        JsonParser parser = new JsonParser("{}");
        Map<String, Object> result = (Map<String, Object>) parser.parse();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * 测试解析简单对象
     * 验证能正确解析包含字符串和数字的 JSON 对象
     */
    @Test
    public void testParseSimpleObject() {
        JsonParser parser = new JsonParser("{\"name\":\"test\",\"age\":123}");
        Map<String, Object> result = (Map<String, Object>) parser.parse();
        assertEquals("test", result.get("name"));
        assertEquals(123L, ((Number)result.get("age")).longValue());
    }

    /**
     * 测试解析嵌套对象
     * 验证能正确解析包含子对象的 JSON
     */
    @Test
    public void testParseNestedObject() {
        JsonParser parser = new JsonParser("{\"user\":{\"name\":\"John\",\"age\":30}}");
        Map<String, Object> result = (Map<String, Object>) parser.parse();
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        assertEquals("John", user.get("name"));
        assertEquals(30L, ((Number)user.get("age")).longValue());
    }

    /**
     * 测试解析数组
     * 验证能正确解析 JSON 数组 [1,2,3]
     */
    @Test
    public void testParseArray() {
        JsonParser parser = new JsonParser("[1,2,3]");
        java.util.List<Object> result = (java.util.List<Object>) parser.parse();
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    /**
     * 测试解析对象数组
     * 验证能正确解析包含多个对象的数组
     */
    @Test
    public void testParseArrayOfObjects() {
        JsonParser parser = new JsonParser("[{\"a\":1},{\"b\":2}]");
        java.util.List<Object> result = (java.util.List<Object>) parser.parse();
        assertEquals(2, result.size());
        Map<String, Object> first = (Map<String, Object>) result.get(0);
        assertEquals(1L, ((Number)first.get("a")).longValue());
    }

    /**
     * 测试解析布尔值
     * 验证能正确解析 true 和 false
     */
    @Test
    public void testParseBoolean() {
        // 测试 true
        JsonParser parserTrue = new JsonParser("true");
        assertEquals(true, parserTrue.parse());
        
        // 测试 false
        JsonParser parserFalse = new JsonParser("false");
        assertEquals(false, parserFalse.parse());
    }

    /**
     * 测试解析 null 值
     * 验证能正确解析 null 并返回 Java 的 null
     */
    @Test
    public void testParseNull() {
        JsonParser parser = new JsonParser("null");
        assertNull(parser.parse());
    }

    /**
     * 测试解析数字
     * 验证能正确解析整数、浮点数和负数
     */
    @Test
    public void testParseNumber() {
        // 测试正整数
        JsonParser parserInt = new JsonParser("123");
        assertEquals(123L, ((Number)parserInt.parse()).longValue());
        
        // 测试浮点数
        JsonParser parserDouble = new JsonParser("123.45");
        assertEquals(123.45, parserDouble.parse());
        
        // 测试负数
        JsonParser parserNegative = new JsonParser("-456");
        assertEquals(-456L, ((Number)parserNegative.parse()).longValue());
    }

    /**
     * 测试解析字符串
     * 验证能正确解析普通字符串
     */
    @Test
    public void testParseString() {
        JsonParser parser = new JsonParser("\"hello world\"");
        assertEquals("hello world", parser.parse());
    }

    /**
     * 测试解析带转义字符的字符串
     * 验证能正确处理 \\n \\t 等转义序列
     */
    @Test
    public void testParseStringWithEscapes() {
        JsonParser parser = new JsonParser("\"hello\\nworld\\t\"");
        assertEquals("hello\nworld\t", parser.parse());
    }

    /**
     * 测试解析 Unicode 转义字符
     * 验证能正确处理 \\u0041 这样的 Unicode 编码
     */
    @Test
    public void testParseStringWithUnicode() {
        JsonParser parser = new JsonParser("\"\\u0041\"");
        assertEquals("A", parser.parse());
    }

    /**
     * 测试解析无效 JSON 抛出异常
     * 验证当输入非法 JSON 时能正确抛出 RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testParseInvalidJson() {
        JsonParser parser = new JsonParser("{invalid}");
        parser.parse();
    }

    /**
     * 测试解析带有空白字符的对象
     * 验证能正确处理 JSON 中的空格和换行
     */
    @Test
    public void testParseObjectWithWhitespace() {
        JsonParser parser = new JsonParser("  {  \"key\"  :  \"value\"  }  ");
        Map<String, Object> result = (Map<String, Object>) parser.parse();
        assertEquals("value", result.get("key"));
    }

    /**
     * 测试解析科学计数法数字
     * 验证能正确处理 1.5e10 这样的科学计数法表示
     */
    @Test
    public void testParseScientificNotation() {
        JsonParser parser = new JsonParser("1.5e10");
        Number result = (Number) parser.parse();
        assertEquals(1.5e10, result.doubleValue(), 0.0001);
    }
}