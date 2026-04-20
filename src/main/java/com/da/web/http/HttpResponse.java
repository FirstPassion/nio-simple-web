package com.da.web.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 响应模型
 */
public class HttpResponse {
    private final SocketChannel channel;
    private String httpVersion = "HTTP/1.1";
    private int statusCode = 200;
    private final MultiValueMap headers;
    private byte[] body;

    public HttpResponse(SocketChannel channel, String httpVersion) {
        this.channel = channel;
        this.httpVersion = httpVersion != null ? httpVersion : "HTTP/1.1";
        this.headers = new MultiValueMap();
    }

    /**
     * 设置状态码
     */
    public HttpResponse status(int code) {
        this.statusCode = code;
        return this;
    }

    /**
     * 设置响应头
     */
    public HttpResponse header(String name, String value) {
        this.headers.add(name, value);
        return this;
    }

    /**
     * 设置 Content-Type
     */
    public HttpResponse contentType(String type) {
        this.headers.add("Content-Type", type);
        return this;
    }

    /**
     * 设置响应体
     */
    public HttpResponse body(byte[] data) {
        this.body = data;
        return this;
    }

    /**
     * 设置响应体（字符串）
     */
    public HttpResponse body(String data) {
        this.body = data.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    /**
     * 获取响应头
     */
    public MultiValueMap getHeaders() {
        return headers;
    }

    /**
     * 获取状态码
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * 发送响应
     */
    public void send() throws IOException {
        // 自动添加 Content-Length
        if (body != null && !headers.containsKey("Content-Length")) {
            headers.add("Content-Length", String.valueOf(body.length));
        }

        // 构建响应
        StringBuilder response = new StringBuilder();
        response.append(httpVersion).append(" ").append(statusCode).append("\r\n");

        // 添加所有头部
        for (String key : headers.keySet()) {
            for (String value : headers.getAll(key)) {
                response.append(key).append(": ").append(value).append("\r\n");
            }
        }

        // 空行分隔头部和身体
        response.append("\r\n");

        // 写入响应
        ByteBuffer buffer = ByteBuffer.wrap(response.toString().getBytes(StandardCharsets.UTF_8));
        channel.write(buffer);

        // 如果有 body，写入 body
        if (body != null) {
            ByteBuffer bodyBuffer = ByteBuffer.wrap(body);
            channel.write(bodyBuffer);
        }

        // 关闭连接
        channel.close();
    }

    /**
     * 快捷方法：发送文本
     */
    public void sendText(String text) throws IOException {
        contentType("text/plain;charset=UTF-8");
        body(text);
        send();
    }

    /**
     * 快捷方法：发送 HTML
     */
    public void sendHtml(String html) throws IOException {
        contentType("text/html;charset=UTF-8");
        body(html);
        send();
    }

    /**
     * 快捷方法：发送 JSON
     */
    public void sendJson(String json) throws IOException {
        contentType("application/json;charset=UTF-8");
        body(json);
        send();
    }

    /**
     * 快捷方法：发送 SSE 事件
     * 用于 Server-Sent Events (SSE) 流式响应
     */
    public void sendSSEvent(String event, String data) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (event != null && !event.isEmpty()) {
            sb.append("event: ").append(event).append("\r\n");
        }
        sb.append("data: ").append(data).append("\r\n\r\n");
        ByteBuffer buffer = ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8));
        channel.write(buffer);
    }

    /**
     * 快捷方法：发送 SSE 数据（不带事件类型）
     */
    public void sendSSData(String data) throws IOException {
        sendSSEvent(null, data);
    }

    /**
     * 发送 SSE 响应头（不关闭连接）
     * 用于初始化 SSE 流式连接
     */
    public void sendSSEHeaders() throws IOException {
        // 构建 SSE 响应头
        String response = httpVersion + " " + statusCode + "\r\n" +
                "Content-Type: text/event-stream;charset=UTF-8\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Connection: keep-alive\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "\r\n";

        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
        channel.write(buffer);
        // 注意：这里不关闭连接，保持长连接用于流式传输
    }

    /**
     * 直接通过 SocketChannel 发送数据（不关闭连接）
     * 用于 SSE 流式传输
     */
    public void writeDirect(String data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));
        channel.write(buffer);
    }

    /**
     * 快捷方法：发送错误响应
     */
    public void sendError(int code, String message) throws IOException {
        status(code);
        contentType("text/html;charset=UTF-8");
        String html = "<html><head><title>" + code + "</title></head><body>" +
                "<h1 style='text-align: center; color: red;'>Error " + code + "</h1>" +
                "<hr/><p>" + message + "</p></body></html>";
        body(html);
        send();
    }

    /**
     * 保持连接活跃（用于 SSE 长连接）
     * 发送注释行作为心跳，防止连接超时
     */
    public void sendSSEKeepAlive() throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(": keep-alive\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        channel.write(buffer);
    }
}
