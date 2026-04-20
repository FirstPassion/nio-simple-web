# DApp - 轻量级 Java NIO Web 服务器

[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

一个基于 **Java NIO** 构建的高性能、轻量级 Web 服务器框架，支持 HTTP/1.1、WebSocket、静态文件服务以及基于注解的 IOC 容器和路由管理。

## 📦 项目结构

```
nio-simple-web/
├── src/main/java/com/da/web/
│   ├── annotations/          # 注解定义
│   │   ├── Component.java   # 组件标记注解
│   │   ├── Path.java         # 路由路径注解
│   │   └── Inject.java       # 依赖注入注解
│   ├── config/               # 配置相关
│   │   └── ServerConfig.java # 服务器配置接口
│   ├── constant/             # 常量定义
│   │   ├── HttpStatus.java   # HTTP 状态码
│   │   └── ContentTypes.java # Content-Type 常量
│   ├── core/                 # 核心模块
│   │   ├── DApp.java         # 启动入口类
│   │   ├── Context.java      # 请求上下文
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
│   │   ├── ComponentScanner.java# 组件扫描器
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

**Happy Coding!** 🎉
