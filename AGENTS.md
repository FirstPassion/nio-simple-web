# AGENTS.md - 项目开发指南

> 本文档专为 AI 编程助手（如 OpenCode）设计，提供完整的项目结构、构建流程、核心 API 和使用规范说明。

---

## 📦 项目概述

**项目名称**: nio-simple-web  
**当前版本**: 1.0.2  
**Java 版本**: Java 8 (严格兼容，不使用 Java 9+ API)  
**项目类型**: 基于 NIO 的轻量级 Web 服务器框架  
**包管理**: Maven

### 核心特性
- 基于 Java NIO 实现的高性能异步服务器
- 支持 WebSocket 协议
- 自动端口探测（从 8080 开始递增）
- 依赖注入容器（支持 `@Component`、`@Path`、`@Inject` 注解）
- 静态资源服务
- 路由系统：显式路由 → 静态文件 → Bean → 404

---

## 🔨 构建命令

### 基础构建
```bash
# 清理并打包，生成 JAR 到 target/ 目录
mvn clean package

# 仅编译
mvn compile

# 运行测试
mvn test
```

### 发布到本地仓库
```bash
# 发布到本地 repo/ 目录（用于 redistribute）
mvn deploy
```

### 构建输出
- **JAR 包位置**: `target/nio-simple-web-1.0.2.jar`
- **本地仓库位置**: `repo/com/da/nio-simple-web/1.0.2/`
- **测试报告**: `target/surefire-reports/`

---

## 📁 项目结构

```
/workspace/
├── src/main/java/com/da/web/       # 主源码目录
│   ├── annotations/                # 注解定义 (@Component, @Path, @Inject)
│   ├── bean/                       # Bean 容器实现
│   ├── config/                     # 服务器配置
│   ├── constant/                   # 常量定义 (HttpStatus, ContentTypes)
│   ├── core/                       # 核心类 (DApp 启动类, Context 上下文)
│   ├── enums/                      # 枚举类型
│   ├── exception/                  # 自定义异常
│   ├── function/                   # 函数式接口 (Handler, WsListener)
│   ├── http/                       # HTTP 相关 (请求/响应解析，JSON 处理)
│   ├── io/                         # IO 工具
│   ├── router/                     # 路由管理
│   ├── util/                       # 工具类
│   └── websocket/                  # WebSocket 支持
├── src/main/resources/
│   ├── app.properties              # 服务器配置文件 (port, static 目录等)
│   └── static/                     # 静态资源目录 (HTML, CSS, JS 等)
├── src/test/java/com/da/web/       # 测试代码目录
│   ├── http/                       # HTTP 相关测试
│   └── util/                       # 工具类测试
├── repo/                           # 本地 Maven 仓库 (deploy 输出)
├── pom.xml                         # Maven 配置文件
└── AGENTS.md                       # 本文档
```

---

## 🚀 核心入口点

### 1. 主启动类
**类名**: `com.da.web.core.DApp`

#### 构造方式
```java
// 方式 1: 空参构造（不扫描 Bean，需手动注册路由）
DApp app = new DApp();

// 方式 2: 指定配置类（自动扫描包及其子包）
DApp app = new DApp(MyConfig.class);
```

#### 启动方法
```java
// 使用默认端口 8080（自动探测可用端口）
app.listen();

// 指定端口
app.listen(3000);
```

#### 路由注册
```java
// 手动注册路由
app.use("/hello", ctx -> {
    ctx.send("Hello World");
});
```

### 2. 配置文件
**路径**: `src/main/resources/app.properties`

```properties
# 服务器端口（可选，默认 8080）
port=8080

# 静态资源目录名称（可选，默认 static）
static=static
```

### 3. 静态资源
**默认路径**: `src/main/resources/static/`  
可通过配置文件中的 `static` 属性修改

---

## 🔑 核心 API 说明

### 注解系统

| 注解 | 作用 | 示例 |
|------|------|------|
| `@Component` | 标记组件类，由容器管理 | `@Component("myService")` |
| `@Path` | 标记路由处理器，路径与 Bean 名称绑定 | `@Path("/user")` |
| `@Inject` | 依赖注入，支持基本类型和 Bean | `@Inject("myService")` |

### Context 上下文对象

`Context` 是请求处理的核心对象，提供以下方法：

