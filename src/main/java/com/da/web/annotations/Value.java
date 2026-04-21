package com.da.web.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于将 app.properties 中的配置项注入到字段上，类似于 Spring Boot 的 @Value
 * 使用方式：@Value("${server.port}") 或 @Value("8080") 指定默认值
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    /**
     * 配置项的 key，支持 ${key} 和 ${key:default} 格式
     * 例如：${server.port} 或 ${server.port:8080}
     */
    String value();
}
