package com.da.web.core;

import com.da.web.constant.ContentTypes;
import com.da.web.function.WsListener;
import com.da.web.http.HttpParser;
import com.da.web.http.HttpRequest;
import com.da.web.http.HttpResponse;
import com.da.web.util.Utils;
import com.da.web.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP 请求上下文类
 * <p>
 * 封装请求信息和响应方法
 */
public class Context {
    
    // Session 存储（内存实现）
    private static final Map<String, Map<String, Object>> SESSION_STORE = new ConcurrentHashMap<>();
    // Session ID -> 过期时间
    private static final Map<String, Long> SESSION_EXPIRY = new ConcurrentHashMap<>();
    // Session 默认过期时间（30 分钟）
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;
    
    private final HttpRequest request;
    private final HttpResponse response;
    private final SocketChannel channel;
    private WsListener wsListener;
    private Map<String, String> pathVariables;

    public Context(SocketChannel channel) {
        this.channel = channel;
        HttpRequest tempRequest = null;
        HttpResponse tempResponse = null;
        try {
            // 使用 HTTP 解析器解析请求
            tempRequest = HttpParser.readAndParse(channel);
            tempResponse = new HttpResponse(channel, tempRequest.getHttpVersion());
            printRequestInfo(tempRequest, tempResponse);
        } catch (Exception e) {
            errPrint(e, tempResponse);
        }
        this.request = tempRequest;
        this.response = tempResponse;
        this.pathVariables = new HashMap<>();
    }
    
    /**
     * 设置路径变量（由路由匹配时设置）
     */
    public void setPathVariables(Map<String, String> variables) {
        if (variables != null) {
            this.pathVariables.putAll(variables);
        }
    }
    
    /**
     * 获取路径变量
     */
    public String getPathVariable(String name) {
        return pathVariables.get(name);
    }
    
