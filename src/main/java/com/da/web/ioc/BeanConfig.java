package com.da.web.ioc;

/**
 * Bean 配置信息，封装扫描和注册逻辑
 */
public class BeanConfig {
    
    private final String basePackage;
    private final Class<?> configClass;
    
    public BeanConfig(Class<?> configClass) {
        this.configClass = configClass;
        this.basePackage = configClass.getPackage().getName();
    }
    
    /**
     * 获取基础包名
     */
    public String getBasePackage() {
        return basePackage;
    }
    
    /**
     * 获取配置类
     */
    public Class<?> getConfigClass() {
        return configClass;
    }
}