```java
// 发送响应
ctx.send(String content);           // 发送文本
ctx.sendHtml(String html);          // 发送 HTML
ctx.sendJson(Object obj);           // 发送 JSON
ctx.send(File file);                // 发送文件
ctx.send(byte[] data, int status);  // 发送字节数组

// 获取请求信息
ctx.getUrl();                       // 获取请求 URL
ctx.getMethod();                    // 获取 HTTP 方法
ctx.getParams();                    // 获取请求参数 Map
ctx.getHeader(String name);         // 获取请求头

// WebSocket 支持
ctx.sendWsMessage(String msg);      // 发送 WebSocket 消息
```

### Handler 函数式接口

```java
@FunctionalInterface
public interface Handler {
    void callback(Context ctx) throws Exception;
}
```

### WebSocket 监听器

```java
@FunctionalInterface
public interface WsListener {
    void onMessage(Context ctx, String message) throws Exception;
}
```

---

## 🧪 测试套件

### 测试文件清单
1. **UtilsTest.java** - 工具类测试（12 个测试用例）
2. **MultiValueMapTest.java** - 多值 Map 测试（9 个测试用例）
3. **JsonParserTest.java** - JSON 解析器测试（14 个测试用例）
4. **HttpParserTest.java** - HTTP 解析器测试（11 个测试用例）

### 运行测试
```bash
# 运行所有测试
mvn test

# 查看测试报告
cat target/surefire-reports/*.txt
```

### 测试状态
- **总测试数**: 46
- **通过率**: 100%
- **最后验证**: 全部通过

---

## ⚠️ 重要注意事项

### Java 8 兼容性
- **严格限制**: 项目必须完全兼容 Java 8
- **禁止使用**: Java 9+ 的 API（如 `InputStream.readAllBytes()`）
- **已修复**: `Context.java` 中的 `readAllBytes()` 已替换为自定义 `readFully()` 方法
- **验证方式**: 编译时使用 `-Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8`

### 端口探测机制
- 起始端口：8080
- 如果端口被占用，自动递增尝试下一个端口
- 适用于开发环境快速启动

### 路由优先级顺序
1. **显式注册的路由** (`app.use()`)
2. **静态资源文件** (匹配 `staticFiles` Map)
3. **Bean 池中的 Bean** (匹配 `@Path` 注解)
4. **404 Not Found**

### Bean 注入规则
- `@Path` 注解的 Bean 会自动注入请求参数到同名字段
- `@Inject` 注解支持：
  - 基本类型和 String（从配置或参数注入）
  - Bean 池中的其他 Bean
- 字段无需 getter/setter，直接反射注入

### 跨平台路径处理
- **Windows**: 使用 `\\` 分隔符
- **Unix/Linux/Mac**: 使用 `/` 分隔符
- 工具类 `Utils` 自动处理路径转换

---

## 🛠️ 开发指南（给 AI 助手）

### 代码修改规范
1. **Java 版本检查**: 任何新代码必须使用 Java 8 API
2. **测试覆盖**: 修改核心逻辑时需更新对应测试
3. **中文注释**: 公共方法和复杂逻辑需添加中文注释
4. **版本管理**: 修改后更新 `pom.xml` 中的版本号

### 常见问题排查
```bash
# 检查 Java 版本兼容性
javac -source 1.8 -target 1.8 src/main/java/...

# 查看编译错误
mvn clean compile -X

# 验证 JAR 包内容
jar tf target/nio-simple-web-1.0.2.jar
```

### 添加新功能步骤
1. 在对应模块目录下创建新类
2. 编写单元测试（`src/test/java/`）
3. 运行测试确保通过：`mvn test`
4. 更新版本号：`pom.xml` 中的 `<version>`
5. 打包发布：`mvn clean package deploy`

---

## 📊 项目统计

- **源码文件数**: 26 个 Java 文件
- **测试文件数**: 4 个测试类
- **测试用例数**: 46 个
- **支持协议**: HTTP/1.1, WebSocket
- **线程模型**: Boss-Worker 模式（Worker 数量 = CPU 核心数）

---

## 📝 版本历史

| 版本 | 日期 | 主要变更 |
|------|------|----------|
| 1.0.1 | - | 初始版本 |
| 1.0.2 | 当前 | 修复 Java 8 兼容性，完善测试套件，添加中文注释 |

---

## 🔗 相关资源

- **Maven 坐标**: `com.da:nio-simple-web:1.0.2`
- **本地仓库路径**: `/workspace/repo/com/da/nio-simple-web/`
- **配置文件示例**: `src/main/resources/app.properties`
- **测试示例**: `src/test/java/com/da/web/util/UtilsTest.java`

---

> **最后更新**: 2024 年  
> **维护者**: Da  
> **文档用途**: 指导 AI 编程助手理解和开发本项目