package com.da.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求头参数绑定注解
 * 用于将 HTTP 请求头绑定到方法参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Header {
    /**
     * 请求头名称
     */
    String value();
}
