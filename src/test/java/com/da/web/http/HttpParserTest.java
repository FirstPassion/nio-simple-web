package com.da.web.http;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public class HttpParserTest {

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

    @Test(expected = HttpParseException.class)
    public void testParseEmptyRequest() throws Exception {
        HttpParser.parse(new byte[0]);
    }

    @Test(expected = HttpParseException.class)
    public void testParseInvalidRequestLine() throws Exception {
        String rawRequest = "INVALID /test\r\n";
        HttpParser.parse(rawRequest.getBytes());
    }

    @Test
    public void testParsePutRequest() throws Exception {
        String rawRequest = "PUT /api/data HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertEquals("PUT", request.getMethod());
        assertEquals("/api/data", request.getPath());
    }

    @Test
    public void testParseDeleteRequest() throws Exception {
        String rawRequest = "DELETE /api/user/123 HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertEquals("DELETE", request.getMethod());
        assertEquals("/api/user/123", request.getPath());
    }

    @Test
    public void testUrlDecode() throws Exception {
        String rawRequest = "GET /search?q=%E6%B5%8B%E8%AF%95 HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        
        HttpRequest request = HttpParser.parse(rawRequest.getBytes());
        
        assertNotNull(request.getQueryParam("q"));
    }
}