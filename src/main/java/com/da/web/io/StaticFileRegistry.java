package com.da.web.io;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 静态文件注册表
 */
public class StaticFileRegistry {
    
    private final Map<String, File> staticFiles = new ConcurrentHashMap<>();
    
    /**
     * 注册静态文件
     */
    public void register(String path, File file) {
        if (file != null && file.exists()) {
            staticFiles.put(path, file);
        }
    }
    
    /**
     * 获取静态文件
     */
    public java.util.Optional<File> getFile(String path) {
        return java.util.Optional.ofNullable(staticFiles.get(path));
    }
    
    /**
     * 检查静态文件是否存在
     */
    public boolean hasFile(String path) {
        return staticFiles.containsKey(path);
    }
    
    /**
     * 移除静态文件映射
     */
    public void remove(String path) {
        staticFiles.remove(path);
    }
    
    /**
     * 获取所有静态文件路径
     */
    public java.util.Set<String> getPaths() {
        return staticFiles.keySet();
    }
}
