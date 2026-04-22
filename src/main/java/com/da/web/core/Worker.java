package com.da.web.core;

import com.da.web.function.WsListener;
import com.da.web.http.HttpRequest;
import com.da.web.util.Logger;
import com.da.web.util.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 工作线程，处理路由事件和 WebSocket 消息
 */
public class Worker implements Runnable {
    
    private volatile Selector selector;
    private final String name;
    private volatile boolean started = false;
    private ConcurrentLinkedQueue<Runnable> queue;
    
    // 依赖组件：使用 DispatcherServlet 统一处理请求
    private final DispatcherServlet dispatcherServlet;
    
    public Worker(String name, DispatcherServlet dispatcherServlet) {
        this.name = name;
        this.dispatcherServlet = dispatcherServlet;
    }
    
    /**
     * 注册读取事件
     */
    public void register(final SocketChannel socket) throws IOException {
        if (!started) {
            Thread thread = new Thread(this, name);
            selector = Selector.open();
            queue = new ConcurrentLinkedQueue<>();
            this.started = true;
            thread.start();
        }
        
        queue.add(() -> {
            try {
                socket.register(selector, SelectionKey.OP_READ);
            } catch (ClosedChannelException e) {
                Logger.error(Worker.class, e);
            }
        });
        
        selector.wakeup();
    }
    
    /**
     * 获取并且执行队列中的任务
     */
    private void getAndStartTask() {
        Runnable task = queue.poll();
        if (task != null) {
            task.run();
        }
    }
    
    @Override
    public void run() {
        while (this.started) {
            try {
                selector.select();
                getAndStartTask();
                
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    
                    if (key.isReadable()) {
                        SocketChannel channel = (SocketChannel) key.channel();
                        
                        // 检查是否为 WebSocket 连接
                        if (DApp.wsContexts.containsKey(channel)) {
                            handleWebSocketMessage(channel);
                        } else {
                            // 普通 HTTP 请求
                            handleHttpRequest(channel);
                        }
                    }
                    
                    iterator.remove();
                }
            } catch (Exception e) {
                Logger.error(Worker.class, e);
            }
        }
    }
    
    /**
     * 处理 WebSocket 消息
     */
    private void handleWebSocketMessage(SocketChannel channel) throws Exception {
        Context context = DApp.wsContexts.get(channel);
        WsListener wsListener = context.getWsListener();
        
        if (wsListener != null) {
            byte[] bytesData = new byte[1024 * 8];
            channel.read(ByteBuffer.wrap(bytesData));
            
            // opcode 为 8，对方主动断开连接
            if ((bytesData[0] & 0xf) == 8) {
                wsListener.onError(context, null);
            }
            
            byte payloadLength = (byte) (bytesData[1] & 0x7f);
            byte[] mask = Arrays.copyOfRange(bytesData, 2, 6);
            byte[] payloadData = Arrays.copyOfRange(bytesData, 6, 6 + payloadLength);
            
            for (int i = 0; i < payloadData.length; i++) {
                payloadData[i] = (byte) (payloadData[i] ^ mask[i % 4]);
            }
            
            wsListener.onMessage(context, new String(payloadData));
        }
    }
    
    /**
     * 处理 HTTP 请求
     */
    private void handleHttpRequest(SocketChannel channel) throws Exception {
        // 创建上下文（会自动解析请求）
        Context context = new Context(channel);
        
        // 检查是否为 WebSocket 握手请求
        HttpRequest request = context.getRequest();
        if (request != null && isWebSocketUpgrade(request)) {
            handleWebSocketHandshake(channel, request, context);
            return;
        }
        
        // 分发请求（带异常处理）
        dispatchRequest(context);
    }
    
    /**
     * 检查是否为 WebSocket 升级请求
     */
    private boolean isWebSocketUpgrade(HttpRequest request) {
        String upgrade = request.getHeader("Upgrade");
        return "websocket".equalsIgnoreCase(upgrade);
    }
    
    /**
     * 处理 WebSocket 握手
     */
    private void handleWebSocketHandshake(SocketChannel channel, HttpRequest request, Context context) throws Exception {
        String secWebSocketKey = request.getHeader("Sec-WebSocket-Key");
        
        if (secWebSocketKey == null || secWebSocketKey.isEmpty()) {
            throw new Exception("Missing Sec-WebSocket-Key");
        }
        
        String response = Utils.getHandShakeResponse(secWebSocketKey);
        channel.write(ByteBuffer.wrap(response.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        
        // 保存 WebSocket 上下文
        DApp.wsContexts.put(channel, context);
    }
    
    /**
     * 分发请求到 DispatcherServlet（带异常捕获）
     */
    private void dispatchRequest(Context context) {
        // 委托给 DispatcherServlet 处理（内部已有异常处理）
        dispatcherServlet.handle(context);
    }
}
