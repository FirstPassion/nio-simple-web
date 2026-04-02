package com.da.web.enums;

import com.da.web.constant.HttpStatus;

/**
 * HTTP 状态码枚举
 * @deprecated 请使用 {@link HttpStatus} 常量类
 */
@Deprecated
public enum States {
    OK(HttpStatus.OK),
    ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FOUND(HttpStatus.NOT_FOUND);
    
    private final int code;

    States(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return String.valueOf(code);
    }
}
