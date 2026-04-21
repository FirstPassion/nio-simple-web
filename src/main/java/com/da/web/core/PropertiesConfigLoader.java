package com.da.web.core;

import com.da.web.annotations.Configuration;
import com.da.web.annotations.Value;
import com.da.web.util.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Properties;

/**
 * Properties 配置加载器，负责解析 app.properties 并注入到配置类中
 * 模仿 Spring Boot 的配置加载机制
 */
public class PropertiesConfigLoader {
    
    private static final String CONFIG_FILE = "app.properties";
    private Properties configProps;
    
    /**
     * 加载 app.properties 配置文件
     */
    public PropertiesConfigLoader() {
        this.configProps = loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private Properties loadConfig() {
        Properties props = new Properties();
        try {
            URL url = getClass().getClassLoader().getResource(CONFIG_FILE);
            if (url == null) {
                Logger.warn(PropertiesConfigLoader.class, "未找到配置文件 " + CONFIG_FILE + "，将使用默认配置");
                return props;
            }
            
            try (InputStream inputStream = url.openStream()) {
                props.load(inputStream);
                Logger.info(PropertiesConfigLoader.class, "成功加载配置文件：" + CONFIG_FILE);
            }
        } catch (Exception e) {
            Logger.error(PropertiesConfigLoader.class, "加载配置文件失败：" + e.getMessage());
        }
        return props;
    }
    
    /**
     * 根据 key 获取配置值
     * @param key 配置键，如 "server.port"
     * @return 配置值
     */
    public Object getValue(String key) {
        if (configProps == null || key == null || key.isEmpty()) {
            return null;
        }
        return configProps.getProperty(key);
    }
    
    /**
     * 根据 key 获取配置值，带默认值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    public Object getValue(String key, Object defaultValue) {
        Object value = getValue(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 解析 Value 注解的值，支持 ${key} 和 ${key:default} 格式
     * @param valueStr 注解值
     * @return 解析后的配置值
     */
    public Object parseValue(String valueStr) {
        if (valueStr == null || valueStr.isEmpty()) {
            return null;
        }
        
        // 处理 ${key} 或 ${key:default} 格式
        if (valueStr.startsWith("${") && valueStr.endsWith("}")) {
            String content = valueStr.substring(2, valueStr.length() - 1);
            int colonIndex = content.indexOf(":");
            
            String key;
            String defaultValue = null;
            
            if (colonIndex > 0) {
                key = content.substring(0, colonIndex);
                defaultValue = content.substring(colonIndex + 1);
            } else {
                key = content;
            }
            
            Object value = getValue(key);
            if (value != null) {
                return value;
            }
            
            // 返回默认值
            if (defaultValue != null) {
                return defaultValue;
            }
        }
        
        // 直接返回值（不带 ${} 的情况）
        return valueStr;
    }
    
    /**
     * 将配置注入到配置对象中
     * @param configObject 配置对象实例
     */
    public void injectToConfig(Object configObject) {
        if (configObject == null) {
            return;
        }
        
        Class<?> clazz = configObject.getClass();
        
        // 检查是否有@Configuration 注解
        if (!clazz.isAnnotationPresent(Configuration.class)) {
            return;
        }
        
        Configuration configAnno = clazz.getAnnotation(Configuration.class);
        String prefix = configAnno.prefix();
        
        // 遍历所有字段，注入@Value 注解的值
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Value.class)) {
                Value valueAnno = field.getAnnotation(Value.class);
                String valueStr = valueAnno.value();
                
                Object configValue = parseValue(valueStr);
                if (configValue != null) {
                    injectFieldValue(configObject, field, configValue);
                }
            }
        }
    }
    
    /**
     * 注入字段值，进行类型转换
     */
    private void injectFieldValue(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            
            // 类型转换
            Object convertedValue = convertType(value, fieldType);
            field.set(target, convertedValue);
            
            Logger.debug(PropertiesConfigLoader.class, 
                "注入配置：" + field.getName() + " = " + convertedValue);
        } catch (Exception e) {
            Logger.error(PropertiesConfigLoader.class, 
                "注入配置失败 [" + field.getName() + "]: " + e.getMessage());
        }
    }
    
    /**
     * 类型转换
     */
    @SuppressWarnings("unchecked")
    private Object convertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isInstance(value)) {
            return value;
        }
        
        String strValue = value.toString();
        
        try {
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(strValue);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(strValue);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(strValue);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(strValue);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(strValue);
            } else if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(strValue);
            } else if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(strValue);
            } else if (targetType == char.class || targetType == Character.class) {
                return strValue.charAt(0);
            } else if (targetType == String.class) {
                return strValue;
            }
        } catch (Exception e) {
            Logger.warn(PropertiesConfigLoader.class, 
                "类型转换失败 [" + targetType.getSimpleName() + "]: " + strValue);
        }
        
        return value;
    }
    
    /**
     * 获取配置映射（用于调试）
     */
    public Properties getConfigProps() {
        return configProps;
    }
}
