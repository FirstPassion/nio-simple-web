package com.da.web.websocket;

import com.da.web.core.DApp;
import com.da.web.sse.SseServer;
import com.da.web.util.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 和 SSE 功能测试类
 * 
 * 使用说明：
 * 1. 运行测试会自动启动 WebSocketServer 和 SseServer
 * 2. 使用 curl 命令测试各个端点
 * 3. 测试结束后自动关闭服务器
 */
public class WebSocketSseTest {
    
    private static Thread wsServerThread;
    private static Thread sseServerThread;
    private static volatile boolean serverRunning = true;
    
    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("\n========== 启动 WebSocket 和 SSE 测试服务器 ==========");
        
        // 启动 WebSocket 服务器
        wsServerThread = new Thread(() -> {
            try {
                DApp wsApp = new DApp(WebSocketServer.class);
                Logger.info(WebSocketSseTest.class, "WebSocket 服务器启动在端口 8082");
                wsApp.listen(8082);
            } catch (Exception e) {
                if (serverRunning) {
                    Logger.error(WebSocketSseTest.class, "WebSocket 服务器启动失败", e);
                }
            }
        });
        wsServerThread.setDaemon(true);
        wsServerThread.start();
        
        // 启动 SSE 服务器
        sseServerThread = new Thread(() -> {
            try {
                DApp sseApp = new DApp(SseServer.class);
                Logger.info(WebSocketSseTest.class, "SSE 服务器启动在端口 8083");
                sseApp.listen(8083);
            } catch (Exception e) {
                if (serverRunning) {
                    Logger.error(WebSocketSseTest.class, "SSE 服务器启动失败", e);
                }
            }
        });
        sseServerThread.setDaemon(true);
        sseServerThread.start();
        
        // 等待服务器启动
        TimeUnit.SECONDS.sleep(2);
        System.out.println("服务器启动完成，等待 2 秒...");
    }
    
    @AfterClass
    public static void tearDown() {
        System.out.println("\n========== 关闭测试服务器 ==========");
        serverRunning = false;
        if (wsServerThread != null) {
            wsServerThread.interrupt();
        }
        if (sseServerThread != null) {
            sseServerThread.interrupt();
        }
        System.out.println("测试服务器已关闭");
    }
    
    /**
     * 测试 WebSocket 页面是否正常返回
     */
    @Test
    public void testWebSocketPage() throws Exception {
        System.out.println("\n===== 测试 WebSocket 页面 =====");
        URL url = new URL("http://localhost:8082/ws");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        System.out.println("WebSocket 页面响应码：" + responseCode);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        System.out.println("WebSocket 页面内容长度：" + response.length());
        assert responseCode == 200 : "WebSocket 页面应该返回 200";
        assert response.toString().contains("WebSocket 测试页面") : "页面应该包含标题";
        System.out.println("✓ WebSocket 页面测试通过");
    }
    
    /**
     * 测试 SSE 页面是否正常返回
     */
    @Test
    public void testSsePage() throws Exception {
        System.out.println("\n===== 测试 SSE 页面 =====");
        URL url = new URL("http://localhost:8083/sse");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        System.out.println("SSE 页面响应码：" + responseCode);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        System.out.println("SSE 页面内容长度：" + response.length());
        assert responseCode == 200 : "SSE 页面应该返回 200";
        assert response.toString().contains("SSE 测试页面") : "页面应该包含标题";
        System.out.println("✓ SSE 页面测试通过");
    }
    
    /**
     * 测试 SSE 连接数接口
     */
    @Test
    public void testSseCount() throws Exception {
        System.out.println("\n===== 测试 SSE 连接数接口 =====");
        URL url = new URL("http://localhost:8083/sse/count");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        System.out.println("SSE 连接数接口响应码：" + responseCode);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        System.out.println("SSE 连接数响应：" + response.toString());
        assert responseCode == 200 : "SSE 连接数接口应该返回 200";
        assert response.toString().contains("activeConnections") : "响应应该包含 activeConnections 字段";
        System.out.println("✓ SSE 连接数接口测试通过");
    }
    
    /**
     * 测试 SSE 流式数据（短时间连接）
     */
    @Test
    public void testSseStream() throws Exception {
        System.out.println("\n===== 测试 SSE 流式数据 =====");
        URL url = new URL("http://localhost:8083/sse/stream");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(8000); // 给更多时间接收数据
        
        int responseCode = conn.getResponseCode();
        System.out.println("SSE 流式数据响应码：" + responseCode);
        System.out.println("Content-Type: " + conn.getContentType());
        
        // 读取前几行数据
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        int lineCount = 0;
        String inputLine;
        while ((inputLine = in.readLine()) != null && lineCount < 5) {
            System.out.println("收到 SSE 数据：" + inputLine);
            lineCount++;
        }
        in.close();
        
        assert responseCode == 200 : "SSE 流式数据应该返回 200";
        assert lineCount > 0 : "应该至少收到一行数据";
        System.out.println("✓ SSE 流式数据测试通过（收到 " + lineCount + " 行数据）");
    }
    
    /**
     * 打印测试说明
     */
    @Test
    public void testPrintInstructions() {
        System.out.println("\n========== WebSocket 和 SSE 手动测试指南 ==========");
        System.out.println("\n【WebSocket 测试】");
        System.out.println("1. 访问页面：http://localhost:8082/ws");
        System.out.println("2. 使用 wscat 工具：wscat -c ws://localhost:8082/ws/chat");
        System.out.println("3. 发送消息示例：");
        System.out.println("   - 回显：{\"action\": \"echo\", \"message\": \"Hello!\"}");
        System.out.println("   - 广播：{\"action\": \"broadcast\", \"message\": \"大家好!\"}");
        System.out.println("   - 查询人数：{\"action\": \"count\"}");
        
        System.out.println("\n【SSE 测试】");
        System.out.println("1. 访问页面：http://localhost:8083/sse");
        System.out.println("2. 使用 curl 测试流式数据：curl -N http://localhost:8083/sse/stream");
        System.out.println("3. 使用 curl 测试时间推送：curl -N http://localhost:8083/sse/time");
        System.out.println("4. 查询连接数：curl http://localhost:8083/sse/count");
        System.out.println("5. 广播消息：curl \"http://localhost:8083/sse/broadcast?msg=Hello\"");
        
        System.out.println("\n========== 测试说明结束 ==========\n");
    }
}
