package com.da.web.websocket;

import com.da.web.core.Context;
import com.da.web.function.WsListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 连接管理器
 * <p>
 * 负责管理所有 WebSocket 连接，提供连接管理、消息发送等功能
 */
public class WebSocketManager {
    
    private static final Map<SocketChannel, Context> connections = new ConcurrentHashMap<>();
    private static final Set<SocketChannel> activeChannels = new CopyOnWriteArraySet<>();
    
    /**
     * 添加 WebSocket 连接
     *
     * @param channel Socket 通道
     * @param context 上下文对象
     */
    public static void addConnection(SocketChannel channel, Context context) {
        connections.put(channel, context);
        activeChannels.add(channel);
        
        // 触发 onOpen 事件
        WsListener listener = context.getWsListener();
        if (listener != null) {
            try {
                listener.onOpen(context);
            } catch (Exception e) {
                context.errPrint(e);
            }
        }
    }
    
    /**
     * 移除 WebSocket 连接
     *
     * @param channel Socket 通道
     */
    public static void removeConnection(SocketChannel channel) {
        Context context = connections.remove(channel);
        activeChannels.remove(channel);
        
        // 触发 onClose 事件
        if (context != null && context.getWsListener() != null) {
            try {
                context.getWsListener().onClose(context);
            } catch (Exception e) {
                context.errPrint(e);
            }
        }
        
        // 关闭通道
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            if (context != null) {
                context.errPrint(e);
            }
        }
    }
    
    /**
     * 获取 WebSocket 连接上下文
     *
     * @param channel Socket 通道
     * @return 上下文对象，不存在则返回 null
     */
    public static Context getContext(SocketChannel channel) {
        return connections.get(channel);
    }
    
    /**
     * 检查连接是否存在
     *
     * @param channel Socket 通道
     * @return true 如果连接存在
     */
    public static boolean hasConnection(SocketChannel channel) {
        return connections.containsKey(channel);
    }
    
    /**
     * 获取连接数量
     *
     * @return 当前活跃连接数
     */
    public static int getConnectionCount() {
        return connections.size();
    }
    
    /**
     * 获取所有活跃的 SocketChannel
     *
     * @return 活跃通道集合
     */
    public static Set<SocketChannel> getActiveChannels() {
        return activeChannels;
    }
    
    /**
     * 向指定连接发送消息
     *
     * @param channel Socket 通道
     * @param message 消息内容
     * @throws IOException 发送异常
     */
    public static void sendMessage(SocketChannel channel, String message) throws IOException {
        if (channel == null || !channel.isOpen()) {
            return;
        }
        
        byte[] messageBytes = message.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length + 2);
        
        // WebSocket 文本帧：0x81 表示 FIN + 文本帧，长度
        buffer.put((byte) 0x81);
        
        if (messageBytes.length <= 125) {
            buffer.put((byte) messageBytes.length);
        } else if (messageBytes.length <= 65535) {
            buffer.put((byte) 126);
            buffer.putShort((short) messageBytes.length);
        } else {
            buffer.put((byte) 127);
            buffer.putLong(messageBytes.length);
        }
        
        buffer.put(messageBytes);
        buffer.flip();
        
        channel.write(buffer);
    }
    
    /**
     * 广播消息给所有连接的客户端
     *
     * @param message 消息内容
     */
    public static void broadcast(String message) {
        for (SocketChannel channel : activeChannels) {
            try {
                sendMessage(channel, message);
            } catch (IOException e) {
                Context ctx = getContext(channel);
                if (ctx != null) {
                    ctx.errPrint(e);
                }
                // 移除失效的连接
                removeConnection(channel);
            }
        }
    }
    
    /**
     * 关闭所有 WebSocket 连接
     */
    public static void closeAll() {
        for (SocketChannel channel : activeChannels) {
            removeConnection(channel);
        }
        connections.clear();
        activeChannels.clear();
    }
}
