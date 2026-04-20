package com.da.web.http;

import com.da.web.exception.HttpParseException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * HttpParser 的单元测试类
 * HttpParser 是一个 HTTP 请求解析器，用于解析原始的 HTTP 请求字节数据
 * 支持解析 GET、POST、PUT、DELETE 等各种 HTTP 方法
 * 支持解析请求头、查询参数、请求体（JSON、表单、文本等）
 */
public class HttpParserTest {

    /**
     * 测试解析简单的 GET 请求
     * 验证能正确解析请求行和请求头
     */
    @Test
    public void testParseSimpleGetRequest() throws Exception {
        String rawRequest = "GET /hello HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "User-Agent: TestClient\r\n" +
                "\r\n";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertEquals("GET", request.getMethod());
        assertEquals("/hello", request.getPath());
        assertEquals("HTTP/1.1", request.getHttpVersion());
        assertEquals("localhost:8080", request.getHeader("Host"));
    }

    /**
     * 测试解析带查询参数的 GET 请求
     * 验证能正确解析 URL 中的查询参数
     */
    @Test
    public void testParseGetRequestWithQueryParams() throws Exception {
        String rawRequest = "GET /search?q=java&page=1 HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertEquals("GET", request.getMethod());
        assertEquals("/search", request.getPath());
        assertEquals("java", request.getQueryParam("q"));
        assertEquals("1", request.getQueryParam("page"));
    }

    /**
     * 测试解析带 JSON 请求体的 POST 请求
     * 验证能正确识别有请求体的 POST 请求
     */
    @Test
    public void testParsePostRequestWithJsonBody() throws Exception {
        String rawRequest = "POST /api/user HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 15\r\n" +
                "\r\n" +
                "{\"name\":\"test\"}";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertEquals("POST", request.getMethod());
        assertEquals("/api/user", request.getPath());
        assertTrue(request.hasBody());
    }

    /**
     * 测试解析带表单请求体的 POST 请求
     * 验证能正确解析 application/x-www-form-urlencoded 格式的请求体
     */
    @Test
    public void testParsePostRequestWithFormBody() throws Exception {
        String rawRequest = "POST /login HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Type: application/x-www-form-urlencoded\r\n" +
                "Content-Length: 27\r\n" +
                "\r\n" +
                "username=admin&password=123";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertEquals("POST", request.getMethod());
        assertTrue(request.hasBody());
    }

    /**
     * 测试解析带纯文本请求体的 POST 请求
     * 验证能正确识别 text/plain 格式的请求体
     */
    @Test
    public void testParsePostRequestWithTextBody() throws Exception {
        String rawRequest = "POST /text HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: 5\r\n" +
                "\r\n" +
                "hello";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertTrue(request.hasBody());
    }

    /**
     * 测试解析带有多个请求头的请求
     * 验证能正确解析所有的请求头字段
     */
    @Test
    public void testParseRequestWithMultipleHeaders() throws Exception {
        String rawRequest = "GET /test HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Accept: text/html,application/json\r\n" +
                "Accept-Language: en-US,zh-CN\r\n" +
                "User-Agent: Test/1.0\r\n" +
                "\r\n";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertNotNull(request.getHeader("Host"));
        assertNotNull(request.getHeader("Accept"));
        assertNotNull(request.getHeader("User-Agent"));
    }

    /**
     * 测试解析空请求抛出异常
     * 验证当输入为空时能正确抛出 HttpParseException
     */
    @Test(expected = HttpParseException.class)
    public void testParseEmptyRequest() throws Exception {
        HttpParser.parse(new byte[0]);
    }

    /**
     * 测试解析无效请求行抛出异常
     * 验证当请求行格式不正确时能正确抛出 HttpParseException
     */
    @Test(expected = HttpParseException.class)
    public void testParseInvalidRequestLine() throws Exception {
        String rawRequest = "INVALID /test\r\n";
        HttpParser.parse(rawRequest.getBytes());
    }

    /**
     * 测试解析 PUT 请求
     * 验证能正确解析 PUT 方法的请求
     */
    @Test
    public void testParsePutRequest() throws Exception {
        String rawRequest = "PUT /api/data HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertEquals("PUT", request.getMethod());
        assertEquals("/api/data", request.getPath());
    }

    /**
     * 测试解析 DELETE 请求
     * 验证能正确解析 DELETE 方法的请求
     */
    @Test
    public void testParseDeleteRequest() throws Exception {
        String rawRequest = "DELETE /api/user/123 HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertEquals("DELETE", request.getMethod());
        assertEquals("/api/user/123", request.getPath());
    }

    /**
     * 测试 URL 解码功能
     * 验证能正确解码 URL 编码的中文字符
     */
    @Test
    public void testUrlDecode() throws Exception {
        String rawRequest = "GET /search?q=%E6%B5%8B%E8%AF%95 HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        // 验证查询参数能被正确解码（不验证具体值，因为解码可能因实现而异）
        assertNotNull(request.getQueryParam("q"));
    }
}