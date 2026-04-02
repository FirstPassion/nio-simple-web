package com.da.web.websocket;

import com.da.web.core.Context;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 连接管理器
 */
public class WebSocketManager {
    
    private static final Map<java.nio.channels.SocketChannel, Context> connections = new ConcurrentHashMap<>();
    
    /**
     * 添加 WebSocket 连接
     */
    public static void addConnection(java.nio.channels.SocketChannel channel, Context context) {
        connections.put(channel, context);
    }
    
    /**
     * 移除 WebSocket 连接
     */
    public static void removeConnection(java.nio.channels.SocketChannel channel) {
        connections.remove(channel);
    }
    
    /**
     * 获取 WebSocket 连接上下文
     */
    public static Context getContext(java.nio.channels.SocketChannel channel) {
        return connections.get(channel);
    }
    
    /**
     * 检查连接是否存在
     */
    public static boolean hasConnection(java.nio.channels.SocketChannel channel) {
        return connections.containsKey(channel);
    }
    
    /**
     * 获取连接数量
     */
    public static int getConnectionCount() {
        return connections.size();
    }
}
