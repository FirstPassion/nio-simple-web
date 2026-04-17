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
}
