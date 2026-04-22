package com.da.web.sse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSE (Server-Sent Events) 管理器
 * 用于管理 SSE 长连接和流式消息发送
 */
public class SSEManager {
    
    // 存储活跃的 SSE 连接
    private static final Map<SocketChannel, SSEConnection> connections = new ConcurrentHashMap<>();
    
    // 线程池用于异步发送消息
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * 注册一个新的 SSE 连接
     */
    public static void registerConnection(SocketChannel channel, String clientId) {
        SSEConnection connection = new SSEConnection(channel, clientId);
        connections.put(channel, connection);
    }
    
    /**
     * 移除一个 SSE 连接
     */
    public static void removeConnection(SocketChannel channel) {
        SSEConnection connection = connections.remove(channel);
        if (connection != null) {
            connection.close();
        }
    }
    
    /**
     * 获取连接数量
     */
    public static int getConnectionCount() {
        return connections.size();
    }
    
    /**
     * 向指定连接发送 SSE 消息
     */
    public static void sendMessage(SocketChannel channel, String data) {
        SSEConnection connection = connections.get(channel);
        if (connection != null) {
            connection.send(data);
        }
    }
    
    /**
     * 向指定连接发送带事件类型的 SSE 消息
     */
    public static void sendEvent(SocketChannel channel, String event, String data) {
        SSEConnection connection = connections.get(channel);
        if (connection != null) {
            connection.sendEvent(event, data);
        }
    }
    
    /**
     * 广播消息给所有连接
     */
    public static void broadcast(String data) {
        connections.forEach((channel, conn) -> conn.send(data));
    }
    
    /**
     * 广播带事件类型的消息给所有连接
     */
    public static void broadcastEvent(String event, String data) {
        connections.forEach((channel, conn) -> conn.sendEvent(event, data));
    }
    
    /**
     * 发送心跳保持连接活跃
     */
    public static void sendKeepAlive(SocketChannel channel) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(": keep-alive\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            channel.write(buffer);
        } catch (IOException e) {
            removeConnection(channel);
        }
    }
    
    /**
     * SSE 连接内部类
     */
    public static class SSEConnection {
        private final SocketChannel channel;
        private final String clientId;
        private volatile boolean active = true;
        
        public SSEConnection(SocketChannel channel, String clientId) {
            this.channel = channel;
            this.clientId = clientId;
        }
        
        public String getClientId() {
            return clientId;
        }
        
        public boolean isActive() {
            return active;
        }
        
        /**
         * 发送 SSE 数据
         */
        public void send(String data) {
            if (!active) return;
            
            executor.submit(() -> {
                try {
                    String message = "data: " + data + "\r\n\r\n";
                    ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
                    channel.write(buffer);
                } catch (IOException e) {
                    active = false;
                    removeConnection(channel);
                }
            });
        }
        
        /**
         * 发送带事件类型的 SSE 消息
         */
        public void sendEvent(String event, String data) {
            if (!active) return;
            
            executor.submit(() -> {
                try {
                    String sb = "event: " + event + "\r\n" +
                            "data: " + data + "\r\n\r\n";
                    ByteBuffer buffer = ByteBuffer.wrap(sb.getBytes(StandardCharsets.UTF_8));
                    channel.write(buffer);
                } catch (IOException e) {
                    active = false;
                    removeConnection(channel);
                }
            });
        }
        
        /**
         * 关闭连接
         */
        public void close() {
            active = false;
            try {
                channel.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
