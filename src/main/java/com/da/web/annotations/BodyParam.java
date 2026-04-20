package com.da.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求体参数绑定注解
 * 用于将 HTTP 请求体绑定到方法参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface BodyParam {
    /**
     * 参数名称，默认为空表示绑定整个请求体
     */
    String value() default "";
}
