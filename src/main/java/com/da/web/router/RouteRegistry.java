package com.da.web.router;

import com.da.web.function.Handler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 路由注册表，支持 HTTP 方法和路径变量
 */
public class RouteRegistry {
    
    // 存储所有路由映射，按优先级排序
    private final List<RouteMapping> routes = new ArrayList<>();
    // 用于快速查找精确匹配的路由 (method:path -> handler)
    private final Map<String, Handler> exactRoutes = new ConcurrentHashMap<>();
    
    /**
     * 注册路由（默认 GET 方法）
     */
    public void register(String path, Handler handler) {
        register(path, "GET", handler);
    }
    
    /**
     * 注册路由（指定 HTTP 方法）
     */
    public void register(String path, String httpMethod, Handler handler) {
        RouteMapping mapping = new RouteMapping(path, httpMethod, handler);
        routes.add(mapping);
        
        // 如果是精确路径（无路径变量），添加到快速查找表
        if (!mapping.hasPathVariables()) {
            String key = httpMethod.toUpperCase() + ":" + path;
            exactRoutes.put(key, handler);
        }
        
        // 按优先级排序（精确匹配优先）
        routes.sort(Comparator.comparingInt(RouteMapping::getPriority));
    }
    
    /**
     * 获取路由处理器
     */
    public Optional<Handler> getHandler(String path, String httpMethod) {
        // 1. 先尝试精确匹配
        String key = httpMethod.toUpperCase() + ":" + path;
        Handler exactHandler = exactRoutes.get(key);
        if (exactHandler != null) {
            return Optional.of(exactHandler);
        }
        
        // 2. 尝试带路径变量的匹配
        for (RouteMapping mapping : routes) {
            if (mapping.matches(path, httpMethod)) {
                return Optional.of(mapping.getHandler());
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 获取路由映射（包含路径变量信息）
     */
    public Optional<RouteMapping> getRouteMapping(String path, String httpMethod) {
        // 1. 先尝试精确匹配
        String key = httpMethod.toUpperCase() + ":" + path;
        Handler exactHandler = exactRoutes.get(key);
        if (exactHandler != null) {
            for (RouteMapping mapping : routes) {
                if (mapping.getPath().equals(path) && mapping.getHttpMethod().equals(httpMethod.toUpperCase())) {
                    return Optional.of(mapping);
                }
            }
        }
        
        // 2. 尝试带路径变量的匹配
        for (RouteMapping mapping : routes) {
            if (mapping.matches(path, httpMethod)) {
                return Optional.of(mapping);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 检查路由是否存在
     */
    public boolean hasRoute(String path, String httpMethod) {
        return getHandler(path, httpMethod).isPresent();
    }
    
    /**
     * 移除路由
     */
    public void remove(String path, String httpMethod) {
        routes.removeIf(mapping -> 
            mapping.getPath().equals(path) && mapping.getHttpMethod().equals(httpMethod.toUpperCase())
        );
        exactRoutes.remove(httpMethod.toUpperCase() + ":" + path);
    }
    
    /**
     * 获取所有注册的路由路径
     */
    public Set<String> getPaths() {
        return routes.stream()
            .map(RouteMapping::getPath)
            .collect(Collectors.toSet());
    }
    
    /**
     * 获取所有路由映射
     */
    public List<RouteMapping> getAllRoutes() {
        return new ArrayList<>(routes);
    }
}
