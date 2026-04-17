package com.da.web.core;

import com.da.web.constant.ContentTypes;
import com.da.web.function.WsListener;
import com.da.web.http.HttpParser;
import com.da.web.http.HttpRequest;
import com.da.web.http.HttpResponse;
import com.da.web.util.Utils;
import com.da.web.websocket.WebSocketManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP 请求上下文类
 * <p>
 * 封装请求信息和响应方法
 */
public class Context {
    
    private final HttpRequest request;
    private final HttpResponse response;
    private final SocketChannel channel;
    private WsListener wsListener;

    public Context(SocketChannel channel) {
        this.channel = channel;
        HttpRequest tempRequest = null;
        HttpResponse tempResponse = null;
        try {
            // 使用新的 HTTP 解析器解析请求
            tempRequest = HttpParser.readAndParse(channel);
            tempResponse = new HttpResponse(channel, tempRequest.getHttpVersion());
            printRequestInfo(tempRequest, tempResponse);
        } catch (Exception e) {
            errPrint(e, tempResponse);
        }
        this.request = tempRequest;
        this.response = tempResponse;
    }

    public void setWsListener(WsListener wsListener) {
        this.wsListener = wsListener;
    }

    public WsListener getWsListener() {
        return wsListener;
    }

    /**
     * 获取原始 HTTP 请求对象
     */
    public HttpRequest getRequest() {
        return request;
    }

    /**
     * 获取原始 HTTP 响应对象
     */
    public HttpResponse getResponse() {
        return response;
    }

    public String getUrl() {
        return request != null ? request.getUrl() : null;
    }

    public String getMethod() {
        return request != null ? request.getMethod() : null;
    }

    /**
     * 获取 URL 查询参数（兼容旧 API）
     */
    public Map<String, Object> getParams() {
        return request != null ? request.getQueryParams() : null;
    }

    /**
     * 获取请求体参数（新增）
     */
    public Map<String, Object> getBodyParams() {
        return request != null ? request.getBodyParams() : null;
    }

    /**
     * 获取指定的请求体参数
     */
    public Object getBodyParam(String name) {
        return request != null ? request.getBodyParam(name) : null;
    }

    /**
     * 获取指定的请求头
     */
    public String getHeader(String name) {
        return request != null ? request.getHeader(name) : null;
    }

    /**
     * 获取 Content-Type
     */
    public String getContentType() {
        return request != null ? request.getContentType() : null;
    }

    /**
     * 获取原始请求体字节数组
     */
    public byte[] getRawBody() {
        return request != null ? request.getRawBody() : null;
    }

    /**
     * 获取请求体为 JSON 字符串
     */
    public String getBodyAsJson() {
        if (request == null || !request.hasBody()) {
            return null;
        }
        Object json = request.getBodyParam("json");
        return json != null ? json.toString() : null;
    }

    /**
     * 将请求体 JSON 解析为指定类型（简单实现，返回 Map）
     */
    @SuppressWarnings("unchecked")
    public <T> T getBodyAs(Class<T> clazz) {
        if (request == null || !request.hasBody()) {
            return null;
        }
        
        if (clazz == Map.class) {
            Map<String, Object> bodyParams = request.getBodyParams();
            if (bodyParams != null && bodyParams.containsKey("json")) {
                return (T) bodyParams.get("json");
            }
            return (T) bodyParams;
        }
        
        // 对于其他类型，尝试从 bodyParams 中获取
        return (T) request.getBodyParams();
    }

    public SocketChannel getChannel() {
        return channel;
    }

    private void printRequestInfo(HttpRequest request, HttpResponse response) {
        if (request == null || request.getUrl() == null) return;
        
        String msg = "请求方式 [" + request.getMethod() + "] 请求路径 [" + request.getUrl() + "]";
        if (!request.getQueryParams().isEmpty() || !request.getBodyParams().isEmpty()) {
            System.out.println(msg + " 请求参数 [queryParams=" + request.getQueryParams() + 
                ", bodyParams=" + request.getBodyParams() + "]");
        } else {
            System.out.println(msg);
        }
    }

