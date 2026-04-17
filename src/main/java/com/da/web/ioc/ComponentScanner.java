package com.da.web.ioc;

import com.da.web.annotations.Component;
import com.da.web.annotations.Path;
import com.da.web.util.Logger;
import com.da.web.util.Utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * 组件扫描器，负责扫描指定包下的@Component 和@Path 注解类
 */
public class ComponentScanner {
    
    private final String basePackage;
    private final List<Class<?>> scannedClasses = new ArrayList<>();
    
    public ComponentScanner(String basePackage) {
        this.basePackage = basePackage;
    }
    
    /**
     * 执行扫描
     * @return 扫描到的类列表
     */
    public List<Class<?>> scan() {
        scannedClasses.clear();
        String rootPathName = Utils.replace(basePackage, "\\.", "/");
        File rootPath = Utils.getResourceFile(rootPathName);
        
        if (rootPath != null) {
            List<File> files = Utils.scanFileToList(rootPath);
            files.forEach(file -> handleScanFile(basePackage, file));
        }
        
        Logger.info(ComponentScanner.class, "扫描完成，共发现 " + scannedClasses.size() + " 个组件类");
        return scannedClasses;
    }
    
    /**
     * 处理扫描出来的每个文件
     */
    private void handleScanFile(String packageName, File file) {
        String fileAbsolutePath = file.getAbsolutePath();
        
        if (fileAbsolutePath.endsWith(".class")) {
            String className = extractClassName(packageName, fileAbsolutePath);
            if (className != null) {
                processClassName(className);
            }
        }
    }
    
    /**
     * 从文件路径提取类名
     */
    private String extractClassName(String packageName, String fileAbsolutePath) {
        String OS_NAME = System.getProperty("os.name");
        String className;
        
        if (OS_NAME != null && OS_NAME.startsWith("Windows")) {
            className = Utils.replace(fileAbsolutePath, "\\\\", "\\.");
        } else {
            className = Utils.replace(fileAbsolutePath, "/", "\\.");
        }
        
        className = className.substring(className.indexOf(packageName));
        className = className.substring(0, className.lastIndexOf("."));
        
        return className;
    }
    
    /**
     * 处理类名，检查是否有目标注解
     */
    private void processClassName(String className) {
        Class<?> clz = Utils.loadClass(className);
        if (clz == null) {
            return;
        }
        
        boolean hasTargetAnnotation = false;
        
        // 检查@Component 注解
        if (clz.isAnnotationPresent(Component.class)) {
            hasTargetAnnotation = true;
        }
        // 检查@Path 注解
        else if (clz.isAnnotationPresent(Path.class)) {
            hasTargetAnnotation = true;
        }
        // 检查 ORM Mapper 注解（如果存在）
        else if (hasMapperAnnotation(clz)) {
            hasTargetAnnotation = true;
        }
        
        if (hasTargetAnnotation) {
            scannedClasses.add(clz);
        }
    }
    
    /**
     * 检查是否有 ORM Mapper 注解
     */
    private boolean hasMapperAnnotation(Class<?> clz) {
        if (!Utils.isReadExist("com.da.orm.annotation.Mapper")) {
            return false;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Class<Annotation> mapperClass = (Class<Annotation>) Class.forName("com.da.orm.annotation.Mapper");
            return clz.isAnnotationPresent(mapperClass);
        } catch (Exception e) {
            Logger.error(ComponentScanner.class, e);
            return false;
        }
    }
    
    /**
     * 获取扫描结果
     */
    public List<Class<?>> getScannedClasses() {
        return scannedClasses;
    }
}
