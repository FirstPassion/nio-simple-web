package com.da.web.ioc;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean 容器，管理由@Component 和@Path 注解创建的实例
 */
public class BeanContainer {
    
    private final Map<String, Object> beans = new ConcurrentHashMap<>();
    
    /**
     * 注册 Bean
     */
    public void register(String name, Object bean) {
        if (bean != null) {
            beans.put(name, bean);
        }
    }
    
    /**
     * 获取 Bean
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getBean(String name) {
        return Optional.ofNullable((T) beans.get(name));
    }
    
    /**
     * 获取 Bean 并转换类型
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> type) {
        Object bean = beans.get(name);
        if (type.isInstance(bean)) {
            return (T) bean;
        }
        return null;
    }
    
    /**
     * 检查 Bean 是否存在
     */
    public boolean containsBean(String name) {
        return beans.containsKey(name);
    }
    
    /**
     * 移除 Bean
     */
    public void remove(String name) {
        beans.remove(name);
    }
    
    /**
     * 获取所有 Bean 名称
     */
    public java.util.Set<String> getBeanNames() {
        return beans.keySet();
    }
}
