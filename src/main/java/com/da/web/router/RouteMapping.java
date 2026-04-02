package com.da.web.router;

import com.da.web.function.Handler;

/**
 * 路由映射类
 */
public class RouteMapping {
    
    private final String path;
    private final Handler handler;
    
    public RouteMapping(String path, Handler handler) {
        this.path = path;
        this.handler = handler;
    }
    
    public String getPath() {
        return path;
    }
    
    public Handler getHandler() {
        return handler;
    }
}
