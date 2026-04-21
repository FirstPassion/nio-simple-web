package com.da.web.ioc;

import com.da.web.annotations.Inject;
import com.da.web.annotations.Configuration;
import com.da.web.core.Context;
import com.da.web.core.PropertiesConfigLoader;
import com.da.web.util.Logger;
import com.da.web.util.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Function;

/**
 * 依赖注入器，统一处理@Inject 注解的字段注入（支持实例字段和静态字段）
 */
public class DependencyInjector {
    
    private final BeanContainer beanContainer;
    private final PropertiesConfigLoader configLoader;
    
    public DependencyInjector(BeanContainer beanContainer) {
        this.beanContainer = beanContainer;
        this.configLoader = new PropertiesConfigLoader();
    }
    
    /**
     * 给 Component Bean 注入属性（基本类型和 Bean 引用）
     */
    public void injectToComponentBean(Object bean) {
        Class<?> clz = bean.getClass();
        
        // 如果类有@Configuration 注解，从 YAML 配置注入
        if (clz.isAnnotationPresent(Configuration.class)) {
            configLoader.injectToConfig(bean);
        }
        
        // 注入实例字段
        for (Field field : clz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Inject.class)) {
                String beanNameOrValue = field.getAnnotation(Inject.class).value();
                injectFieldValue(field, beanNameOrValue, bean, null);
            }
        }
    }
    
    /**
     * 注入静态字段（用于未标记@Component 但需要注入的类）
     */
    public void injectStaticFields(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        
        // 递归处理父类
        injectStaticFields(clazz.getSuperclass());
        
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Inject.class)) {
                String beanNameOrValue = field.getAnnotation(Inject.class).value();
                injectStaticFieldValue(field, beanNameOrValue);
            }
        }
    }
    
    /**
     * 给 Path Bean 注入属性（支持请求参数注入）
     */
    public void injectToPathBean(Object bean, Context context) {
        Map<String, Object> params = context.getParams();
        Class<?> clz = bean.getClass();
        
        for (Field field : clz.getDeclaredFields()) {
            // 有@Inject 注解
            if (field.isAnnotationPresent(Inject.class)) {
                String beanNameOrValue = field.getAnnotation(Inject.class).value();
                injectFieldValue(field, beanNameOrValue, bean, context);
            }
            // 没有@Inject 注解，尝试从请求参数注入
            else if (params.containsKey(field.getName())) {
                String value = (String) params.get(field.getName());
                injectFromRequestParam(field, value, bean, context);
            }
        }
    }
    
    /**
     * 注入静态字段值（基本类型或 Bean 引用）
     */
    private void injectStaticFieldValue(Field field, String beanNameOrValue) {
        Function<String, Object> conv = Utils.getTypeConv(field.getType().getName());
        
        // 基本类型和 String
        if (conv != null) {
            setStaticFieldAccessible(field, () -> {
                try {
                    Object value = conv.apply(beanNameOrValue);
                    field.set(null, value);
                } catch (Exception e) {
                    logStaticInjectionError(field, e);
                }
            });
        }
        // Bean 引用注入
        else if (beanContainer.containsBean(beanNameOrValue)) {
            beanContainer.getBean(beanNameOrValue).ifPresent(targetBean -> setStaticFieldAccessible(field, () -> {
                try {
                    field.set(null, targetBean);
                } catch (IllegalAccessException e) {
                    logStaticInjectionError(field, e);
                }
            }));
        } else {
            // Bean 不存在，记录错误但不抛出异常（允许可选依赖）
            Logger.error(DependencyInjector.class, 
                "注入失败：静态字段 " + field.getName() + " 找不到 Bean: " + beanNameOrValue);
        }
    }
    
    /**
     * 注入字段值（基本类型或 Bean 引用）
     */
    private void injectFieldValue(Field field, String beanNameOrValue, Object bean, Context context) {
        Function<String, Object> conv = Utils.getTypeConv(field.getType().getName());
        
        // 基本类型和 String
        if (conv != null) {
            setFieldAccessible(field, bean, () -> {
                try {
                    Object value = conv.apply(beanNameOrValue);
                    field.set(bean, value);
                } catch (Exception e) {
                    logInjectionError(field, bean, e, context);
                }
            });
        }
        // Bean 引用注入
        else if (beanContainer.containsBean(beanNameOrValue)) {
            beanContainer.getBean(beanNameOrValue).ifPresent(targetBean -> setFieldAccessible(field, bean, () -> {
                try {
                    field.set(bean, targetBean);
                } catch (IllegalAccessException e) {
                    logInjectionError(field, bean, e, context);
                }
            }));
        }
    }
    
    /**
     * 从请求参数注入
     */
    private void injectFromRequestParam(Field field, String value, Object bean, Context context) {
        Function<String, Object> conv = Utils.getTypeConv(field.getType().getName());
        
        if (conv != null) {
            setFieldAccessible(field, bean, () -> {
                try {
                    Object o = conv.apply(value);
                    field.set(bean, o);
                } catch (Exception e) {
                    logInjectionError(field, bean, e, context);
                }
            });
        }
    }
    
    /**
     * 设置字段可访问并执行注入操作（实例字段）
     */
    private void setFieldAccessible(Field field, Object bean, Runnable injectAction) {
        field.setAccessible(true);
        try {
            injectAction.run();
        } finally {
            field.setAccessible(false);
        }
    }
    
    /**
     * 设置静态字段可访问并执行注入操作
     */
    private void setStaticFieldAccessible(Field field, Runnable injectAction) {
        field.setAccessible(true);
        try {
            injectAction.run();
        } finally {
            field.setAccessible(false);
        }
    }
    
    /**
     * 记录注入错误日志（实例字段）
     */
    private void logInjectionError(Field field, Object bean, Exception e, Context context) {
        if (context != null) {
            context.errPrint(e);
        } else {
            Logger.error(DependencyInjector.class, 
                "注入失败：" + field.getName() + " in " + bean.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * 记录静态字段注入错误日志
     */
    private void logStaticInjectionError(Field field, Exception e) {
        Logger.error(DependencyInjector.class, 
            "静态字段注入失败：" + field.getName() + " in " + field.getDeclaringClass().getSimpleName(), e);
    }
}
