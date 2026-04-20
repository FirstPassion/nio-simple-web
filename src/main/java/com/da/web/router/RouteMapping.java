package com.da.web.router;

import com.da.web.function.Handler;

/**
 * 路由映射类，支持 HTTP 方法和路径变量
 */
public class RouteMapping {
    
    private final String path;
    private final String httpMethod;
    private final Handler handler;
    private final boolean hasPathVariables;
    private final String[] pathVariableNames;
    private final int priority;
    
    public RouteMapping(String path, String httpMethod, Handler handler) {
        this.path = path;
        this.httpMethod = httpMethod != null ? httpMethod.toUpperCase() : "GET";
        this.handler = handler;
        
        // 解析路径变量
        if (path.contains("{") && path.contains("}")) {
            this.hasPathVariables = true;
            String[] parts = path.split("/");
            java.util.List<String> varNames = new java.util.ArrayList<>();
            for (String part : parts) {
                if (part.startsWith("{") && part.endsWith("}")) {
                    varNames.add(part.substring(1, part.length() - 1));
                }
            }
            this.pathVariableNames = varNames.toArray(new String[0]);
            // 带路径变量的路由优先级较低
            this.priority = 1;
        } else {
            this.hasPathVariables = false;
            this.pathVariableNames = new String[0];
            // 精确匹配的路由优先级较高
            this.priority = 0;
        }
    }
    
    /**
     * @deprecated 使用新的构造函数 RouteMapping(String path, String httpMethod, Handler handler)
     */
    @Deprecated
    public RouteMapping(String path, Handler handler) {
        this(path, "GET", handler);
    }
    
    public String getPath() {
        return path;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public Handler getHandler() {
        return handler;
    }
    
    public boolean hasPathVariables() {
        return hasPathVariables;
    }
    
    public String[] getPathVariableNames() {
        return pathVariableNames;
    }
    
    public int getPriority() {
        return priority;
    }
    
    /**
     * 检查请求路径是否匹配此路由
     */
    public boolean matches(String requestPath, String requestMethod) {
        if (!httpMethod.equals(requestMethod.toUpperCase())) {
            return false;
        }
        
        if (path.equals(requestPath)) {
            return true;
        }
        
        if (hasPathVariables) {
            return matchWithPathVariables(requestPath);
        }
        
        return false;
    }
    
    /**
     * 匹配带路径变量的路径
     */
    private boolean matchWithPathVariables(String requestPath) {
        String[] pathParts = path.split("/");
        String[] requestParts = requestPath.split("/");
        
        if (pathParts.length != requestParts.length) {
            return false;
        }
        
        for (int i = 0; i < pathParts.length; i++) {
            String pathPart = pathParts[i];
            String requestPart = requestParts[i];
            
            // 如果是路径变量（以{开头，以}结尾），则匹配任何值
            if (pathPart.startsWith("{") && pathPart.endsWith("}")) {
                continue;
            }
            
            // 否则必须精确匹配
            if (!pathPart.equals(requestPart)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 从请求路径中提取路径变量值
     */
    public java.util.Map<String, String> extractPathVariables(String requestPath) {
        java.util.Map<String, String> variables = new java.util.HashMap<>();
        
        if (!hasPathVariables) {
            return variables;
        }
        
        String[] pathParts = path.split("/");
        String[] requestParts = requestPath.split("/");
        
        int varIndex = 0;
        for (int i = 0; i < pathParts.length && varIndex < pathVariableNames.length; i++) {
            String pathPart = pathParts[i];
            if (pathPart.startsWith("{") && pathPart.endsWith("}")) {
                variables.put(pathVariableNames[varIndex], requestParts[i]);
                varIndex++;
            }
        }
        
        return variables;
    }
}
