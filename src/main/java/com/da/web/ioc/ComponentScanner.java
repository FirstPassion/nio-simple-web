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
     */
    public void scan() {
        scannedClasses.clear();
        String rootPathName = Utils.replace(basePackage, "\\.", "/");
        File rootPath = Utils.getResourceFile(rootPathName);
        
        if (rootPath != null) {
            List<File> files = Utils.scanFileToList(rootPath);
            files.forEach(file -> handleScanFile(basePackage, file));
        }
        
        Logger.info(ComponentScanner.class, "扫描完成，共发现 " + scannedClasses.size() + " 个组件类");
    }
    
    /**
     * 处理扫描出来的每个文件
     */
    private void handleScanFile(String packageName, File file) {
        String fileAbsolutePath = file.getAbsolutePath();
        
        if (fileAbsolutePath.endsWith(".class")) {
            String className = extractClassName(packageName, fileAbsolutePath);
            processClassName(className);
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
        
        // 使用 Set 记录已注册的方法签名，避免重复注册
        java.util.Set<String> registeredMethods = new java.util.HashSet<>();
        
        // 遍历所有公共方法（getMethods 会返回父类的公共方法）
        for (Method method : clazz.getMethods()) {
            // 只处理当前类声明的方法，跳过继承自 Object 等父类的方法
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            //if (registerMethodRoute(beanInstance, classPath, method, routeRegistry, registeredMethods)) {
                //TODO 已成功注册
            //}
        }
    }
    
    /**
     * 注册单个方法的路由
     */
    private boolean registerMethodRoute(Object beanInstance, String classPath, Method method, 
                                     com.da.web.router.RouteRegistry routeRegistry,
                                     java.util.Set<String> registeredMethods) {
        // 生成方法签名用于去重
        String methodSignature = method.getName() + ":" + method.toGenericString();
        if (!registeredMethods.add(methodSignature)) {
            return false; // 已注册过，跳过
        }
        
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
            return false;
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
        return true;
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
    
    /**
     * 实例化 Controller 类（用于测试）
     * 支持无参构造和有参构造的依赖注入
     * @param controllerClass Controller 类
     * @return 实例化的 Controller 对象
     */
    public <T> T instantiateController(Class<T> controllerClass) {
        try {
            // 创建 BeanContainer 用于依赖注入
            BeanContainer beanContainer = new BeanContainer();
            // 使用 Utils.newInstance 进行实例化（支持有参构造）
            return controllerClass.cast(Utils.newInstance(controllerClass, beanContainer));
        } catch (Exception e) {
            throw new RuntimeException("实例化 Controller 失败：" + controllerClass.getName(), e);
        }
    }
}
