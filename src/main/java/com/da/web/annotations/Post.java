package com.da.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP POST 请求映射注解
 * 用于标记处理 POST 请求的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Post {
    /**
     * 请求路径，支持路径变量如 /user/{id}
     */
    String value();
}
