# NIO Simple Web Server

基于 Java NIO 实现的轻量级 Web 服务器，支持 HTTP 和 WebSocket 协议。

## 特性

- ✅ 基于 NIO 的非阻塞 IO 模型
- ✅ 支持 HTTP/1.1 协议（完整实现）
- ✅ 支持 HTTP 请求处理（GET/POST/PUT/DELETE 等）
- ✅ **新增** 递归下降法实现的 HTTP 解析器
- ✅ **新增** 支持 `application/json` 自动解析
- ✅ **新增** 支持 `application/x-www-form-urlencoded` 表单解析
- ✅ **新增** 支持 `multipart/form-data` 文件上传解析
- ✅ **新增** 完整的请求头管理（MultiValueMap）
- ✅ **新增** 响应构建器模式（HttpResponse）
- ✅ 支持静态资源服务
- ✅ 支持注解驱动的路由注册（@Path）
- ✅ 支持依赖注入（@Component, @Inject）
- ✅ 完整的 WebSocket 支持（连接管理、消息发送、广播）
- ✅ 配置文件支持（app.properties）
- ✅ 日志开关配置（log=true/false）
- ✅ Bean 容器管理

## 快速开始

### Maven 依赖

```xml
<!--添加 nio-simple-web 仓库地址-->
<repositories>
    <repository>
        <id>nio-simple-web</id>
        <url>https://raw.githubusercontent.com/daHaoShuai/nio-simple-web/main/repo</url>
    </repository>
</repositories>
```

```xml
<!--添加依赖-->
<dependencies>
    <dependency>
        <groupId>com.da</groupId>
        <artifactId>nio-simple-web</artifactId>
        <version>1.0.2</version>
    </dependency>
</dependencies>
```

### 基础示例

```java
public class App {
    public static void main(String[] args) {
        DApp app = new DApp();
        app.use("/", ctx -> ctx.sendHtml("<h1>hello world</h1>"));
        app.listen();
    }
}
```

### 注解方式注册路由

```java
public class App {
    public static void main(String[] args) {
        // 自动扫描加了@Path 注解并且实现了 Handler 接口的类，注册到路由表
        DApp app = new DApp(App.class);
        app.listen();
        // 获取配置文件中的信息
        System.out.println(app.getCfgInfo("port"));
    }
}

@Path("/hello")
public class IndexController implements Handler {
    @Override
    public void callback(Context ctx) {
        ctx.send("hello");
    }
}
```

### 配置文件

在 `resources/app.properties` 中配置：

```properties
# 端口号 (默认从 8080 开始找可用的端口)
port=8083
# 静态资源目录 (默认为 resources/static)
static=aaa/aaa
# 日志开关 (默认为 true)
log=true
```

### 依赖注入示例

```java
public class App {
    public static void main(String[] args) {
        DApp app = new DApp(App.class);
        app.use("/", ctx -> ctx.sendHtml("<h1>hello world</h1>"));
        
        // @Component 注册的 bean
        Dog dog = app.getBean("dog", Dog.class);
        System.out.println(dog);
        
        // @path 注解用路径表示 beanName
        IndexController bean = (IndexController) app.getBean("/hello");
        System.out.println(bean);
        
        app.listen();
    }
}

@Component("dog")
public class Dog {
    @Inject("小黄")
    private String name;

    @Override
    public String toString() {
        return "Dog{" +
                "name='" + name + '\'' +
                '}';
    }
    
    // getters and setters...
}

@Component("user")
public class User {
    @Inject("杰哥")
    private String name;
    
    @Inject("dog")
    private Dog dog;
    
    // getters and setters...
}

@Path("/hello")
public class IndexController implements Handler {
    @Inject("user")
    private User user;

    @Inject("20")
    private int age;

    // 如果没有@Inject 注解会尝试从请求参数中注入值
    private String name;
    private int sex;

    @Override
    public void callback(Context ctx) {
        ctx.send(user.toString() + " name = " 
                + name + " age = " + age + " sex = " + sex);
    }
}
```

### 处理 JSON 数据

```java
public class App {
    public static void main(String[] args) {
        final DApp app = new DApp(App.class);
        app.use("/aa", ctx -> {
            // post 请求传来的 json 字符串（旧方式，兼容）
            final Object o = ctx.getParams().get("request-json-data");
            System.out.println(o);
            
            // 新方式：使用 getBodyAsJson() 获取 JSON 字符串
            String jsonStr = ctx.getBodyAsJson();
            System.out.println("JSON: " + jsonStr);
            
            // 或者使用 getBodyAs(Map.class) 直接解析为 Map
            Map<String, Object> bodyMap = ctx.getBodyAs(Map.class);
            System.out.println("Parsed: " + bodyMap);
            
            ctx.send("ok");
        });
        app.listen();
    }
}
```

