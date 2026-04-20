package com.da.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP 请求映射注解（通用）
 * 可用于标记处理任意 HTTP 方法请求的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    /**
     * 请求路径，支持路径变量如 /user/{id}
     */
    String value();
    
    /**
     * 指定 HTTP 方法，默认为空表示匹配所有方法
     */
    String method() default "";
}
