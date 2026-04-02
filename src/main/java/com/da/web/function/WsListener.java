package com.da.web.function;

import com.da.web.core.Context;

/**
 * WebSocket 监听器接口
 * <p>
 * 用于处理 WebSocket 连接的生命周期事件
 */
public interface WsListener {
    /**
     * 当 WebSocket 连接建立时调用
     *
     * @param ctx 上下文对象
     */
    default void onOpen(Context ctx) throws Exception {
        // 可选实现
    }

    /**
     * 当收到客户端消息时调用
     *
     * @param ctx     上下文对象
     * @param message 消息内容
     * @throws Exception 处理异常
     */
    void onMessage(Context ctx, String message) throws Exception;

    /**
     * 当连接发生错误时调用
     *
     * @param ctx 上下文对象
     * @param e   异常信息，为 null 时表示正常关闭
     */
    void onError(Context ctx, Exception e);

    /**
     * 当 WebSocket 连接关闭时调用
     *
     * @param ctx 上下文对象
     * @throws Exception 处理异常
     */
    default void onClose(Context ctx) throws Exception {
        // 可选实现
    }
}
