package com.da.web.core;

import com.da.web.annotations.Component;
import com.da.web.annotations.Inject;
import com.da.web.annotations.Path;
import com.da.web.bean.BeanContainer;
import com.da.web.function.Handler;
import com.da.web.function.WsListener;
import com.da.web.ioc.BeanConfig;
import com.da.web.ioc.ComponentScanner;
import com.da.web.ioc.DependencyInjector;
import com.da.web.io.StaticFileRegistry;
import com.da.web.router.RequestDispatcher;
import com.da.web.router.RouteRegistry;
import com.da.web.util.Logger;
import com.da.web.util.Utils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.file.Files;
import java.util.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * 服务器启动配置入口
 * 精简后仅负责启动配置和初始化流程编排
 */
public class DApp {
    
    // WebSocket 连接上下文（保持静态以便 Worker 访问）
    public final static Map<SocketChannel, Context> wsContexts = new ConcurrentHashMap<>();
    
    private String staticDirName = "static";
    private int PORT = 8080;
    private final long startTime;
    private boolean isStart = false;
    private Properties properties = null;
    
    // 核心组件
    private final BeanContainer beanContainer;
    private final RouteRegistry routeRegistry;
    private final StaticFileRegistry staticFileRegistry;
    private RequestDispatcher requestDispatcher;
    private DependencyInjector dependencyInjector;
    
    /**
     * 空参构造不会扫描注入 bean
     */
    public DApp() {
        this.startTime = System.currentTimeMillis();
        this.beanContainer = new BeanContainer();
        this.routeRegistry = new RouteRegistry();
        this.staticFileRegistry = new StaticFileRegistry();
        
        getAndParseConfig();
        scanStaticFiles();
    }
    
    /**
     * @param clz 配置类，容器会扫描配置类的包及其子包下面的所有文件
     */
    public DApp(Class<?> clz) {
        this.startTime = System.currentTimeMillis();
        this.beanContainer = new BeanContainer();
        this.routeRegistry = new RouteRegistry();
        this.staticFileRegistry = new StaticFileRegistry();
        
        getAndParseConfig();
        scanStaticFiles();
        initScan(clz);
        
        // 初始化依赖注入器和请求分发器
        this.dependencyInjector = new DependencyInjector(beanContainer);
        this.requestDispatcher = new RequestDispatcher(routeRegistry, beanContainer, staticFileRegistry);
        
        // 给 Component Bean 注入属性
        injectToComponentBeans();
    }
    
    /**
     * 请求注册处理
     *
     * @param path    请求的路由路径
     * @param handler 对对应路由路径的处理
     */
    public void use(String path, Handler handler) {
        routeRegistry.register(path, handler);
    }
    
    /**
     * 用默认端口 (8080) 启动服务
     */
    public void listen() {
        listen(PORT);
    }
    
