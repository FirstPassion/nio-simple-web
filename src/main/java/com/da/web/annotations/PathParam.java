package com.da.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 路径参数绑定注解
 * 用于将 URL 路径中的变量绑定到方法参数
 * 例如：/user/{id} 中的 id
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathParam {
    /**
     * 路径变量名称
     */
    String value();
}
