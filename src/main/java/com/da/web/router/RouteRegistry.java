package com.da.web.router;

import com.da.web.function.Handler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路由注册表
 */
public class RouteRegistry {
    
    private final Map<String, Handler> routes = new ConcurrentHashMap<>();
    
    /**
     * 注册路由
     */
    public void register(String path, Handler handler) {
        routes.put(path, handler);
    }
    
    /**
     * 获取路由处理器
     */
    public Optional<Handler> getHandler(String path) {
        return Optional.ofNullable(routes.get(path));
    }
    
    /**
     * 检查路由是否存在
     */
    public boolean hasRoute(String path) {
        return routes.containsKey(path);
    }
    
    /**
     * 移除路由
     */
    public void remove(String path) {
        routes.remove(path);
    }
    
    /**
     * 获取所有注册的路由路径
     */
    public java.util.Set<String> getPaths() {
        return routes.keySet();
    }
}
