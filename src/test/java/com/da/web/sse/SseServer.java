package com.da.web.sse;

import com.da.web.annotations.Component;
import com.da.web.annotations.Get;
import com.da.web.annotations.Path;
import com.da.web.core.Context;
import com.da.web.core.DApp;
import com.da.web.http.HttpRequest;
import com.da.web.util.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.*;

/**
 * SSE (Server-Sent Events) 测试服务启动类
 * 
 * 使用说明：
 * 1. 运行此类启动服务器
 * 2. 访问 http://localhost:8083/sse 页面进行 SSE 测试
 * 3. 使用 curl 测试 SSE 端点
 * 
 * 示例请求（使用 curl）:
 * curl -N http://localhost:8083/sse/stream
 * 
 * 示例请求（获取时间流）:
 * curl -N http://localhost:8083/sse/time
 */
public class SseServer {
    
    public static void main(String[] args) {
        try {
            // 创建应用实例，自动扫描并注册路由和组件
            DApp app = new DApp(SseServer.class);
            
            // 启动服务器
            Logger.info(SseServer.class, "正在启动 SSE 测试服务...");
            app.listen(8083);
            
            Logger.info(SseServer.class, "SSE 测试服务已启动!");
            Logger.info(SseServer.class, "端点:");
            Logger.info(SseServer.class, "  GET  /sse           - SSE 测试页面");
            Logger.info(SseServer.class, "  GET  /sse/stream    - SSE 流式数据端点");
            Logger.info(SseServer.class, "  GET  /sse/time      - SSE 时间推送端点");
            Logger.info(SseServer.class, "");
            Logger.info(SseServer.class, "使用 curl 测试:");
            Logger.info(SseServer.class, "  curl -N http://localhost:8083/sse/stream");
            Logger.info(SseServer.class, "  curl -N http://localhost:8083/sse/time");
            
        } catch (Exception e) {
            Logger.error(SseServer.class, "启动失败", e);
        }
    }
    
    /**
     * SSE 流式数据处理器
     */
    @Path("/sse")
    @Component
    public static class SseController {
        
        private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
        private static final ConcurrentMap<String, SocketChannel> activeClients = new ConcurrentHashMap<>();
        
        /**
         * SSE 测试页面
         */
        @Get("")
        public void index(Context ctx) {
            String html = "<!DOCTYPE html>" +
                "<html><head><title>SSE 测试</title></head>" +
                "<body>" +
                "<h1>SSE (Server-Sent Events) 测试页面</h1>" +
                "<div>" +
                "<button onclick=\"connectStream()\">连接流</button>" +
                "<button onclick=\"disconnect()\">断开</button>" +
                "</div>" +
                "<h3>接收到的消息:</h3>" +
                "<textarea id=\"output\" rows=\"20\" cols=\"80\"></textarea>" +
                "<script>" +
                "var eventSource;" +
                "function connectStream() {" +
                "  eventSource = new EventSource('/sse/stream');" +
                "  eventSource.onopen = function(e) { log('连接成功'); };" +
                "  eventSource.onerror = function(e) { log('错误'); };" +
                "  eventSource.addEventListener('message', function(e) { log('收到：' + e.data); });" +
                "  eventSource.addEventListener('time', function(e) { log('时间：' + e.data); });" +
                "}" +
                "function disconnect() { if(eventSource) eventSource.close(); }" +
                "function log(msg) { document.getElementById('output').value += msg + '\\n'; }" +
                "</script>" +
                "</body></html>";
            ctx.sendHtml(html);
        }
        
