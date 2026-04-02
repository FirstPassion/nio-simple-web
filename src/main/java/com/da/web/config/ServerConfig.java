package com.da.web.config;

/**
 * 服务器配置类
 */
public class ServerConfig {
    
    private int port = 8080;
    private String staticDir = "static";
    private String host = "localhost";
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
