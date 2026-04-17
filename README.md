# DApp - 轻量级 Java NIO Web 服务器

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

一个基于 **Java NIO** 构建的高性能、轻量级 Web 服务器框架，支持 HTTP/1.1、WebSocket、静态文件服务以及基于注解的 IOC 容器和路由管理。

## ✨ 特性

- 🚀 **高性能 NIO**: 基于 `Selector` + `ServerSocketChannel` 的非阻塞 IO 模型
- 🌐 **完整 HTTP 支持**: 支持 HTTP/1.1 协议，包括 GET、POST、PUT、DELETE 等方法
- 🔌 **WebSocket 支持**: 内置 WebSocket 协议升级和消息处理
- 📁 **静态文件服务**: 高效的静态资源映射和缓存机制
- 🏗️ **IOC 容器**: 基于注解的依赖注入，支持 `@Component`、`@Inject`、`@Value`
- 🛣️ **注解路由**: 使用 `@Path`、`@Get`、`@Post` 等注解定义 RESTful API
- 🔧 **模块化设计**: 清晰的分层架构，易于扩展和维护（2024 重构版）
- ⚡ **零外部依赖**: 纯 Java 实现，无需任何第三方库

## 📦 项目结构

```
src/main/java/com/da/web/
├── annotations/          # 注解定义
│   ├── Component.java
│   ├── Path.java
│   ├── Get.java, Post.java, Put.java, Delete.java
│   ├── Inject.java
│   └── Value.java
├── bean/                 # IOC 容器核心
│   └── BeanContainer.java
├── config/               # 配置类
│   └── AppConfig.java
├── core/                 # 服务器核心引擎
│   ├── DApp.java         # 启动入口（重构后精简至 344 行）
│   ├── ServerEngine.java # 服务器引擎（重构新增）
│   ├── WorkerPool.java   # 工作线程池（重构新增）
│   ├── Worker.java       # 工作线程（重构新增独立类）
│   └── Context.java      # 请求上下文
├── function/             # 函数式接口
│   └── RouteHandler.java
├── http/                 # HTTP 协议解析
│   ├── HttpRequest.java
│   ├── HttpResponse.java
│   └── HttpUtil.java
├── ioc/                  # IOC 模块（重构新增）
│   ├── ComponentScanner.java    # 组件扫描器
│   ├── DependencyInjector.java  # 依赖注入器
│   └── BeanConfig.java          # Bean 配置
├── router/               # 路由管理
│   ├── RouteRegistry.java       # 路由注册表
│   ├── RouteMapping.java        # 路由映射
│   └── RequestDispatcher.java   # 请求分发器（重构新增）
├── io/                   # IO 工具
│   └── StaticFileRegistry.java  # 静态文件注册
└── util/                 # 工具类
    └── StringUtil.java
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+ (可选，用于构建)

### 编译项目

```bash
# 创建输出目录
mkdir -p target/classes

