package com.da.web.ioc;

import com.da.web.annotations.Component;
import com.da.web.annotations.Path;
import com.da.web.annotations.*;
import com.da.web.function.Handler;
import com.da.web.util.Logger;
import com.da.web.util.Utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 组件扫描器，负责扫描指定包下的@Component 和@Path 注解类
 * 并自动注册带@Get、@Post 等注解的方法路由
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
    
    /**
     * 注册@Path 类中带 HTTP 方法注解的路由
     * @param beanInstance Bean 实例
     * @param routeRegistry 路由注册表
     */
    public void registerAnnotatedRoutes(Object beanInstance, com.da.web.router.RouteRegistry routeRegistry) {
        Class<?> clazz = beanInstance.getClass();
        
        // 检查是否有@Path 注解
        if (!clazz.isAnnotationPresent(Path.class)) {
            return;
        }
        
        Path pathAnnotation = clazz.getAnnotation(Path.class);
        String classPath = pathAnnotation.value();
        
        // 遍历所有公共方法
        for (Method method : clazz.getMethods()) {
            registerMethodRoute(beanInstance, classPath, method, routeRegistry);
        }
        
        // 也检查声明的方法（包括非公共方法）
        for (Method method : clazz.getDeclaredMethods()) {
            registerMethodRoute(beanInstance, classPath, method, routeRegistry);
        }
    }
    
    /**
     * 注册单个方法的路由
     */
    private void registerMethodRoute(Object beanInstance, String classPath, Method method, 
                                     com.da.web.router.RouteRegistry routeRegistry) {
        String methodPath = null;
        String httpMethod = "GET";
        
        // 检查@Get 注解
        if (method.isAnnotationPresent(Get.class)) {
            Get get = method.getAnnotation(Get.class);
            methodPath = get.value();
            httpMethod = "GET";
        }
        // 检查@Post 注解
        else if (method.isAnnotationPresent(Post.class)) {
            Post post = method.getAnnotation(Post.class);
            methodPath = post.value();
            httpMethod = "POST";
        }
        // 检查@Put 注解
        else if (method.isAnnotationPresent(Put.class)) {
            Put put = method.getAnnotation(Put.class);
            methodPath = put.value();
            httpMethod = "PUT";
        }
        // 检查@Delete 注解
        else if (method.isAnnotationPresent(Delete.class)) {
            Delete delete = method.getAnnotation(Delete.class);
            methodPath = delete.value();
            httpMethod = "DELETE";
        }
        // 检查@RequestMapping 注解
        else if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            methodPath = mapping.value();
            String m = mapping.method();
            if (m != null && !m.isEmpty()) {
                httpMethod = m;
            }
        }
        
        // 如果没有找到任何路由注解，跳过
        if (methodPath == null) {
            return;
        }
        
        // 组合完整路径
        String fullPath = combinePaths(classPath, methodPath);
        
        // 创建 Handler 包装器
        Handler handler = ctx -> {
            try {
                // 设置路径变量到 Context
                method.setAccessible(true);
                
                // 构建参数
                Object[] args = buildMethodArguments(method, ctx);
                
                // 调用方法
                method.invoke(beanInstance, args);
            } catch (Exception e) {
                throw new RuntimeException("调用方法失败：" + method.getName(), e);
            }
        };
        
        // 注册路由
        routeRegistry.register(fullPath, httpMethod, handler);
        Logger.info(ComponentScanner.class, "自动注册路由：" + httpMethod + " " + fullPath);
    }
    
    /**
     * 组合类路径和方法路径
     */
    private String combinePaths(String classPath, String methodPath) {
        if (classPath.endsWith("/")) {
            classPath = classPath.substring(0, classPath.length() - 1);
        }
        if (!methodPath.startsWith("/")) {
            methodPath = "/" + methodPath;
        }
        return classPath + methodPath;
    }
    
    /**
     * 构建方法参数
     */
    private Object[] buildMethodArguments(Method method, com.da.web.core.Context ctx) throws Exception {
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        
        for (int i = 0; i < paramTypes.length; i++) {
            // 检查参数注解
            boolean hasAnnotation = false;
            
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation instanceof PathParam) {
                    String paramName = ((PathParam) annotation).value();
                    args[i] = ctx.getPathVariable(paramName);
                    hasAnnotation = true;
                    break;
                } else if (annotation instanceof QueryParam) {
                    String paramName = ((QueryParam) annotation).value();
                    Object param = ctx.getParams().get(paramName);
                    args[i] = convertType(param, paramTypes[i]);
                    hasAnnotation = true;
                    break;
                } else if (annotation instanceof BodyParam) {
                    String paramName = ((BodyParam) annotation).value();
                    if (paramName.isEmpty()) {
                        // 绑定整个请求体
                        args[i] = ctx.getBodyAs(paramTypes[i]);
                    } else {
                        Object param = ctx.getBodyParam(paramName);
                        args[i] = convertType(param, paramTypes[i]);
                    }
                    hasAnnotation = true;
                    break;
                } else if (annotation instanceof Header) {
                    String headerName = ((Header) annotation).value();
                    args[i] = ctx.getHeader(headerName);
                    hasAnnotation = true;
                    break;
                }
            }
            
            // 如果没有注解，尝试按类型自动绑定
            if (!hasAnnotation) {
                if (paramTypes[i] == com.da.web.core.Context.class) {
                    args[i] = ctx;
                } else if (paramTypes[i] == String.class) {
                    // 尝试从路径变量或查询参数获取
                    args[i] = null;
                } else {
                    // 其他类型尝试从请求体解析
                    args[i] = ctx.getBodyAs(paramTypes[i]);
                }
            }
        }
        
        return args;
    }
    
    /**
     * 类型转换辅助方法
     */
    private Object convertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isInstance(value)) {
            return value;
        }
        
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value.toString());
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value.toString());
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value.toString());
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value.toString());
        }
        
        return value;
    }
}
