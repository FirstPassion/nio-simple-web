package com.da.web.exception;

/**
 * 服务器异常基类
 */
public class ServerException extends RuntimeException {
    
    private final int statusCode;
    
    public ServerException(String message) {
        this(message, 500);
    }
    
    public ServerException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public ServerException(Throwable cause) {
        this(cause.getMessage(), 500);
    }
    
    public ServerException(Throwable cause, int statusCode) {
        this(cause.getMessage(), statusCode);
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
