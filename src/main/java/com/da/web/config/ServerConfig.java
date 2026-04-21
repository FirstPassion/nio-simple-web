package com.da.web.config;

import com.da.web.annotations.Configuration;
import com.da.web.annotations.Value;

/**
 * 服务器配置类
 * 使用 @Configuration 标记为配置类，并通过 @Value 注解绑定 app.properties 中的配置
 */
@Configuration(prefix = "server")
public class ServerConfig {
    
    @Value("${server.port:8080}")
    private int port = 8080;
    
    @Value("${server.static-dir:static}")
    private String staticDir = "static";
    
    @Value("${server.host:localhost}")
    private String host = "localhost";
    
    @Value("${server.worker-count:4}")
    private int workerCount = Runtime.getRuntime().availableProcessors();
    
    public ServerConfig() {
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getStaticDir() {
        return staticDir;
    }
    
    public void setStaticDir(String staticDir) {
        this.staticDir = staticDir;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getWorkerCount() {
        return workerCount;
    }
    
    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }
}