### 访问请求头和请求体

```java
public class App {
    public static void main(String[] args) {
        DApp app = new DApp();
        
        app.use("/info", ctx -> {
            // 获取原始 HttpRequest 对象
            HttpRequest request = ctx.getRequest();
            
            // 获取请求头
            String userAgent = ctx.getHeader("User-Agent");
            String contentType = ctx.getContentType();
            
            // 获取查询参数
            String name = (String) ctx.getParams().get("name");
            
            // 获取请求体参数（表单或 JSON 自动解析）
            String username = (String) ctx.getBodyParam("username");
            String password = (String) ctx.getBodyParam("password");
            
            // 获取原始请求体字节
            byte[] rawBody = ctx.getRawBody();
            
            StringBuilder sb = new StringBuilder();
            sb.append("Method: ").append(ctx.getMethod()).append("<br>");
            sb.append("URL: ").append(ctx.getUrl()).append("<br>");
            sb.append("User-Agent: ").append(userAgent).append("<br>");
            sb.append("Content-Type: ").append(contentType).append("<br>");
            sb.append("Name (query): ").append(name).append("<br>");
            sb.append("Username (body): ").append(username).append("<br>");
            
            ctx.sendHtml(sb.toString());
        });
        
        app.listen();
    }
}
```

### 实体类转 JSON

```java
public class App {
    public static void main(String[] args) {
        // 把实体类列表转成 json 数组形式字符串
        System.out.println(Utils.parseListToJsonString(
            Arrays.asList(new User("a"), new User("b"), new User("c"))
        ));
        // 输出：{"User":[{"name":"a"},{"name":"b"},{"name":"c"}]}
    }
}

class User {
    public String name;
    public User(String name) {
        this.name = name;
    }
}
```

## WebSocket 功能

### 完整示例

#### 1. 创建 WebSocket 监听器

```java
import com.da.web.annotations.Component;
import com.da.web.core.Context;
import com.da.web.function.WsListener;
import com.da.web.websocket.WebSocketManager;

// 注册到容器，并且实现 WsListener 接口
@Component("ws")
public class WsImpl implements WsListener {

    @Override
    public void onOpen(Context ctx) throws Exception {
        System.out.println("新客户端连接！当前连接数：" + WebSocketManager.getConnectionCount());
        // 发送欢迎消息
        WebSocketManager.sendMessage(ctx.getChannel(), "欢迎连接 WebSocket 服务器！");
    }

    @Override
    public void onMessage(Context ctx, String message) throws Exception {
        System.out.println("收到客户端消息：" + message);
        
        // 回复消息
        WebSocketManager.sendMessage(ctx.getChannel(), "服务器收到：" + message);
        
        // 或者广播给所有客户端
        // WebSocketManager.broadcast("广播消息：" + message);
    }

    @Override
    public void onError(Context ctx, Exception e) {
        if (e == null) {
            System.out.println("客户端正常断开连接");
        } else {
            System.out.println("连接错误：" + e.getMessage());
        }
    }

    @Override
    public void onClose(Context ctx) throws Exception {
        System.out.println("连接关闭！当前连接数：" + WebSocketManager.getConnectionCount());
    }
}
```

#### 2. 注册 WebSocket 路由

```java
import com.da.web.annotations.Inject;
import com.da.web.annotations.Path;
import com.da.web.core.Context;
import com.da.web.function.Handler;
import com.da.web.function.WsListener;

@Path("/ws")
public class WebSocketController implements Handler {

    @Inject("ws")
    WsListener wsListener;

    @Override
    public void callback(Context ctx) throws Exception {
        // 注册监听器，完成 WebSocket 握手
        ctx.setWsListener(wsListener);
    }
}
```

#### 3. 前端测试页面

