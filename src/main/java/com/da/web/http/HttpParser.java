package com.da.web.http;

import com.da.web.exception.HttpParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求解析器 - 使用递归下降法实现
 */
public class HttpParser {

    /**
     * 从 SocketChannel 读取并解析 HTTP 请求
     */
    public static HttpRequest readAndParse(SocketChannel channel) throws IOException, HttpParseException {
        byte[] requestData = readRequest(channel);
        return parse(requestData);
    }

    /**
     * 从字节数组解析 HTTP 请求
     */
    public static HttpRequest parse(byte[] requestData) throws HttpParseException {
        if (requestData == null || requestData.length == 0) {
            throw new HttpParseException("Empty request");
        }

        ParserContext ctx = new ParserContext(requestData);
        HttpRequest request = new HttpRequest();

        // 解析请求行
        parseRequestLine(ctx, request);

        // 解析请求头
        parseHeaders(ctx, request);

        // 解析请求体
        parseBody(ctx, request);

        return request;
    }

    /**
     * 从 SocketChannel 读取原始数据
     */
    private static byte[] readRequest(SocketChannel channel) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        int bytesRead;
        boolean hasData = false;
        while (true) {
            bytesRead = channel.read(buffer);
            
            if (bytesRead > 0) {
                hasData = true;
                buffer.flip();
                byte[] data = new byte[bytesRead];
                buffer.get(data);
                baos.write(data);
                
                // 检查是否已读取完整的 HTTP 请求
                byte[] currentData = baos.toByteArray();
                if (isCompleteRequest(currentData)) {
                    break;
                }
                
                // 扩容缓冲区
                if (buffer.remaining() < 64) {
                    buffer = ByteBuffer.allocate(buffer.capacity() * 2);
                } else {
                    buffer.clear();
                }
            } else if (bytesRead == -1 || (bytesRead == 0 && hasData)) {
                // 连接关闭或没有更多数据
                break;
            }
            // bytesRead == 0 且没有数据，继续等待
        }
        