    /**
     * 获取所有路径变量
     */
    public Map<String, String> getPathVariables() {
        return new HashMap<>(pathVariables);
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
     * 获取 URL 查询参数
     */
    public Map<String, Object> getParams() {
        return request != null ? request.getQueryParams() : null;
    }

    /**
     * 获取请求体参数
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
            Logger.info(Context.class, msg + " 请求参数 [queryParams=" + request.getQueryParams() + 
                ", bodyParams=" + request.getBodyParams() + "]");
        } else {
            Logger.info(Context.class, msg);
        }
    }

    /**
     * 发送响应（HttpResponse）
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
        Logger.error(Context.class, e);
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
            Logger.error(Context.class, e1);
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

        try (FileInputStream is = new FileInputStream(file)) {

            if (response != null) {
                response.contentType(fileType + ";charset=utf-8");
                byte[] fileData = readFully(is);
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
        }
    }
    
    /**
     * Java 8 兼容的读取输入流全部字节的方法
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO 异常
     */
    private byte[] readFully(FileInputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }
    
    // ==================== 重定向方法 ====================
    
    /**
     * 临时重定向（302）
     */
    public void redirect(String location) {
        try {
            if (response != null) {
                response.status(302).header("Location", location).send();
            } else {
                String result = "HTTP/1.1 302 Found\r\n" +
                    "Location: " + location + "\r\n" +
                    ContentTypes.withDefaultCharset(ContentTypes.TEXT_HTML) + "\r\n\r\n";
                channel.write(ByteBuffer.wrap(result.getBytes(StandardCharsets.UTF_8)));
                channel.close();
            }
        } catch (IOException e) {
            errPrint(e);
        }
    }
    
    /**
     * 永久重定向（301）
     */
    public void redirectPermanent(String location) {
        try {
            if (response != null) {
                response.status(301).header("Location", location).send();
            } else {
                String result = "HTTP/1.1 301 Moved Permanently\r\n" +
                    "Location: " + location + "\r\n" +
                    ContentTypes.withDefaultCharset(ContentTypes.TEXT_HTML) + "\r\n\r\n";
                channel.write(ByteBuffer.wrap(result.getBytes(StandardCharsets.UTF_8)));
                channel.close();
            }
        } catch (IOException e) {
            errPrint(e);
        }
    }
    
    // ==================== Cookie 操作方法 ====================
    
    /**
     * 设置 Cookie
     * @param name Cookie 名称
     * @param value Cookie 值
     */
    public void setCookie(String name, String value) {
        setCookie(name, value, -1);
    }
    
    /**
     * 设置 Cookie
     * @param name Cookie 名称
     * @param value Cookie 值
     * @param maxAge 最大存活时间（秒），-1 表示会话 Cookie
     */
    public void setCookie(String name, String value, int maxAge) {
        setCookie(name, value, maxAge, "/");
    }
    
    /**
     * 设置 Cookie
     * @param name Cookie 名称
     * @param value Cookie 值
     * @param maxAge 最大存活时间（秒），-1 表示会话 Cookie
     * @param path Cookie 路径
     */
    public void setCookie(String name, String value, int maxAge, String path) {
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(name).append("=").append(value);
        
        if (maxAge >= 0) {
            cookieHeader.append("; Max-Age=").append(maxAge);
            cookieHeader.append("; Expires=").append(getExpiresDate(maxAge));
        }
        
        if (path != null) {
            cookieHeader.append("; Path=").append(path);
        }
        
        response.header("Set-Cookie", cookieHeader.toString());
    }
    
    /**
     * 删除 Cookie
     */
    public void deleteCookie(String name) {
        setCookie(name, "", 0);
    }
    
    /**
     * 获取 Cookie
     */
    public String getCookie(String name) {
        String cookieHeader = getHeader("Cookie");
        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return null;
        }
        
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && parts[0].trim().equals(name)) {
                return parts[1].trim();
            }
        }
        return null;
    }
    
    /**
     * 计算过期日期字符串
     */
    private String getExpiresDate(int maxAgeSeconds) {
        long expiresTime = System.currentTimeMillis() + (maxAgeSeconds * 1000L);
        java.util.Date expiresDate = new java.util.Date(expiresTime);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        return sdf.format(expiresDate);
    }
    
    // ==================== Session 管理方法 ====================
    
    /**
     * 获取当前 Session，如果不存在则创建新的
     */
    public Map<String, Object> getSession() {
        return getSession(true);
    }
    
    /**
     * 获取当前 Session
     * @param create 如果为 true，当 Session 不存在时创建新的
     */
    public Map<String, Object> getSession(boolean create) {
        // 清理过期的 Session
        cleanupExpiredSessions();
        
        // 从 Cookie 获取 Session ID
        String sessionId = getCookie("JSESSIONID");
        
        // 如果 Session ID 存在且有效，返回对应的 Session
        if (sessionId != null && SESSION_STORE.containsKey(sessionId)) {
            Long expiry = SESSION_EXPIRY.get(sessionId);
            if (expiry != null && expiry > System.currentTimeMillis()) {
                // 更新过期时间
                SESSION_EXPIRY.put(sessionId, System.currentTimeMillis() + SESSION_TIMEOUT_MS);
                return SESSION_STORE.get(sessionId);
            } else {
                // Session 已过期，清理
                SESSION_STORE.remove(sessionId);
                SESSION_EXPIRY.remove(sessionId);
            }
        }
        
        // 如果需要创建新 Session
        if (create) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
            Map<String, Object> session = new ConcurrentHashMap<>();
            SESSION_STORE.put(sessionId, session);
            SESSION_EXPIRY.put(sessionId, System.currentTimeMillis() + SESSION_TIMEOUT_MS);
            
            // 设置 Session Cookie
            setCookie("JSESSIONID", sessionId, (int)(SESSION_TIMEOUT_MS / 1000), "/");
            
            return session;
        }
        
        return null;
    }
    
    /**
     * 获取 Session 中的属性
     */
    public Object getSessionAttribute(String name) {
        Map<String, Object> session = getSession(false);
        return session != null ? session.get(name) : null;
    }
    
    /**
     * 设置 Session 属性
     */
    public void setSessionAttribute(String name, Object value) {
        Map<String, Object> session = getSession();
        session.put(name, value);
    }
    
    /**
     * 移除 Session 属性
     */
    public void removeSessionAttribute(String name) {
        Map<String, Object> session = getSession(false);
        if (session != null) {
            session.remove(name);
        }
    }
    
    /**
     * 使当前 Session 失效
     */
    public void invalidateSession() {
        String sessionId = getCookie("JSESSIONID");
        if (sessionId != null) {
            SESSION_STORE.remove(sessionId);
            SESSION_EXPIRY.remove(sessionId);
        }
        deleteCookie("JSESSIONID");
    }
    
    /**
     * 清理过期的 Session
     */
    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        java.util.Iterator<Map.Entry<String, Long>> iterator = SESSION_EXPIRY.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < now) {
                iterator.remove();
                SESSION_STORE.remove(entry.getKey());
            }
        }
    }
}
