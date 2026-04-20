# DApp - 轻量级 Java NIO Web 服务器

[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8+-blue.svg)](https://openjdk.java.net/)

一个基于 **Java NIO** 构建的高性能、轻量级 Web 服务器框架，支持 HTTP/1.1、WebSocket、静态文件服务以及基于注解的 IOC 容器和路由管理。**兼容 Java 8+**。

## ✨ 特性

- 🚀 **高性能**: 基于 Java NIO 异步非阻塞模型
- 📝 **注解驱动**: 支持 `@Get`、`@Post`、`@Put`、`@Delete` 等 HTTP 方法注解
- 🔗 **灵活路由**: 支持路径变量、查询参数、精确匹配优先
- 🍪 **Cookie/Session**: 内置 Cookie 操作和 Session 管理（30 分钟超时）
- 🔄 **重定向**: 支持 302 临时重定向和 301 永久重定向
- 📦 **参数绑定**: 自动绑定 `@PathParam`、`@QueryParam`、`@BodyParam`、`@Header`
- 🧩 **IOC 容器**: 自动扫描 `@Component`、`@Path` 并注入依赖
- 🌐 **WebSocket**: 完整的 WebSocket 支持
- 📡 **SSE**: 服务端发送事件支持
- 📄 **静态文件**: 内置静态文件服务

## 📦 项目结构

```
nio-simple-web/
├── src/main/java/com/da/web/
│   ├── annotations/          # 注解定义
│   │   ├── Component.java    # 组件标记注解
│   │   ├── Path.java         # 路由路径注解
│   │   ├── Inject.java       # 依赖注入注解
│   │   ├── Get.java          # GET 请求映射
│   │   ├── Post.java         # POST 请求映射
│   │   ├── Put.java          # PUT 请求映射
│   │   ├── Delete.java       # DELETE 请求映射
│   │   ├── RequestMapping.java # 通用请求映射
│   │   ├── PathParam.java    # 路径变量绑定
│   │   ├── QueryParam.java   # 查询参数绑定
│   │   ├── BodyParam.java    # 请求体参数绑定
│   │   └── Header.java       # 请求头绑定
│   ├── config/               # 配置相关
│   │   └── ServerConfig.java # 服务器配置接口
│   ├── constant/             # 常量定义
│   │   ├── HttpStatus.java   # HTTP 状态码
│   │   └── ContentTypes.java # Content-Type 常量
│   ├── core/                 # 核心模块
│   │   ├── DApp.java         # 启动入口类
│   │   ├── Context.java      # 请求上下文（含 Cookie/Session/重定向）
│   │   └── Worker.java       # 工作线程
│   ├── exception/            # 异常类
│   ├── function/             # 函数式接口
│   │   ├── Handler.java      # 请求处理器
│   │   └── WsListener.java   # WebSocket 监听器
│   ├── http/                 # HTTP 协议处理
│   │   ├── HttpParser.java   # HTTP 解析器
│   │   ├── HttpRequest.java  # 请求对象
│   │   ├── HttpResponse.java # 响应对象
│   │   ├── MultiValueMap.java# 多值映射
│   │   └── JsonParser.java   # JSON 解析器
│   ├── io/                   # IO 操作
│   │   └── StaticFileRegistry.java # 静态文件注册
│   ├── ioc/                  # IOC 容器
│   │   ├── BeanContainer.java    # Bean 容器
│   │   ├── ComponentScanner.java # 组件扫描器（自动路由注册）
│   │   ├── DependencyInjector.java# 依赖注入器
│   │   └── BeanConfig.java   # Bean 配置接口
│   ├── router/               # 路由模块
│   │   ├── RouteRegistry.java   # 路由注册表
│   │   ├── RouteMapping.java    # 路由映射
│   │   └── RequestDispatcher.java# 请求分发器
│   ├── util/                 # 工具类
│   │   ├── Logger.java       # 日志工具
│   │   └── Utils.java        # 通用工具
│   ├── websocket/            # WebSocket 支持
│   │   └── WebSocketManager.java
│   └── sse/                  # SSE 支持
│       └── SSEManager.java
├── src/main/resources/
│   └── static/               # 静态资源目录
├── src/test/java/            # 单元测试
├── pom.xml                   # Maven 配置
└── README.md
```

## 🚀 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.da.web</groupId>
    <artifactId>nio-simple-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 创建控制器

```java
import com.da.web.annotations.*;
import com.da.web.core.Context;

@Path("/user")
@Component
public class UserController {
    
    @Get("/{id}")
    public void getUser(@PathParam("id") Integer id, Context ctx) {
        ctx.sendJson("{\"id\":" + id + ",\"name\":\"张三\"}");
    }
    
    @Post("/create")
    public void createUser(@BodyParam("username") String username, 
                          @BodyParam("email") String email, 
                          Context ctx) {
        ctx.sendJson("{\"status\":\"created\",\"username\":\"" + username + "\"}");
    }
    
    @Get("/login")
    public void login(@QueryParam("username") String username, Context ctx) {
        // 设置 Session
        ctx.setSessionAttribute("currentUser", username);
        // 设置 Cookie
        ctx.setCookie("rememberMe", "true", 3600, "/");
        // 重定向
        ctx.redirect("/dashboard");
    }
    
    @Get("/logout")
    public void logout(Context ctx) {
        // 使 Session 失效
        ctx.invalidateSession();
        // 删除 Cookie
        ctx.deleteCookie("rememberMe");
        ctx.send("已退出登录");
    }
}
```

### 3. 启动服务器

```java
import com.da.web.core.DApp;

public class Main {
    public static void main(String[] args) {
        DApp app = new DApp(8080);
        
        // 手动注册路由（优先级高于注解路由）
        app.use("/health", "GET", ctx -> ctx.send("OK"));
        
        // 启动并自动扫描 @Component 和 @Path
        app.start();
    }
}
```

## 📖 API 文档

### 注解说明

| 注解 | 位置 | 说明 |
|------|------|------|
| `@Path("/base")` | 类级别 | 定义基础路径 |
| `@Get("/path")` | 方法级别 | 映射 GET 请求，支持路径变量如 `/{id}` |
| `@Post("/path")` | 方法级别 | 映射 POST 请求 |
| `@Put("/path")` | 方法级别 | 映射 PUT 请求 |
| `@Delete("/path")` | 方法级别 | 映射 DELETE 请求 |
| `@RequestMapping(value="/path", method="GET")` | 方法级别 | 通用请求映射 |
| `@PathParam("name")` | 参数级别 | 绑定路径变量 |
| `@QueryParam("name")` | 参数级别 | 绑定查询参数 |
| `@BodyParam("field")` | 参数级别 | 绑定请求体字段 |
| `@Header("name")` | 参数级别 | 绑定请求头 |
| `@Component` | 类级别 | 标记为组件，自动扫描 |
| `@Inject` | 字段/构造器 | 依赖注入 |

### Context 方法

#### 响应方法
- `send(String content)` - 发送文本响应
- `sendJson(String json)` - 发送 JSON 响应
- `sendHtml(String html)` - 发送 HTML 响应
- `sendStatus(int statusCode)` - 仅发送状态码

#### 重定向
- `redirect(String location)` - 302 临时重定向
- `redirectPermanent(String location)` - 301 永久重定向

#### Cookie 操作
- `setCookie(String name, String value)` - 设置 Cookie
- `setCookie(String name, String value, int maxAge, String path)` - 设置带过期时间的 Cookie
- `getCookie(String name)` - 获取 Cookie 值
- `deleteCookie(String name)` - 删除 Cookie

#### Session 管理
- `getSession()` - 获取或创建 Session
- `getSession(boolean create)` - 获取 Session，可选是否创建
- `setSessionAttribute(String name, Object value)` - 设置 Session 属性
- `getSessionAttribute(String name)` - 获取 Session 属性
- `removeSessionAttribute(String name)` - 移除 Session 属性
- `invalidateSession()` - 使 Session 失效

#### 路径变量
- `getPathVariable(String name)` - 获取路径变量值
- `getPathVariables()` - 获取所有路径变量（Map）

#### 请求信息
- `getMethod()` - 获取 HTTP 方法
- `getPath()` - 获取请求路径
- `getQueryParam(String name)` - 获取查询参数
- `getHeader(String name)` - 获取请求头
- `getBody()` - 获取请求体

## 🧪 运行测试

```bash
# 编译项目（兼容 Java 8+）
mvn clean compile

# 运行所有测试
mvn test

# 查看测试报告
cat TEST_REPORT.md
```

## ⚙️ 路由优先级

1. **手动注册优先**: `DApp.use()` 注册的路由最先生效
2. **精确匹配优先**: 精确路径匹配优于路径变量匹配
3. **注解路由**: `@Get`、`@Post` 等注解自动注册的路由

示例：
```java
// 手动注册，优先级最高
app.use("/user/profile", "GET", ctx -> ctx.send("手动路由"));

// 注解路由，会被上面的手动路由覆盖
@Get("/user/profile")
public void profile(Context ctx) { ... }

// 路径变量路由，优先级最低
@Get("/user/{id}")
public void getUser(@PathParam("id") Integer id, Context ctx) { ... }
```

## 📝 注意事项

- **Java 版本**: 兼容 Java 8 及以上版本
- **Session 存储**: 当前使用内存存储，重启后丢失，生产环境建议集成 Redis
- **线程安全**: Controller 是单例的，请注意并发问题
- **路径变量**: 支持多个路径变量，如 `/user/{userId}/order/{orderId}`

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

**Happy Coding!** 🎉
