package com.da.web.exception;

/**
 * HTTP 解析异常
 */
public class HttpParseException extends Exception {
    public HttpParseException(String message) {
        super(message);
    }

    public HttpParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
