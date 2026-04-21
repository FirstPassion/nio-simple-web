package com.da.web.core;

/**
 * 处理器链节点（责任链模式）
 */
public interface HandlerChain {
    
    /**
     * 处理请求
     * @param context 请求上下文
     * @param next 下一个处理器
     */
    void handle(Context context, HandlerChain next) throws Exception;
}
