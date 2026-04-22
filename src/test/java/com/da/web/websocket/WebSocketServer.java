package com.da.web.websocket;

import com.da.web.annotations.Component;
import com.da.web.annotations.Get;
import com.da.web.annotations.Path;
import com.da.web.core.Context;
import com.da.web.core.DApp;
import com.da.web.function.WsListener;
import com.da.web.util.Logger;

/**
 * WebSocket 测试服务启动类
 * 
 * 使用说明：
 * 1. 运行此类启动服务器
 * 2. 使用浏览器或 wscat 工具连接 WebSocket
 * 3. 访问 http://localhost:8082/ws 页面进行 WebSocket 测试
 * 
 * 示例请求（使用 wscat）:
 * wscat -c ws://localhost:8082/ws/chat
 * 
 * 发送消息:
 * > {"action": "echo", "message": "Hello WebSocket!"}
 * 
 * 广播消息:
 * > {"action": "broadcast", "message": "Hello everyone!"}
 * 
 * 获取在线人数:
 * > {"action": "count"}
 */
public class WebSocketServer {
    
    public static void main(String[] args) {
        try {
            // 创建应用实例，自动扫描并注册路由和组件
            DApp app = new DApp(WebSocketServer.class);
            
            // 启动服务器
            Logger.info(WebSocketServer.class, "正在启动 WebSocket 测试服务...");
            app.listen(8082);
            
            Logger.info(WebSocketServer.class, "WebSocket 测试服务已启动!");
            Logger.info(WebSocketServer.class, "端点:");
            Logger.info(WebSocketServer.class, "  GET  /ws           - WebSocket 测试页面");
            Logger.info(WebSocketServer.class, "  WS   /ws/chat     - WebSocket 聊天端点");
            Logger.info(WebSocketServer.class, "");
            Logger.info(WebSocketServer.class, "使用 wscat 测试:");
            Logger.info(WebSocketServer.class, "  wscat -c ws://localhost:8082/ws/chat");
            Logger.info(WebSocketServer.class, "");
            Logger.info(WebSocketServer.class, "发送消息示例:");
            Logger.info(WebSocketServer.class, "  {\"action\": \"echo\", \"message\": \"Hello!\"}");
            Logger.info(WebSocketServer.class, "  {\"action\": \"broadcast\", \"message\": \"Hello everyone!\"}");
            Logger.info(WebSocketServer.class, "  {\"action\": \"count\"}");
            
        } catch (Exception e) {
            Logger.error(WebSocketServer.class, "启动失败", e);
        }
    }
    
    /**
     * WebSocket 聊天处理器
     */
    @Component("/ws/chat")
    public static class ChatHandler implements WsListener {
        
        @Override
        public void onOpen(Context ctx) throws Exception {
            Logger.info(ChatHandler.class, "新 WebSocket 连接建立：" + ctx.getChannel());
            String welcomeMsg = "{\"type\": \"welcome\", \"message\": \"欢迎加入聊天室!\", \"onlineCount\": " + 
                WebSocketManager.getConnectionCount() + "}";
            WebSocketManager.sendMessage(ctx.getChannel(), welcomeMsg);
        }
        
        @Override
        public void onMessage(Context ctx, String message) throws Exception {
            Logger.info(ChatHandler.class, "收到消息：" + message);
            
            // 简单的消息解析和处理
            if (message.contains("\"action\": \"echo\"")) {
                // 回显消息
                String response = "{\"type\": \"echo\", \"message\": \"Echo: " + extractMessage(message) + "\"}";
                WebSocketManager.sendMessage(ctx.getChannel(), response);
            } else if (message.contains("\"action\": \"broadcast\"")) {
                // 广播消息
                String msg = extractMessage(message);
                String broadcastMsg = "{\"type\": \"broadcast\", \"from\": \"" + ctx.getChannel() + "\", \"message\": \"" + msg + "\"}";
                WebSocketManager.broadcast(broadcastMsg);
            } else if (message.contains("\"action\": \"count\"")) {
                // 返回在线人数
                int count = WebSocketManager.getConnectionCount();
                String response = "{\"type\": \"count\", \"onlineCount\": " + count + "}";
                WebSocketManager.sendMessage(ctx.getChannel(), response);
            } else {
                // 默认回显
                String response = "{\"type\": \"message\", \"received\": \"" + escapeJson(message) + "\"}";
                WebSocketManager.sendMessage(ctx.getChannel(), response);
            }
        }
        
        @Override
        public void onError(Context ctx, Exception e) {
            Logger.error(ChatHandler.class, "WebSocket 错误：" + ctx.getChannel(), e);
        }
        
        @Override
        public void onClose(Context ctx) throws Exception {
            Logger.info(ChatHandler.class, "WebSocket 连接关闭：" + ctx.getChannel());
            String leaveMsg = "{\"type\": \"leave\", \"message\": \"用户已离开\", \"onlineCount\": " + 
                WebSocketManager.getConnectionCount() + "}";
            WebSocketManager.broadcast(leaveMsg);
        }
        
        private String extractMessage(String json) {
            int start = json.indexOf("\"message\"");
            if (start == -1) return "";
            int colon = json.indexOf(":", start);
            if (colon == -1) return "";
            int quoteStart = json.indexOf("\"", colon + 1);
            if (quoteStart == -1) return "";
            int quoteEnd = json.indexOf("\"", quoteStart + 1);
            if (quoteEnd == -1) return "";
            return json.substring(quoteStart + 1, quoteEnd);
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
    
    /**
     * WebSocket 测试页面控制器
     */
    @Path("/ws")
    @Component
    public static class PageController {
        
        @Get("")
        public void index(Context ctx) {
            String html = "<!DOCTYPE html>" +
                "<html><head><title>WebSocket 测试</title></head>" +
                "<body>" +
                "<h1>WebSocket 测试页面</h1>" +
                "<div>" +
                "<button onclick=\"connect()\">连接</button>" +
                "<button onclick=\"disconnect()\">断开</button>" +
                "<button onclick=\"sendEcho()\">发送 Echo</button>" +
                "<button onclick=\"sendBroadcast()\">发送广播</button>" +
                "<button onclick=\"sendCount()\">查询人数</button>" +
                "</div>" +
                "<textarea id=\"output\" rows=\"20\" cols=\"80\"></textarea>" +
                "<script>" +
                "var ws;" +
                "function connect() {" +
                "  ws = new WebSocket('ws://' + location.host + '/ws/chat');" +
                "  ws.onopen = function(e) { log('连接成功'); };" +
                "  ws.onclose = function(e) { log('连接关闭'); };" +
                "  ws.onerror = function(e) { log('错误：' + JSON.stringify(e)); };" +
                "  ws.onmessage = function(e) { log('收到：' + e.data); };" +
                "}" +
                "function disconnect() { if(ws) ws.close(); }" +
                "function sendEcho() { if(ws) ws.send(JSON.stringify({action:'echo',message:'Hello WebSocket!'})); }" +
                "function sendBroadcast() { if(ws) ws.send(JSON.stringify({action:'broadcast',message:'大家好!'})); }" +
                "function sendCount() { if(ws) ws.send(JSON.stringify({action:'count'})); }" +
                "function log(msg) { document.getElementById('output').value += msg + '\\n'; }" +
                "</script>" +
                "</body></html>";
            ctx.sendHtml(html);
        }
    }
}