    /**
     * 发送响应（使用新的 HttpResponse）
     */
    public void send(String headers, int code, String data) {
        try {
            if (response != null) {
                // 解析头部字符串
                String[] headerLines = headers.split("\n");
                for (String line : headerLines) {
                    if (line.contains(":")) {
                        String[] kv = line.split(":", 2);
                        response.header(kv[0].trim(), kv[1].trim());
                    }
                }
                response.status(code).body(data).send();
            } else {
                // 备用方案：直接发送
                String result = "HTTP/1.1 " + code + "\r\n" + headers + "\r\n\r\n" + data;
                channel.write(ByteBuffer.wrap(result.getBytes(StandardCharsets.UTF_8)));
                channel.close();
            }
        } catch (Exception e) {
            errPrint(e, response);
        }
    }

    /**
     * 发送错误信息
     */
    public void errPrint(Exception e, HttpResponse resp) {
        e.printStackTrace();
        try {
            if (resp != null) {
                resp.sendError(500, e.getMessage());
            } else {
                String result = "HTTP/1.1 500\r\n" + 
                    ContentTypes.withDefaultCharset(ContentTypes.TEXT_HTML) + "\r\n\r\n" +
                    "<head><title>500</title></head><body>" +
                    "<h1 style='text-align: center;color: red;'>服务器出错了</h1><hr/>" +
                    "<p>错误信息：" + e.getMessage() + "</p></body></html>";
                channel.write(ByteBuffer.wrap(result.getBytes(StandardCharsets.UTF_8)));
                channel.close();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void errPrint(Exception e) {
        errPrint(e, response);
    }

    public void errPrint(String msg) {
        errPrint(new Exception(msg));
    }

    /**
     * 发送文本
     */
    public void send(String msg) {
        try {
            if (response != null) {
                response.sendText(msg);
            } else {
                send(ContentTypes.withDefaultCharset(ContentTypes.TEXT_PLAIN), 200, msg);
            }
        } catch (IOException e) {
            errPrint(e);
        }
    }

    public void send(String msg, int code) {
        try {
            if (response != null) {
                response.status(code).contentType("text/plain;charset=UTF-8").body(msg).send();
            } else {
                send(ContentTypes.withDefaultCharset(ContentTypes.TEXT_PLAIN), code, msg);
            }
        } catch (IOException e) {
            errPrint(e);
        }
    }

    /**
     * 发送 HTML
     */
    public void sendHtml(String msg) {
        try {
            if (response != null) {
                response.sendHtml(msg);
            } else {
                send(ContentTypes.withDefaultCharset(ContentTypes.TEXT_HTML), 200, msg);
            }
        } catch (IOException e) {
            errPrint(e);
        }
    }

    public void sendHtml(String msg, int code) {
        try {
            if (response != null) {
                response.status(code).contentType("text/html;charset=UTF-8").body(msg).send();
            } else {
                send(ContentTypes.withDefaultCharset(ContentTypes.TEXT_HTML), code, msg);
            }
        } catch (IOException e) {
            errPrint(e);
        }
    }

    /**
     * 发送 HTML 文件
     */
    public void sendHtmlFile(File file) {
        sendHtml(Utils.readHtmlFileToString(file));
    }

    public void sendHtmlFile(File file, int code) {
        sendHtml(Utils.readHtmlFileToString(file), code);
    }

    /**
     * 发送 JSON
     */
    public void sendJson(String msg) {
        try {
            if (response != null) {
                response.sendJson(msg);
            } else {
                send(ContentTypes.withDefaultCharset(ContentTypes.APPLICATION_JSON), 200, msg);
            }
        } catch (IOException e) {
            errPrint(e);
        }
    }

    public void sendJson(String msg, int code) {
        try {
            if (response != null) {
                response.status(code).contentType("application/json;charset=UTF-8").body(msg).send();
            } else {
                send(ContentTypes.withDefaultCharset(ContentTypes.APPLICATION_JSON), code, msg);
            }
        } catch (IOException e) {
            errPrint(e);
        }
    }

    /**
     * 发送文件
     */
    public void send(File file) {
        String fileType = Utils.getFileType(file);
        FileInputStream is = null;
        
        try {
            is = new FileInputStream(file);
            
            if (response != null) {
                response.contentType(fileType + ";charset=utf-8");
                byte[] fileData = is.readAllBytes();
                response.body(fileData).send();
            } else {
                String header = "HTTP/1.1 200\r\nContent-Type: " + fileType + ";charset=utf-8\r\n\r\n";
                channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
                
                FileChannel fc = is.getChannel();
                fc.transferTo(0, fc.size(), channel);
                fc.close();
                channel.close();
            }
        } catch (IOException e) {
            errPrint(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    errPrint(e);
                }
            }
        }
    }
}