```html
<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <title>WebSocket 测试</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 20px; }
        #messages { border: 1px solid #ccc; height: 300px; overflow-y: scroll; padding: 10px; margin: 10px 0; }
        .message { margin: 5px 0; padding: 5px; background: #f0f0f0; }
        button { padding: 10px 20px; margin: 5px; cursor: pointer; }
        input { padding: 10px; width: 300px; }
    </style>
</head>
<body>
    <h1>WebSocket 测试</h1>
    <div id="status">状态：未连接</div>
    <div id="messages"></div>
    <input type="text" id="messageInput" placeholder="输入消息...">
    <button onclick="send()">发送</button>
    <button onclick="closeConnection()">断开连接</button>

    <script>
        let socket;
        const messagesDiv = document.getElementById('messages');
        const statusDiv = document.getElementById('status');

        function addMessage(text, isReceived = true) {
            const div = document.createElement('div');
            div.className = 'message';
            div.style.background = isReceived ? '#e3f2fd' : '#f5f5f5';
            div.textContent = (isReceived ? '收到：' : '发送：') + text;
            messagesDiv.appendChild(div);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }

        function connect() {
            socket = new WebSocket('ws://localhost:8080/ws');

            socket.onopen = () => {
                statusDiv.textContent = '状态：已连接';
                statusDiv.style.color = 'green';
                addMessage('连接成功！');
            };

            socket.onmessage = (event) => {
                addMessage(event.data);
            };

            socket.onclose = () => {
                statusDiv.textContent = '状态：已断开';
                statusDiv.style.color = 'red';
                addMessage('连接已关闭');
            };

            socket.onerror = (error) => {
                statusDiv.textContent = '状态：错误';
                statusDiv.style.color = 'orange';
                addMessage('连接错误');
            };
        }

        function send() {
            const input = document.getElementById('messageInput');
            const message = input.value.trim();
            if (message && socket && socket.readyState === WebSocket.OPEN) {
                socket.send(message);
                addMessage(message, false);
                input.value = '';
            }
        }

        function closeConnection() {
            if (socket) {
                socket.close();
            }
        }

        // 页面加载时自动连接
        connect();

        // 回车发送
        document.getElementById('messageInput').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') send();
        });
    </script>
</body>
</html>
```

### WebSocketManager API

| 方法 | 说明 |
|------|------|
| `addConnection(SocketChannel, Context)` | 添加 WebSocket 连接 |
| `removeConnection(SocketChannel)` | 移除并关闭连接 |
| `getContext(SocketChannel)` | 获取连接的上下文 |
| `hasConnection(SocketChannel)` | 检查连接是否存在 |
| `getConnectionCount()` | 获取当前连接数 |
| `sendMessage(SocketChannel, String)` | 向指定连接发送消息 |
| `broadcast(String)` | 广播消息给所有客户端 |
| `closeAll()` | 关闭所有连接 |

### WsListener 接口

```java
public interface WsListener {
    // 连接建立时调用（可选实现）
    default void onOpen(Context ctx) throws Exception { }
    
    // 收到消息时调用（必须实现）
    void onMessage(Context ctx, String message) throws Exception;
    
    // 发生错误或关闭时调用（必须实现）
    void onError(Context ctx, Exception e);
    
    // 连接关闭时调用（可选实现）
    default void onClose(Context ctx) throws Exception { }
}
```

## LLM 协议支持

项目支持多种 LLM 协议格式，包括 OpenAI 和 Anthropic，采用容器化依赖注入架构。

### 架构设计

```
src/main/java/com/da/web/
├── core/                    # 核心包（纯净）
│   ├── LlmProvider.java     # LLM 提供者接口
│   ├── Context.java         # 请求上下文
│   └── DApp.java            # 服务器主类
├── model/                   # 通用数据模型
│   ├── ChatRequest.java     # 通用聊天请求
│   ├── Message.java         # 消息模型
│   └── ModelInfo.java       # 模型信息
├── protocol/                # 协议实现层
│   ├── openai/              # OpenAI 协议实现
│   │   ├── OpenAiProvider.java    # 核心业务逻辑（@Component）
│   │   ├── OpenAIService.java     # API 端点服务（@Component）
│   │   ├── ModelService.java      # 模型列表服务（@Component）
│   │   └── *.java           # 协议特定的 DTO 类
│   └── anthropic/           # Anthropic 协议实现
│       ├── AnthropicProvider.java # 核心业务逻辑（@Component）
│       ├── AnthropicService.java  # API 端点服务（@Component）
│       ├── ModelService.java      # 模型列表服务（@Component）
│       └── *.java           # 协议特定的 DTO 类
├── annotations/             # 注解定义
│   ├── Component.java       # Bean 组件注解
│   ├── Inject.java          # 依赖注入注解
│   └── Path.java            # 路由路径注解
└── bean/                    # Bean 容器管理
    └── BeanContainer.java   # IOC 容器实现
```

