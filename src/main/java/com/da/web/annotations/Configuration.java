package com.da.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为配置类，类似于 Spring Boot 的 @Configuration
 * 配置类中的字段可以通过 @Value 注解绑定到 app.properties 中的配置项
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {
    /**
     * 配置的前缀，用于匹配 app.properties 中的配置项
     * 例如 prefix = "server" 会匹配 server.port, server.host 等
     */
    String prefix() default "";
}
