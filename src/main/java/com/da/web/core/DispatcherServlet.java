package com.da.web.core;

import com.da.web.ioc.BeanContainer;
import com.da.web.io.StaticFileRegistry;
import com.da.web.router.RequestDispatcher;
import com.da.web.router.RouteRegistry;
import com.da.web.util.Logger;

/**
 * 调度中心（外观模式 + 责任链模式）
 * 统一管理请求处理流程：异常捕获 → 路由匹配 → 业务执行
 */
public class DispatcherServlet {
    
    private final RequestDispatcher requestDispatcher;
    private final HandlerChain exceptionHandler;
    
    public DispatcherServlet(RouteRegistry routeRegistry, 
                            BeanContainer beanContainer,
                            StaticFileRegistry staticFileRegistry) {
        // 创建请求分发器
        this.requestDispatcher = new RequestDispatcher(routeRegistry, beanContainer, staticFileRegistry);
        // 创建异常处理器（责任链第一个节点）
        this.exceptionHandler = new ExceptionHandler(requestDispatcher);
    }
    
    /**
     * 处理请求（通过责任链）
     */
    public void handle(Context context) {
        try {
            // 执行责任链：异常处理器会调用内部的 requestDispatcher
            exceptionHandler.handle(context, null);
        } catch (Exception e) {
            // 兜底异常处理，确保任何情况都有响应
            Logger.error(DispatcherServlet.class, "请求处理失败", e);
            context.send("Internal Server Error: " + e.getMessage(), 500);
        }
    }
}