### 核心设计理念

1. **Core 包纯净原则**：只包含核心接口和基础设施，不包含具体业务实现
2. **模型层独立**：通用 POJO 类放在 `model` 包，与核心逻辑分离
3. **协议实现隔离**：每个协议在 `protocol/{name}` 目录下独立实现
4. **全面容器化管理**：所有组件使用 `@Component` 注册，通过 `@Inject` 依赖注入
5. **Provider-Service 分离**：
   - `Provider`：核心业务逻辑，管理模型列表、生成响应
   - `Service`：API 端点服务，处理 HTTP 请求/响应转换

### OpenAI 协议示例

#### 启动服务

```java
public class OpenAIServer {
    public static void main(String[] args) {
        // 自动扫描并注册所有 @Component 注解的类
        DApp app = new DApp(OpenAIServer.class);
        app.listen();
    }
}
```

#### Provider 实现（核心业务逻辑）

```java
@Component("openAiProvider")
public class OpenAiProvider implements LlmProvider {
    
    private static final List<String> SUPPORTED_MODELS = Arrays.asList(
        "gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"
    );
    
    @Override
    public List<ModelInfo> getModels() {
        List<ModelInfo> models = new ArrayList<>();
        for (String modelId : SUPPORTED_MODELS) {
            models.add(new ModelInfo(modelId, "openai"));
        }
        return models;
    }
    
    @Override
    public String chatComplete(ChatRequest request) {
        // 基于规则的智能响应生成
        String userMessage = extractLastUserMessage(request);
        return generateIntelligentResponse(userMessage);
    }
    
    @Override
    public Iterator<String> chatCompleteStream(ChatRequest request) {
        String fullResponse = chatComplete(request);
        return new ResponseIterator(fullResponse, 3);
    }
}
```

#### Service 实现（API 端点）

```java
@Component("/v1/chat/completions")
public class OpenAIService implements Handler {
    
    // 依赖注入 Provider
    @Inject("openAiProvider")
    private LlmProvider llmProvider;
    
    @Override
    public void callback(Context ctx) throws Exception {
        if ("POST".equalsIgnoreCase(ctx.getMethod())) {
            handleChatCompletion(ctx);
        }
    }
    
    private void handleChatCompletion(Context ctx) throws Exception {
        ChatCompletionRequest request = parseRequest(ctx);
        
        // 转换为通用请求
        ChatRequest chatRequest = convertToChatRequest(request);
        
        // 调用 Provider 生成响应
        if (request.getStream()) {
            handleStreamingResponse(ctx, chatRequest);
        } else {
            String reply = llmProvider.chatComplete(chatRequest);
            sendJsonResponse(ctx, buildResponse(reply));
        }
    }
}
```

#### 模型列表服务

```java
@Component("/v1/models")
public class ModelService implements Handler {
    
    @Inject("openAiProvider")
    private LlmProvider llmProvider;
    
    @Override
    public void callback(Context ctx) throws Exception {
        List<ModelInfo> models = llmProvider.getModels();
        String json = modelsToJson(models);
        ctx.sendJson(json);
    }
}
```

#### 测试示例

```bash
# 非流式请求
curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{"role": "user", "content": "你好"}]
  }'

# 流式请求
curl http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{"role": "user", "content": "你好"}],
    "stream": true
  }'

# 获取模型列表
curl http://localhost:8080/v1/models
```

### Anthropic 协议示例

#### 启动服务

```java
public class AnthropicServer {
    public static void main(String[] args) {
        DApp app = new DApp(AnthropicServer.class);
        app.listen();
    }
}
```

#### Provider 实现

```java
@Component("anthropicProvider")
public class AnthropicProvider implements LlmProvider {
    
    private static final List<String> SUPPORTED_MODELS = Arrays.asList(
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229",
        "claude-3-haiku-20240307",
        "claude-2.1",
        "claude-2.0"
    );
    
    @Override
    public List<ModelInfo> getModels() {
        List<ModelInfo> models = new ArrayList<>();
        for (String modelId : SUPPORTED_MODELS) {
            models.add(new ModelInfo(modelId, "anthropic"));
        }
        return models;
    }
    
    @Override
    public String chatComplete(ChatRequest request) {
        // Anthropic 风格的响应生成
        return generateAnthropicResponse(request);
    }
}
```

#### Service 实现

