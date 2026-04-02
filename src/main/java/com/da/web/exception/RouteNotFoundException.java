package com.da.web.exception;

/**
 * 路由未找到异常
 */
public class RouteNotFoundException extends ServerException {
    
    private final String path;
    
    public RouteNotFoundException(String path) {
        super("Route not found: " + path, 404);
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
}
