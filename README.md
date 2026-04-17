# NIO Simple Web Server

基于 Java NIO 实现的轻量级 Web 服务器，支持 HTTP 和 WebSocket 协议。

## ⚠️ 重要更新说明 (v2.0)

**这是一个破坏性更新！** 我们完全重写了 HTTP 请求解析模块，采用递归下降法实现完整的 HTTP/1.1 解析器。

### 主要变更

1. **完全重构的 HTTP 解析器**：使用递归下降法手写解析，不依赖任何第三方库
2. **移除旧的 States 枚举**：HTTP 状态码直接使用整数表示（如 `404`、`200`）
3. **增强的 POST 请求处理**：
   - 自动解析 `application/json`
   - 自动解析 `application/x-www-form-urlencoded`
   - 支持 `multipart/form-data` 文件上传
   - 支持 `text/plain` 纯文本
4. **新增 HttpRequest/HttpResponse 模型**：更清晰的请求响应对象
5. **MultiValueMap**：支持多值的 Map，用于 HTTP 头部管理
6. **JsonParser**：递归下降法实现的 JSON 解析器

### 迁移指南

#### 旧代码
```java
context.sendHtml("<h1>404</h1>", States.NOT_FOUND.ordinal());
```

#### 新代码
```java
context.sendHtml("<h1>404</h1>", 404);
```

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
        <version>1.0.1</version>
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
#指定端口号 (默认从 8080 开始找可用的端口)
port=8083
#指定 static 目录 (默认为 resources/static),用/分开文件夹，static 目录下的 index.html 访问路径为/
static=aaa/aaa
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

## 项目结构

```
src/main/java/com/da/web/
├── annotations/          # 注解定义
│   ├── Component.java
│   ├── Inject.java
│   └── Path.java
├── bean/                 # Bean 容器
│   └── BeanContainer.java
├── config/               # 配置管理
│   └── ServerConfig.java
├── constant/             # 常量定义
│   ├── HttpStatus.java
│   └── ContentTypes.java
├── core/                 # 核心类
│   ├── DApp.java         # 服务器主类
│   └── Context.java      # 请求上下文
├── enums/                # 枚举类型
├── exception/            # 异常处理
├── function/             # 函数式接口
│   ├── Handler.java
│   └── WsListener.java
├── io/                   # IO 相关
│   └── StaticFileRegistry.java
├── router/               # 路由管理
│   ├── RouteRegistry.java
│   └── RouteMapping.java
├── util/                 # 工具类
│   └── Utils.java
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
