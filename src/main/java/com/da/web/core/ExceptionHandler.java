package com.da.web.core;

import com.da.web.router.RequestDispatcher;
import com.da.web.util.Logger;

/**
 * 异常处理器（责任链第一个节点）
 * 确保任何异常都有响应返回，防止连接卡住
 */
public class ExceptionHandler implements HandlerChain {
    
    private final RequestDispatcher requestDispatcher;
    
    public ExceptionHandler(RequestDispatcher requestDispatcher) {
        this.requestDispatcher = requestDispatcher;
    }
    
    @Override
    public void handle(Context context, HandlerChain next) throws Exception {
        try {
            // 执行实际的请求分发
            requestDispatcher.dispatch(context);
        } catch (Exception e) {
            // 捕获所有异常并返回错误响应，防止连接卡住
            Logger.error(ExceptionHandler.class, "请求处理失败", e);
            context.send("Internal Server Error: " + e.getMessage(), 500);
        }
    }
}
