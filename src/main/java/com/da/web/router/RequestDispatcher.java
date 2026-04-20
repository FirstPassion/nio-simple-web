package com.da.web.router;

import com.da.web.ioc.BeanContainer;
import com.da.web.core.Context;
import com.da.web.function.Handler;
import com.da.web.io.StaticFileRegistry;

import java.io.File;
import java.util.Optional;

/**
 * 请求分发器，统一处理路由匹配逻辑
 * 匹配顺序：routes → staticFiles → beans
 */
public class RequestDispatcher {
    
    private final RouteRegistry routeRegistry;
    private final BeanContainer beanContainer;
    private final StaticFileRegistry staticFileRegistry;
    
    public RequestDispatcher(RouteRegistry routeRegistry, 
                            BeanContainer beanContainer,
                            StaticFileRegistry staticFileRegistry) {
        this.routeRegistry = routeRegistry;
        this.beanContainer = beanContainer;
        this.staticFileRegistry = staticFileRegistry;
    }
    
    /**
     * 分发请求到对应的处理器
     */
    public void dispatch(Context context) throws Exception {
        String url = context.getUrl();
        
        // 1. 优先匹配显式注册的路由
        Optional<Handler> routeHandler = routeRegistry.getHandler(url);
        if (routeHandler.isPresent()) {
            routeHandler.get().callback(context);
            return;
        }
        
        // 2. 匹配静态资源
        Optional<File> staticFile = staticFileRegistry.getFile(url);
        if (staticFile.isPresent()) {
            context.send(staticFile.get());
            return;
        }
        
        // 3. 匹配 Bean（@Path 注解的类）
        if (beanContainer.containsBean(url)) {
            Object bean = beanContainer.getBean(url).get();
            if (bean instanceof Handler) {
                ((Handler) bean).callback(context);
                return;
            }
        }
        
        // 4. 未找到，返回 404
        context.sendHtml("<h1 style='color: red;text-align: center;'>404 not found</h1><hr/>", 404);
    }
    
    /**
     * 检查是否存在对应的路由
     */
    public boolean hasRoute(String url) {
        return routeRegistry.hasRoute(url) || 
               staticFileRegistry.hasFile(url) || 
               beanContainer.containsBean(url);
    }
}