```java
@Component("/v1/messages")
public class AnthropicService implements Handler {
    
    @Inject("anthropicProvider")
    private LlmProvider llmProvider;
    
    @Override
    public void callback(Context ctx) throws Exception {
        if ("POST".equalsIgnoreCase(ctx.getMethod())) {
            handleMessages(ctx);
        }
    }
}
```

#### 测试示例

```bash
# 非流式请求
curl http://localhost:8080/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: test-key" \
  -d '{
    "model": "claude-3-sonnet-20240229",
    "messages": [{"role": "user", "content": "你好"}],
    "max_tokens": 100
  }'

# 流式请求
curl http://localhost:8080/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: test-key" \
  -d '{
    "model": "claude-3-sonnet-20240229",
    "messages": [{"role": "user", "content": "你好"}],
    "max_tokens": 100,
    "stream": true
  }'
```

### 依赖注入详解

#### @Component 注解

注册 Bean 到容器：

```java
// 指定 Bean 名称
@Component("myService")
public class MyService implements Handler { }

// 路径作为 Bean 名称（用于路由）
@Component("/api/users")
public class UserController implements Handler { }
```

#### @Inject 注解

注入依赖：

```java
@Component("userService")
public class UserService {
    
    // 注入指定的 Bean
    @Inject("userRepository")
    private UserRepository repository;
    
    // 注入基本类型（从配置或默认值）
    @Inject("100")
    private int maxUsers;
    
    // 注入字符串
    @Inject("admin")
    private String defaultRole;
}
```

#### Bean 获取方式

```java
DApp app = new DApp(MyServer.class);

// 通过名称获取 Bean
MyService service = app.getBean("myService", MyService.class);

// 通过路径获取 Bean（@Component 的路径作为名称）
UserController controller = (UserController) app.getBean("/api/users");

// 获取所有 Bean 名称
Set<String> beanNames = app.getAllBeanNames();
```

## 项目结构

```
src/main/java/com/da/web/
├── annotations/          # 注解定义
│   ├── Component.java    # Bean 组件注解
│   ├── Inject.java       # 依赖注入注解
│   └── Path.java         # 路由路径注解
├── bean/                 # Bean 容器管理
│   └── BeanContainer.java
├── config/               # 配置管理
│   └── ServerConfig.java
├── constant/             # 常量定义
│   ├── HttpStatus.java
│   └── ContentTypes.java
├── core/                 # 核心包（纯净）
│   ├── DApp.java         # 服务器主类
│   ├── Context.java      # 请求上下文
│   └── LlmProvider.java  # LLM 提供者接口
├── model/                # 通用数据模型
│   ├── ChatRequest.java  # 通用聊天请求
│   ├── Message.java      # 消息模型
│   └── ModelInfo.java    # 模型信息
├── protocol/             # 协议实现层
│   ├── openai/           # OpenAI 协议实现
│   │   ├── OpenAiProvider.java
│   │   ├── OpenAIService.java
│   │   ├── ModelService.java
│   │   └── *.java        # 协议特定 DTO
│   └── anthropic/        # Anthropic 协议实现
│       ├── AnthropicProvider.java
│       ├── AnthropicService.java
│       ├── ModelService.java
│       └── *.java        # 协议特定 DTO
├── enums/                # 枚举类型
├── exception/            # 异常处理
├── function/             # 函数式接口
│   ├── Handler.java
│   └── WsListener.java
├── http/                 # HTTP 解析与处理
│   ├── HttpRequest.java
│   ├── HttpResponse.java
│   ├── HttpParser.java
│   ├── JsonParser.java
│   └── MultiValueMap.java
├── io/                   # IO 相关
│   └── StaticFileRegistry.java
├── router/               # 路由管理
│   ├── RouteRegistry.java
│   └── RouteMapping.java
├── sse/                  # SSE（Server-Sent Events）支持
│   └── SSEManager.java
├── util/                 # 工具类
│   ├── Utils.java
│   └── Logger.java
└── websocket/            # WebSocket 支持
    └── WebSocketManager.java
```

## 注意事项

1. **线程安全**：服务器使用 NIO 多路复用模型，支持高并发连接
2. **WebSocket 帧解析**：支持标准的 WebSocket 协议帧格式（包括掩码处理）
3. **连接管理**：使用 ConcurrentHashMap 和 CopyOnWriteArraySet 确保线程安全
4. **自动重连端口**：如果指定端口被占用，会自动尝试下一个可用端口

## License

MIT License
