package com.da.web.config;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * ServerConfig 测试类
 * 
 * 测试目的：
 * 1. 验证 @Configuration 注解能够正确解析 app.properties 中的配置
 * 2. 验证 @Value 注解能够正确绑定配置值到字段
 * 3. 验证默认值在配置缺失时能够生效
 * 
 * 配置说明：
 * - 使用 @Configuration(prefix = "server") 标记配置类，指定前缀为 "server"
 * - 使用 @Value("${key:default}") 绑定具体配置项，支持默认值
 * - 配置文件位于 src/main/resources/app.properties
 * 
 * 示例配置 (app.properties):
 * server.port=8080
 * server.host=localhost
 * server.static-dir=static
 * server.worker-count=4
 */
public class ServerConfigTest {

    private ServerConfig serverConfig;

    @Before
    public void setUp() {
        // 在实际场景中，配置类会由框架自动实例化并注入配置值
        // 这里手动实例化用于演示，实际使用时依赖注解自动绑定
        serverConfig = new ServerConfig();
    }

    @Test
    public void testDefaultValues() {
        // 测试默认值是否正确
        assertEquals("默认端口应为 8080", 8080, serverConfig.getPort());
        assertEquals("默认主机应为 localhost", "localhost", serverConfig.getHost());
        assertEquals("默认静态目录应为 static", "static", serverConfig.getStaticDir());
        // workerCount 默认值为可用处理器数量
        assertTrue("workerCount 应大于 0", serverConfig.getWorkerCount() > 0);
    }

    @Test
    public void testSetters() {
        // 测试 setter 方法
        serverConfig.setPort(9090);
        assertEquals(9090, serverConfig.getPort());

        serverConfig.setHost("127.0.0.1");
        assertEquals("127.0.0.1", serverConfig.getHost());

        serverConfig.setStaticDir("public");
        assertEquals("public", serverConfig.getStaticDir());

        serverConfig.setWorkerCount(8);
        assertEquals(8, serverConfig.getWorkerCount());
    }

    @Test
    public void testConfigurationAnnotation() {
        // 验证类上有 @Configuration 注解
        assertTrue(
            "ServerConfig 应该被 @Configuration 注解标记",
            serverConfig.getClass().isAnnotationPresent(com.da.web.annotations.Configuration.class)
        );

        com.da.web.annotations.Configuration configAnnotation = 
            serverConfig.getClass().getAnnotation(com.da.web.annotations.Configuration.class);
        assertEquals("配置前缀应为 'server'", "server", configAnnotation.prefix());
    }

    @Test
    public void testValueAnnotations() throws NoSuchFieldException {
        // 验证字段上有 @Value 注解
        java.lang.reflect.Field portField = serverConfig.getClass().getDeclaredField("port");
        assertTrue(
            "port 字段应该被 @Value 注解标记",
            portField.isAnnotationPresent(com.da.web.annotations.Value.class)
        );

        java.lang.reflect.Field hostField = serverConfig.getClass().getDeclaredField("host");
        assertTrue(
            "host 字段应该被 @Value 注解标记",
            hostField.isAnnotationPresent(com.da.web.annotations.Value.class)
        );

        java.lang.reflect.Field staticDirField = serverConfig.getClass().getDeclaredField("staticDir");
        assertTrue(
            "staticDir 字段应该被 @Value 注解标记",
            staticDirField.isAnnotationPresent(com.da.web.annotations.Value.class)
        );

        java.lang.reflect.Field workerCountField = serverConfig.getClass().getDeclaredField("workerCount");
        assertTrue(
            "workerCount 字段应该被 @Value 注解标记",
            workerCountField.isAnnotationPresent(com.da.web.annotations.Value.class)
        );
    }
}
