package com.da.web.core;

import com.da.web.constant.ContentTypes;
import com.da.web.function.WsListener;
import com.da.web.util.Utils;
import com.da.web.websocket.WebSocketManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求上下文类
 * <p>
 * 封装请求信息和响应方法
 */
public class Context {
    
    private String url;
    private String method;
    private final Map<String, Object> params = new HashMap<>();
    private String httpVersion = "HTTP/1.1";
    private final SocketChannel channel;
    private WsListener wsListener;

    public Context(SocketChannel channel) {
        this.channel = channel;
        handleRequest();
        printRequestInfo();
    }

    public void setWsListener(WsListener wsListener) {
        this.wsListener = wsListener;
    }

    public WsListener getWsListener() {
        return wsListener;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    private void handleRequest() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            StringBuilder requestMsg = new StringBuilder();
            
            while (channel.read(buffer) > 0) {
                buffer.flip();
                byte[] buff = new byte[buffer.limit()];
                buffer.get(buff);
                requestMsg.append(new String(buff));
            }
            
            if (Utils.isNotBlank(requestMsg.toString())) {
                String[] messages = requestMsg.toString().split("\n");
                
                if (isWebSocketRequest(messages)) {
                    handleWebSocketHandshake(messages);
                    return;
                }
                
                parseHttpRequest(messages);
            }
        } catch (Exception e) {
            errPrint(e);
        }
    }

    private boolean isWebSocketRequest(String[] messages) {
        return messages.length > 3 && messages[messages.length - 3].contains("Sec-WebSocket-Key");
    }

    private void handleWebSocketHandshake(String[] messages) throws IOException {
        String secWebSocketKey = messages[messages.length - 3];
        secWebSocketKey = secWebSocketKey.substring(secWebSocketKey.indexOf(":") + 1).trim();
        String response = Utils.getHandShakeResponse(secWebSocketKey);
        channel.write(ByteBuffer.wrap(response.getBytes()));
        WebSocketManager.addConnection(channel, this);
    }

    private void parseHttpRequest(String[] messages) {
        if (messages.length == 0) return;
        
        String[] info = messages[0].split(" ");
        if (info.length != 3) return;
        
        this.method = info[0];
        
        if ("POST".equals(this.method)) {
            handlePostBody(messages);
        }
        
        parseUrl(info[1]);
        this.httpVersion = info[2].trim();
    }

    private void handlePostBody(String[] messages) {
        String jsonMsg = messages[messages.length - 1];
        if (!"}]".equals(jsonMsg.trim())) {
            params.put("request-json-data", jsonMsg.trim());
        }
    }

    private void parseUrl(String beforeUrl) {
        if (beforeUrl.contains("?")) {
            this.url = beforeUrl.substring(0, beforeUrl.indexOf("?"));
            parseParams(beforeUrl.substring(beforeUrl.indexOf("?") + 1));
        } else {
            this.url = beforeUrl;
        }
    }

    private void parseParams(String paramsStr) {
        if (!paramsStr.contains("=")) return;
        
        if (paramsStr.contains("&")) {
            for (String param : paramsStr.split("&")) {
                parseParamToMap(param);
            }
        } else {
            parseParamToMap(paramsStr);
        }
    }

    private void parseParamToMap(String param) {
        try {
            if (param.contains("=")) {
                String[] resParams = param.split("=", 2);
                if (resParams.length == 2) {
                    String decoded = URLDecoder.decode(resParams[1], "utf-8");
                    params.put(resParams[0], decoded);
                }
            }
        } catch (UnsupportedEncodingException e) {
            errPrint(e);
        }
    }

    private void printRequestInfo() {
        if (this.url == null) return;
        
        String msg = "请求方式 [" + this.method + "] 请求路径 [" + this.url + "]";
        if (!params.isEmpty()) {
            System.out.println(msg + " 请求参数 [" + this.params + "]");
        } else {
            System.out.println(msg);
        }
    }

    /**
     * 发送响应
     */
    public void send(String headers, int code, String data) {
        try {
            String result = httpVersion + " " + code + "\n" + headers + "\n\n" + data;
            channel.write(ByteBuffer.wrap(result.getBytes(StandardCharsets.UTF_8)));
            channel.close();
        } catch (Exception e) {
            errPrint(e);
        }
    }

    /**
     * 发送错误信息
     */
    public void errPrint(Exception e) {
        e.printStackTrace();
        String result = httpVersion + " 500\n" + 
            ContentTypes.withDefaultCharset(ContentTypes.TEXT_HTML) + "\n\n" +
            "<head><title>500</title></head><body>" +
            "<h1 style='text-align: center;color: red;'>服务器出错了</h1><hr/>" +
            "<p>错误信息：" + e.getMessage() + "</p></body></html>";
        
        try {
            channel.write(ByteBuffer.wrap(result.getBytes(StandardCharsets.UTF_8)));
            channel.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void errPrint(String msg) {
        errPrint(new Exception(msg));
    }

    /**
     * 发送文本
     */
    public void send(String msg) {
        send(ContentTypes.withDefaultCharset(ContentTypes.TEXT_PLAIN), 200, msg);
    }

    public void send(String msg, int code) {
        send(ContentTypes.withDefaultCharset(ContentTypes.TEXT_PLAIN), code, msg);
    }

    /**
     * 发送 HTML
     */
    public void sendHtml(String msg) {
        send(ContentTypes.withDefaultCharset(ContentTypes.TEXT_HTML), 200, msg);
    }

    public void sendHtml(String msg, int code) {
        send(ContentTypes.withDefaultCharset(ContentTypes.TEXT_HTML), code, msg);
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
        send(ContentTypes.withDefaultCharset(ContentTypes.APPLICATION_JSON), 200, msg);
    }

    public void sendJson(String msg, int code) {
        send(ContentTypes.withDefaultCharset(ContentTypes.APPLICATION_JSON), code, msg);
    }

    /**
     * 发送文件
     */
    public void send(File file) {
        String fileType = Utils.getFileType(file);
        FileInputStream is = null;
        
        try {
            is = new FileInputStream(file);
            String header = httpVersion + " 200\nContent-Type: " + fileType + ";charset=utf-8\n\n";
            channel.write(ByteBuffer.wrap(header.getBytes(StandardCharsets.UTF_8)));
            
            FileChannel fc = is.getChannel();
            fc.transferTo(0, fc.size(), channel);
            fc.close();
            channel.close();
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