# 分步编译（解决依赖顺序问题）
javac -d target/classes \
  src/main/java/com/da/web/annotations/*.java \
  src/main/java/com/da/web/util/*.java \
  src/main/java/com/da/web/function/*.java \
  src/main/java/com/da/web/http/*.java \
  src/main/java/com/da/web/io/*.java \
  src/main/java/com/da/web/bean/*.java \
  src/main/java/com/da/web/router/*.java \
  src/main/java/com/da/web/ioc/*.java \
  src/main/java/com/da/web/core/*.java \
  src/main/java/com/da/web/config/*.java
```

### 运行示例

```bash
# 运行主程序（默认端口 8080）
java -cp target/classes com.da.web.core.DApp

# 指定端口运行
java -cp target/classes com.da.web.core.DApp 9090
```

### 开发你的第一个应用

#### 1. 创建 Controller

```java
package com.example.controller;

import com.da.web.annotations.*;
import com.da.web.core.Context;

@Path("/api")
@Component
public class UserController {

    @Get("/users")
    public void getUsers(Context ctx) {
        ctx.json("{\"users\": [{\"id\": 1, \"name\": \"Alice\"}]}");
    }

    @Post("/users")
    public void createUser(Context ctx) {
        String body = ctx.body();
        // 处理用户创建逻辑
        ctx.status(201).json("{\"message\": \"User created\"}");
    }

    @Get("/users/:id")
    public void getUserById(Context ctx) {
        String id = ctx.pathParam("id");
        ctx.json("{\"id\": " + id + ", \"name\": \"Alice\"}");
    }
}
```

#### 2. 创建 WebSocket 处理器

```java
package com.example.websocket;

import com.da.web.annotations.*;
import com.da.web.core.Context;

@Path("/ws")
@Component
public class ChatWebSocket {

    @OnOpen
    public void onOpen(Context ctx) {
        System.out.println("Client connected: " + ctx.sessionId());
    }

    @OnMessage
    public void onMessage(Context ctx, String message) {
        // 广播消息给所有连接的客户端
        ctx.broadcast("User says: " + message);
    }

    @OnClose
    public void onClose(Context ctx) {
        System.out.println("Client disconnected: " + ctx.sessionId());
    }
}
```

#### 3. 配置静态文件

```java
package com.example.config;

import com.da.web.config.AppConfig;
import com.da.web.io.StaticFileRegistry;

public class MyConfig implements AppConfig {
    
    @Override
    public void configure(StaticFileRegistry registry) {
        // 映射静态资源目录
        registry.addStaticFiles("/static", "src/main/resources/static");
        registry.addStaticFiles("/assets", "src/main/resources/assets");
        
        // 设置默认首页
        registry.setWelcomeFile("index.html");
    }
}
```

#### 4. 启动服务器

```java
package com.example;

import com.da.web.core.DApp;

public class Application {
    public static void main(String[] args) {
        // 启动服务器，自动扫描 com.example 包下的组件
        DApp.run("com.example", 8080);
    }
}
```

## 📖 核心概念

### 注解说明

| 注解 | 作用域 | 描述 |
|------|--------|------|
| `@Component` | Class | 标记为 IOC 容器管理的组件 |
| `@Path` | Class | 定义类的路由前缀 |
| `@Get/@Post/@Put/@Delete` | Method | 定义 HTTP 方法路由 |
| `@Inject` | Field | 自动注入依赖的 Bean |
| `@Value` | Field | 注入配置值或环境变量 |
| `@OnOpen/@OnMessage/@OnClose` | Method | WebSocket 事件处理 |

### 依赖注入

框架支持三种注入方式：

```java
@Component
public class UserService {
    
    // 注入其他 Bean
    @Inject
    private UserRepository userRepository;
    
    // 注入配置值
    @Value("${app.name:MyApp}")
    private String appName;
    
    // 注入环境变量
    @Value("${JAVA_HOME}")
    private String javaHome;
}
```

### 请求上下文 (Context)

`Context` 对象封装了完整的请求和响应信息：

```java
@Get("/example")
public void handle(Context ctx) {
    // 获取请求参数
    String name = ctx.query("name");
    String header = ctx.header("Authorization");
    String body = ctx.body();
    
    // 设置响应
    ctx.status(200)
       .header("Content-Type", "application/json")
       .json("{\"message\": \"success\"}");
       
    // 重定向
    // ctx.redirect("/home");
    
    // 返回文件
    // ctx.file("path/to/file.pdf");
}
```

## 🏗️ 架构设计

### 模块化架构（2024 重构版）

```
┌─────────────────────────────────────────────────┐
│                    DApp                         │
│              (启动配置 & 流程编排)               │
│              重构后：344 行 (原 667 行)           │
└───────────────┬─────────────────────────────────┘
                │
        ┌───────┼───────────┐
        │       │           │
        ▼       ▼           ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ ServerEngine │  │  IOC Module  │  │ Router Module│
│              │  │              │  │              │
│ • Selector   │  │ • Scanner    │  │ • Registry   │
│ • Accept     │  │ • Injector   │  │ • Dispatcher │
│ • LoadBalance│  │ • Container  │  │ • Matching   │
└──────┬───────┘  └──────────────┘  └──────┬───────┘
       │                                   │
       ▼                                   ▼
┌──────────────┐                  ┌──────────────┐
│  WorkerPool  │                  │ RouteMapping │
└──────┬───────┘                  └──────────────┘
       │
       ▼
┌──────────────┐
│    Worker    │
│              │
│ • Read       │
│ • Parse      │
│ • Dispatch   │
│ • Write      │
└──────────────┘
```

### 请求处理流程

```
1. Client Connect → Boss Selector (Accept)
2. Register to Worker → Worker Selector (Read)
3. Parse HttpRequest → RouteDispatcher
4. Match Route → Find Handler
5. Invoke Handler → Build Response
6. Write Response → Client
```

### 重构亮点

- **DApp 精简**: 从 667 行降至 344 行，减少 48% 代码量
- **职责分离**: IOC、路由、服务器引擎完全解耦
- **独立 Worker**: 从内部类提取为独立类，便于测试和扩展
- **统一扫描**: ComponentScanner 统一处理包扫描逻辑
- **智能注入**: DependencyInjector 统一处理依赖注入

## 🔧 配置选项

通过实现 `AppConfig` 接口自定义服务器行为：

```java
public class CustomConfig implements AppConfig {
    
    @Override
    public int getPort() {
        return 9090;
    }
    
    @Override
    public int getWorkerThreads() {
        return Runtime.getRuntime().availableProcessors() * 2;
    }
    
    @Override
    public int getMaxConnections() {
        return 10000;
    }
    
    @Override
    public long getConnectionTimeout() {
        return 30000; // 30 秒
    }
    
    @Override
    public void configure(StaticFileRegistry registry) {
        registry.addStaticFiles("/", "public");
        registry.setCacheSeconds(3600);
    }
}
```

## 📊 性能特点

- **非阻塞 IO**: 单线程可处理数千并发连接
- **零拷贝**: 静态文件传输使用 `FileChannel.transferTo()`
- **连接复用**: 支持 HTTP Keep-Alive
- **负载均衡**: Worker 线程间均匀分配连接
- **内存高效**: 缓冲区复用，减少 GC 压力

## 🧪 测试建议

由于框架采用模块化设计，各组件可独立测试：

```java
// 测试 IOC 容器
@Test
public void testComponentScanner() {
    ComponentScanner scanner = new ComponentScanner("com.example");
    List<Class<?>> components = scanner.scan();
    assertEquals(5, components.size());
}

// 测试路由匹配
@Test
public void testRouteMatching() {
    RouteRegistry registry = new RouteRegistry();
    registry.register(UserController.class);
    
    RouteMapping mapping = registry.findRoute("GET", "/api/users");
    assertNotNull(mapping);
}

// 测试依赖注入
@Test
public void testDependencyInjection() {
    BeanContainer container = new BeanContainer();
    DependencyInjector injector = new DependencyInjector(container);
    
    UserService service = container.getBean(UserService.class);
    assertNotNull(service.getUserRepository());
}
```

## 🔄 重构迁移指南

### 从旧版本迁移

如果你使用的是重构前的版本，主要变化：

1. **DApp API 变化**:
   ```java
   // 旧版本
   DApp app = new DApp(MyClass.class);
   
   // 新版本
   DApp.run("com.example.package", 8080);
   ```

2. **Worker 不再是内部类**: 现在通过 `WorkerPool` 管理

3. **路由管理**: 现在统一使用 `RouteRegistry` 和 `RequestDispatcher`

4. **Bean 管理**: 现在统一使用 `BeanContainer` 和 `ComponentScanner`

### 兼容性

- ✅ 所有注解保持兼容
- ✅ Context API 保持兼容
- ✅ HTTP/WebSocket 功能保持兼容
- ✅ 配置文件格式保持兼容

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

## 🙏 致谢

- 灵感来源于 Netty、Jetty 等优秀框架
- 感谢所有贡献者的支持和反馈

---

**Happy Coding!** 🎉