    /**
     * 指定端口启动服务
     *
     * @param port 端口
     */
    public void listen(int port) {
        System.setProperty("java.awt.headless", Boolean.toString(true));
        
        Thread serverThread = new Thread(() -> start0(port));
        isStart = !serverThread.isAlive();
        serverThread.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    /**
     * 关闭服务器
     */
    public void shutdown() {
        isStart = false;
    }
    
    /**
     * 获取扫描出来的实例化好的 bean
     *
     * @param beanName bean 的名字
     * @return 容器实例化好的 bean
     */
    public Object getBean(String beanName) {
        return beanContainer.getBean(beanName).orElse(null);
    }
    
    /**
     * 获取扫描出来的实例化好的 bean，并且转好类型
     *
     * @param beanName bean 的名字
     * @param t        要转成的类型
     * @return 转好类型的 bean
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String beanName, Class<T> t) {
        return beanContainer.getBean(beanName, t);
    }
    
    // ==================== 私有方法 ====================
    
    private void start0(int port) {
        try {
            initServer(port);
        } catch (IOException e) {
            PORT = PORT + 1;
            listen();
        }
    }
    
    private void initServer(int port) throws IOException {
        Thread.currentThread().setName("Boss");
        Selector boss = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(boss, SelectionKey.OP_ACCEPT);
        
        printInitMessage(port, startTime);
        startServer(boss, serverSocketChannel);
    }
    
    private void startServer(Selector boss, ServerSocketChannel serverSocketChannel) throws IOException {
        final int workerNum = Runtime.getRuntime().availableProcessors();
        final Worker[] workers = new Worker[workerNum];
        
        // 创建 Worker 线程
        IntStream.range(0, workerNum).forEach(i -> 
            workers[i] = new Worker("worker-" + i, routeRegistry, beanContainer, staticFileRegistry, dependencyInjector)
        );
        
        AtomicInteger robin = new AtomicInteger(0);
        
        while (isStart) {
            if (boss.select() > 0) {
                Set<SelectionKey> selectionKeys = boss.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    
                    if (key.isAcceptable()) {
                        SocketChannel accept = serverSocketChannel.accept();
                        accept.configureBlocking(false);
                        workers[robin.getAndIncrement() % workers.length].register(accept);
                    }
                    
                    iterator.remove();
                }
            }
        }
    }
    
    private void getAndParseConfig() {
        final String port = getCfgInfo("port");
        if (Utils.isNotBlank(port)) PORT = Integer.parseInt(port);
        
        final String staticPath = getCfgInfo("static");
        if (Utils.isNotBlank(staticPath)) staticDirName = staticPath;
        
        final String logEnabled = getCfgInfo("log");
        Logger.init(logEnabled);
    }
    
    private void scanStaticFiles() {
        File rootFile = Utils.getResourceFile(this.staticDirName);
        if (rootFile != null) {
            List<File> files = Utils.scanFileToList(rootFile);
            files.forEach(this::createRouteToStaticFile);
        }
    }
    
    private void createRouteToStaticFile(File file) {
        String absolutePath = file.getAbsolutePath().replaceAll("\\\\", "/");
        String path = absolutePath.substring(absolutePath.indexOf(this.staticDirName) + this.staticDirName.length());
        
        if ("/index.html".equals(path)) path = "/";
        
        staticFileRegistry.register(path, file);
    }
    
    private void initScan(Class<?> clz) {
        BeanConfig config = new BeanConfig(clz);
        ComponentScanner scanner = new ComponentScanner(config.getBasePackage());
        scanner.scan();
        
        // 注册扫描到的类到 BeanContainer
        for (Class<?> clazz : scanner.getScannedClasses()) {
            registerBean(clazz);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void registerBean(Class<?> clz) {
        String beanName = "";
        Object bean = null;
        
        if (clz.isAnnotationPresent(Component.class)) {
            beanName = clz.getAnnotation(Component.class).value();
            bean = Utils.newInstance(clz);
        } else if (clz.isAnnotationPresent(Path.class)) {
            beanName = clz.getAnnotation(Path.class).value();
            bean = Utils.newInstance(clz);
        } else if (hasMapperAnnotation(clz)) {
            bean = createMapperProxy(clz);
            if (bean != null) {
                beanName = clz.getSimpleName();
            }
        }
        
        if (Utils.isNotBlank(beanName) && bean != null) {
            beanContainer.register(beanName, bean);
        }
    }
    
    private boolean hasMapperAnnotation(Class<?> clz) {
        if (!Utils.isReadExist("com.da.orm.annotation.Mapper")) {
            return false;
        }
        try {
            Class<Annotation> mapper = (Class<Annotation>) Class.forName("com.da.orm.annotation.Mapper");
            return clz.isAnnotationPresent(mapper);
        } catch (Exception e) {
            Logger.error(DApp.class, e);
            return false;
        }
    }
    
    private Object createMapperProxy(Class<?> clz) {
        try {
            if (!Utils.isReadExist("com.da.orm.core.MapperProxyFactory")) {
                return null;
            }
            
            Class<?> mapperProxyFactoryClz = Class.forName("com.da.orm.core.MapperProxyFactory");
            Object instance = mapperProxyFactoryClz.getConstructor().newInstance();
            Method getMapper = mapperProxyFactoryClz.getDeclaredMethod("getMapper", Class.class);
            getMapper.setAccessible(true);
            Object proxy = getMapper.invoke(instance, clz);
            getMapper.setAccessible(false);
            
            return proxy;
        } catch (Exception e) {
            Logger.error(DApp.class, e);
            return null;
        }
    }
    
    private void injectToComponentBeans() {
        for (String beanName : beanContainer.getBeanNames()) {
            Object bean = beanContainer.getBean(beanName).get();
            
            // 不是 Handler 接口实现的（即 Component Bean）
            if (!Utils.isInterface(bean.getClass(), Handler.class)) {
                dependencyInjector.injectToComponentBean(bean);
            }
        }
    }
    
    private void printInitMessage(int port, long startTime) {
        String[] banner = new String[]{
            "    .___                      ___.    ", "  __| _/____    __  _  __ ____\\_ |__  ",
            " / __ |\\__  \\   \\ \\/ \\/ // __ \\| __ \\ ", "/ /_/ | / __ \\_  \\     /\\  ___/| \\_\\ \\",
            "\\____ |(____  /   \\/\\_/  \\___  >___  /", "     \\/     \\/               \\/    \\/"
        };
        
        for (String s : banner) Logger.info(DApp.class, s);
        Logger.info(DApp.class, "NIO 服务器启动成功:");
        Logger.info(DApp.class, "\t> 本地访问：http://localhost:" + port);
        
        try {
            Logger.info(DApp.class, "\t> 网络访问：http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port);
        } catch (Exception e) {
            Logger.error(DApp.class, e);
        }
        
        Logger.info(DApp.class, "\t启动总耗时：" + (System.currentTimeMillis() - startTime) + "ms");
    }
    
    private String getCfgInfo(String propName) {
        final File configFile = Utils.getResourceFile("app.properties");
        try {
            if (configFile != null) {
                if (properties == null) {
                    properties = new Properties();
                    properties.load(Files.newInputStream(configFile.toPath()));
                }
                return properties.getProperty(propName);
            }
        } catch (IOException e) {
            Logger.error(DApp.class, e);
        }
        return "";
    }
}