        /**
         * SSE 流式数据端点
         * 每秒推送一条消息
         */
        @Get("/stream")
        public void stream(Context ctx) throws Exception {
            // 发送 SSE 响应头
            ctx.getResponse().sendSSEHeaders();
            
            SocketChannel channel = ctx.getChannel();
            String clientId = channel != null ? channel.toString() : "unknown";
            
            // 注册连接
            SSEManager.registerConnection(channel, clientId);
            activeClients.put(clientId, channel);
            
            Logger.info(SseController.class, "新 SSE 连接：" + clientId + ", 当前连接数：" + SSEManager.getConnectionCount());
            
            // 发送欢迎消息
            SSEManager.sendMessage(channel, "{\"type\": \"welcome\", \"message\": \"欢迎订阅 SSE 流!\"}");
            
            // 定时推送消息
            ScheduledFuture<?>[] futureRef = new ScheduledFuture<?>[1];
            futureRef[0] = scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (!channel.isOpen()) {
                        if (futureRef[0] != null) {
                            futureRef[0].cancel(true);
                        }
                        return;
                    }
                    
                    long timestamp = System.currentTimeMillis();
                    String data = "{\"type\": \"data\", \"timestamp\": " + timestamp + ", \"message\": \"这是第" + (timestamp % 1000) + "条消息\"}";
                    SSEManager.sendMessage(channel, data);
                    
                    // 每 5 秒发送一次心跳
                    if (timestamp % 5000 < 1000) {
                        SSEManager.sendKeepAlive(channel);
                    }
                } catch (Exception e) {
                    Logger.error(SseController.class, "推送消息失败", e);
                    if (futureRef[0] != null) {
                        futureRef[0].cancel(true);
                    }
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
            
            // 注意：这里不能关闭响应，需要保持长连接
            // 连接会在客户端断开或服务器关闭时自动清理
        }
        
        /**
         * SSE 时间推送端点
         * 推送带事件类型的时间信息
         */
        @Get("/time")
        public void timeStream(Context ctx) throws Exception {
            // 发送 SSE 响应头
            ctx.getResponse().sendSSEHeaders();
            
            SocketChannel channel = ctx.getChannel();
            String clientId = channel != null ? channel.toString() : "unknown";
            
            // 注册连接
            SSEManager.registerConnection(channel, clientId);
            activeClients.put(clientId, channel);
            
            Logger.info(SseController.class, "新时间订阅：" + clientId);
            
            // 发送欢迎消息
            SSEManager.sendEvent(channel, "welcome", "{\"message\": \"欢迎订阅时间推送!\"}");
            
            // 定时推送时间
            ScheduledFuture<?>[] futureRef = new ScheduledFuture<?>[1];
            futureRef[0] = scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (!channel.isOpen()) {
                        if (futureRef[0] != null) {
                            futureRef[0].cancel(true);
                        }
                        return;
                    }
                    
                    String now = java.time.LocalDateTime.now().toString();
                    String data = "{\"time\": \"" + now + "\", \"timestamp\": " + System.currentTimeMillis() + "}";
                    SSEManager.sendEvent(channel, "time", data);
                } catch (Exception e) {
                    Logger.error(SseController.class, "推送时间失败", e);
                    if (futureRef[0] != null) {
                        futureRef[0].cancel(true);
                    }
                }
            }, 0, 2000, TimeUnit.MILLISECONDS);
        }
        
        /**
         * 广播消息端点（用于测试）
         */
        @Get("/broadcast")
        public void broadcast(Context ctx) {
            HttpRequest request = ctx.getRequest();
            String message = request != null && request.getQueryParams() != null && request.getQueryParams().containsKey("msg") 
                ? request.getQueryParams().get("msg").toString() 
                : "Hello from server!";
            if (message == null || message.isEmpty()) {
                message = "Hello from server!";
            }
            
            SSEManager.broadcast("{\"type\": \"broadcast\", \"message\": \"" + escapeJson(message) + "\"}");
            
            int count = SSEManager.getConnectionCount();
            ctx.sendJson("{\"success\": true, \"message\": \"已广播给 " + count + " 个客户端\", \"count\": " + count + "}");
        }
        
        /**
         * 获取连接数
         */
        @Get("/count")
        public void count(Context ctx) {
            int count = SSEManager.getConnectionCount();
            ctx.sendJson("{\"activeConnections\": " + count + "}");
        }
        
        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "\\r")
                      .replace("\t", "\\t");
        }
    }
}
