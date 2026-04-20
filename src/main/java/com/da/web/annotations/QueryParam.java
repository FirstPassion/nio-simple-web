package com.da.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询参数绑定注解
 * 用于将 URL 查询参数绑定到方法参数
 * 例如：/user?name=xxx 中的 name
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryParam {
    /**
     * 查询参数名称
     */
    String value();
}
