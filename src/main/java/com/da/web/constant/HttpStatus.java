package com.da.web.constant;

/**
 * HTTP 状态码常量
 */
public final class HttpStatus {
    
    public static final int OK = 200;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_SERVER_ERROR = 500;
    
    private HttpStatus() {
        // 私有构造，防止实例化
    }
}