        if (!hasData) {
            return new byte[0];
        }
        return baos.toByteArray();
    }

    /**
     * 检查是否是完整的 HTTP 请求
     */
    private static boolean isCompleteRequest(byte[] data) {
        if (data.length < 4) {
            return false;
        }
        
        // 查找 headers 结束标记 \r\n\r\n
        String content = new String(data, StandardCharsets.ISO_8859_1);
        int headerEndIndex = content.indexOf("\r\n\r\n");
        
        if (headerEndIndex == -1) {
            return false;
        }
        
        // 检查是否有 Content-Length 或 Transfer-Encoding
        String headersPart = content.substring(0, headerEndIndex);
        int contentLength = getContentLength(headersPart);
        
        if (contentLength == -1) {
            // 没有 Content-Length，假设请求完整
            return true;
        }
        
        // 计算 body 的起始位置
        int bodyStartIndex = headerEndIndex + 4;
        int bodyLength = data.length - bodyStartIndex;
        
        // 检查 body 是否完整
        return bodyLength >= contentLength;
    }

    /**
     * 从头部字符串中获取 Content-Length
     */
    private static int getContentLength(String headers) {
        String[] lines = headers.split("\r\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    return Integer.parseInt(line.substring(15).trim());
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * 解析请求行 (递归下降入口)
     * Request-Line = Method SP Request-URI SP HTTP-Version CRLF
     */
    private static void parseRequestLine(ParserContext ctx, HttpRequest request) throws HttpParseException {
        String line = readLine(ctx);
        if (line.isEmpty()) {
            throw new HttpParseException("Empty request line");
        }

        String[] parts = splitRequestLine(line);
        if (parts.length != 3) {
            throw new HttpParseException("Invalid request line: " + line);
        }

        request.setMethod(parts[0].trim().toUpperCase());
        parseRequestUri(ctx, request, parts[1]);
        request.setHttpVersion(parts[2].trim());
    }

    /**
     * 分割请求行
     */
    private static String[] splitRequestLine(String line) {
        // 手动分割，处理多个空格的情况
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSpace = false;
        
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                if (!inSpace && current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                    inSpace = true;
                }
            } else {
                inSpace = false;
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts.toArray(new String[0]);
    }

    /**
     * 解析请求 URI（包含路径和查询参数）
     */
    private static void parseRequestUri(ParserContext ctx, HttpRequest request, String uri) {
        if (uri.contains("?")) {
            int queryIndex = uri.indexOf("?");
            request.setPath(uri.substring(0, queryIndex));
            request.setUrl(request.getPath());
            parseQueryString(ctx, request, uri.substring(queryIndex + 1));
        } else {
            request.setPath(uri);
            request.setUrl(uri);
        }
    }

    /**
     * 解析查询字符串
     */
    private static void parseQueryString(ParserContext ctx, HttpRequest request, String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return;
        }

        Map<String, Object> params = new HashMap<>();
        String[] pairs = queryString.split("&");
        
        for (String pair : pairs) {
            if (pair.contains("=")) {
                String[] kv = pair.split("=", 2);
                String key = urlDecode(kv[0]);
                String value = kv.length > 1 ? urlDecode(kv[1]) : "";
                params.put(key, value);
            } else {
                params.put(urlDecode(pair), "");
            }
        }
        
        request.setQueryParams(params);
    }

    /**
     * URL 解码
     */
    private static String urlDecode(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        try {
            return java.net.URLDecoder.decode(str, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return str;
        }
    }

    /**
     * 解析请求头
     */
    private static void parseHeaders(ParserContext ctx, HttpRequest request) throws HttpParseException {
        MultiValueMap headers = new MultiValueMap();
        
        while (ctx.hasRemaining()) {
            String line = readLine(ctx);
            if (line.isEmpty()) {
                // 空行表示 headers 结束
                break;
            }
            
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String name = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.add(name, value);
                
                // 特殊处理 Content-Type 和 charset
                if ("Content-Type".equalsIgnoreCase(name)) {
                    request.setContentType(value);
                    parseCharset(value, request);
                }
            }
        }
        
        request.setHeaders(headers);
    }

    /**
     * 从 Content-Type 中解析 charset
     */
    private static void parseCharset(String contentType, HttpRequest request) {
        if (contentType == null) {
            return;
        }
        
        String lower = contentType.toLowerCase();
        int charsetIndex = lower.indexOf("charset=");
        if (charsetIndex != -1) {
            String charset = contentType.substring(charsetIndex + 8).trim();
            // 移除可能的分号或其他参数
            int semicolonIndex = charset.indexOf(';');
            if (semicolonIndex != -1) {
                charset = charset.substring(0, semicolonIndex).trim();
            }
            // 移除引号
            charset = charset.replace("\"", "").replace("'", "");
            if (!charset.isEmpty()) {
                request.setCharset(charset);
            }
        }
    }

    /**
     * 解析请求体
     */
    private static void parseBody(ParserContext ctx, HttpRequest request) throws HttpParseException {
        String contentType = request.getContentType();
        if (contentType == null) {
            return;
        }

        int contentLength = getContentLengthFromHeaders(request);
        if (contentLength <= 0) {
            return;
        }

        byte[] body = new byte[contentLength];
        if (ctx.remaining() >= contentLength) {
            ctx.read(body, contentLength);
            request.setRawBody(body);
            
            // 根据 Content-Type 解析 body
            parseBodyContent(request, body, contentType);
        }
    }

    /**
     * 从 headers 获取 Content-Length
     */
    private static int getContentLengthFromHeaders(HttpRequest request) {
        String contentLengthStr = request.getHeader("Content-Length");
        if (contentLengthStr != null) {
            try {
                return Integer.parseInt(contentLengthStr.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 根据 Content-Type 解析请求体内容
     */
    private static void parseBodyContent(HttpRequest request, byte[] body, String contentType) {
        String lowerContentType = contentType.toLowerCase();
        
        if (lowerContentType.startsWith("application/json")) {
            parseJsonBody(request, body);
        } else if (lowerContentType.startsWith("application/x-www-form-urlencoded")) {
            parseFormBody(request, body);
        } else if (lowerContentType.startsWith("multipart/form-data")) {
            parseMultipartBody(request, body, contentType);
        } else if (lowerContentType.startsWith("text/plain")) {
            parseTextBody(request, body);
        }
    }

    /**
     * 解析 JSON 请求体
     */
    private static void parseJsonBody(HttpRequest request, byte[] body) {
        try {
            String jsonString = new String(body, request.getCharset());
            // 简单 JSON 解析，将整个 JSON 作为 "json" 参数
            Map<String, Object> bodyParams = new HashMap<>();
            bodyParams.put("json", jsonString);
            
            // 尝试解析 JSON 对象为 Map
            Map<String, Object> parsedJson = parseSimpleJson(jsonString);
            if (parsedJson != null && !parsedJson.isEmpty()) {
                bodyParams.putAll(parsedJson);
            }
            
            request.setBodyParams(bodyParams);
        } catch (Exception e) {
            // JSON 解析失败，保留原始字符串
            Map<String, Object> bodyParams = new HashMap<>();
            bodyParams.put("json", new String(body, StandardCharsets.UTF_8));
            request.setBodyParams(bodyParams);
        }
    }

    /**
     * 简单的 JSON 解析器（递归下降）
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseSimpleJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return null;
        }
        
        try {
            JsonParser parser = new JsonParser(json);
            return (Map<String, Object>) parser.parse();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析表单请求体
     */
    private static void parseFormBody(HttpRequest request, byte[] body) {
        try {
            String formString = new String(body, request.getCharset());
            Map<String, Object> bodyParams = new HashMap<>();
            
            String[] pairs = formString.split("&");
            for (String pair : pairs) {
                if (pair.contains("=")) {
                    String[] kv = pair.split("=", 2);
                    String key = urlDecode(kv[0].trim());
                    String value = kv.length > 1 ? urlDecode(kv[1].trim()) : "";
                    bodyParams.put(key, value);
                }
            }
            
            request.setBodyParams(bodyParams);
        } catch (Exception e) {
            // 解析失败
        }
    }

    /**
     * 解析 multipart/form-data 请求体
     */
    private static void parseMultipartBody(HttpRequest request, byte[] body, String contentType) {
        // 提取 boundary
        String boundary = extractBoundary(contentType);
        if (boundary == null || body.length == 0) {
            return;
        }

        Map<String, Object> bodyParams = new HashMap<>();
        String bodyString = new String(body, StandardCharsets.ISO_8859_1);
        
        String[] parts = bodyString.split("--" + boundary);
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty() || part.equals("--")) {
                continue;
            }
            
            // 解析每个 part
            int headerEndIndex = part.indexOf("\r\n\r\n");
            if (headerEndIndex == -1) {
                continue;
            }
            
            String headers = part.substring(0, headerEndIndex);
            String value = part.substring(headerEndIndex + 4).trim();
            
            // 移除末尾的 --
            if (value.endsWith("--")) {
                value = value.substring(0, value.length() - 2).trim();
            }
            
            // 提取 field name
            String name = extractFieldName(headers);
            if (name != null) {
                bodyParams.put(name, value);
            }
        }
        
        request.setBodyParams(bodyParams);
    }

    /**
     * 从 Content-Type 中提取 boundary
     */
    private static String extractBoundary(String contentType) {
        int boundaryIndex = contentType.toLowerCase().indexOf("boundary=");
        if (boundaryIndex != -1) {
            String boundary = contentType.substring(boundaryIndex + 9).trim();
            // 移除引号
            boundary = boundary.replace("\"", "").replace("'", "");
            // 移除可能的分号或其他参数
            int semicolonIndex = boundary.indexOf(';');
            if (semicolonIndex != -1) {
                boundary = boundary.substring(0, semicolonIndex).trim();
            }
            return boundary;
        }
        return null;
    }

    /**
     * 从 multipart headers 中提取 field name
     */
    private static String extractFieldName(String headers) {
        String[] lines = headers.split("\r\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("name=")) {
                int nameIndex = line.toLowerCase().indexOf("name=");
                String name = line.substring(nameIndex + 5).trim();
                // 移除引号
                name = name.replace("\"", "").replace("'", "");
                // 移除可能的分号
                int semicolonIndex = name.indexOf(';');
                if (semicolonIndex != -1) {
                    name = name.substring(0, semicolonIndex).trim();
                }
                return name;
            }
        }
        return null;
    }

    /**
     * 解析纯文本请求体
     */
    private static void parseTextBody(HttpRequest request, byte[] body) {
        try {
            String text = new String(body, request.getCharset());
            Map<String, Object> bodyParams = new HashMap<>();
            bodyParams.put("text", text);
            request.setBodyParams(bodyParams);
        } catch (Exception e) {
            // 解析失败
        }
    }

    /**
     * 读取一行（以 \r\n 结尾）
     */
    private static String readLine(ParserContext ctx) {
        StringBuilder sb = new StringBuilder();
        
        while (ctx.hasRemaining()) {
            byte b = ctx.readByte();
            if (b == '\r') {
                if (ctx.hasRemaining() && ctx.peekByte() == '\n') {
                    ctx.readByte(); // 消耗 \n
                    break;
                } else {
                    sb.append((char) b);
                }
            } else if (b == '\n') {
                break;
            } else {
                sb.append((char) b);
            }
        }
        
        return sb.toString();
    }

    /**
     * 解析器上下文
     */
    private static class ParserContext {
        private final byte[] data;
        private int position = 0;

        ParserContext(byte[] data) {
            this.data = data;
        }

        boolean hasRemaining() {
            return position < data.length;
        }

        int remaining() {
            return data.length - position;
        }

        byte readByte() {
            if (position >= data.length) {
                throw new IndexOutOfBoundsException("No more data");
            }
            return data[position++];
        }

        void read(byte[] buffer, int length) {
            if (position + length > data.length) {
                throw new IndexOutOfBoundsException("Not enough data");
            }
            System.arraycopy(data, position, buffer, 0, length);
            position += length;
        }

        byte peekByte() {
            if (position >= data.length) {
                throw new IndexOutOfBoundsException("No more data");
            }
            return data[position];
        }
    }
}
