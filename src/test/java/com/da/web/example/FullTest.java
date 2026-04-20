package com.da.web.example;

import com.da.web.annotations.*;
import com.da.web.core.Context;
import com.da.web.core.DApp;
import com.da.web.router.RouteRegistry;
import com.da.web.ioc.ComponentScanner;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * 综合功能测试：展示完整的 Web 框架功能
 * 包括：路由注解、参数绑定、Cookie、Session、重定向等
 * 通过 JUnit 测试验证 IOC 容器、依赖注入、路由注册等功能
 */
public class FullTest {

    private DApp app;
    private RouteRegistry routeRegistry;

    @Before
    public void setUp() {
        routeRegistry = new RouteRegistry();
    }

    @After
    public void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    /**
     * 测试 IOC 容器实例化所有 Controller（包括无参构造和有参构造）
     */
    @Test
    public void testIOCContainerInstantiation() {
        // 扫描并注册所有示例 Controller
        ComponentScanner scanner = new ComponentScanner("com.da.web.example");
        
        // 验证能够成功实例化所有 Controller 类
        UserController userController = scanner.instantiateController(UserController.class);
        assertNotNull("UserController 应该被成功实例化", userController);
        
        AuthController authController = scanner.instantiateController(AuthController.class);
        assertNotNull("AuthController 应该被成功实例化", authController);
        
        ProductController productController = scanner.instantiateController(ProductController.class);
        assertNotNull("ProductController 应该被成功实例化", productController);
        
        PageController pageController = scanner.instantiateController(PageController.class);
        assertNotNull("PageController 应该被成功实例化", pageController);
        
        ApiController apiController = scanner.instantiateController(ApiController.class);
        assertNotNull("ApiController 应该被成功实例化", apiController);
    }

    /**
     * 测试路由注解扫描和注册 - UserController
     */
    @Test
    public void testUserRoutesRegistration() {
        UserController controller = new UserController();
        ComponentScanner scanner = new ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        // 验证所有 UserController 的路由都被注册
        assertTrue("GET /user/{id} 应该被注册", 
            routeRegistry.hasRoute("/user/{id}", "GET"));
        assertTrue("GET /user/{id}/posts/{postId} 应该被注册", 
            routeRegistry.hasRoute("/user/{id}/posts/{postId}", "GET"));
        assertTrue("GET /user/search 应该被注册", 
            routeRegistry.hasRoute("/user/search", "GET"));
        assertTrue("GET /user/profile 应该被注册", 
            routeRegistry.hasRoute("/user/profile", "GET"));
    }

    /**
     * 测试路由注解扫描和注册 - AuthController
     */
    @Test
    public void testAuthRoutesRegistration() {
        AuthController controller = new AuthController();
        ComponentScanner scanner = new ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("POST /auth/login 应该被注册", 
            routeRegistry.hasRoute("/auth/login", "POST"));
        assertTrue("GET /auth/logout 应该被注册", 
            routeRegistry.hasRoute("/auth/logout", "GET"));
        assertTrue("GET /auth/profile 应该被注册", 
            routeRegistry.hasRoute("/auth/profile", "GET"));
        assertTrue("GET /auth/check 应该被注册", 
            routeRegistry.hasRoute("/auth/check", "GET"));
    }

    /**
     * 测试路由注解扫描和注册 - ProductController
     */
    @Test
    public void testProductRoutesRegistration() {
        ProductController controller = new ProductController();
        ComponentScanner scanner = new ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("GET /product/list 应该被注册", 
            routeRegistry.hasRoute("/product/list", "GET"));
        assertTrue("GET /product/{id} 应该被注册", 
            routeRegistry.hasRoute("/product/{id}", "GET"));
        assertTrue("POST /product/create 应该被注册", 
            routeRegistry.hasRoute("/product/create", "POST"));
        assertTrue("PUT /product/{id} 应该被注册", 
            routeRegistry.hasRoute("/product/{id}", "PUT"));
        assertTrue("DELETE /product/{id} 应该被注册", 
            routeRegistry.hasRoute("/product/{id}", "DELETE"));
    }

    /**
     * 测试路由注解扫描和注册 - PageController
     */
    @Test
    public void testPageRoutesRegistration() {
        PageController controller = new PageController();
        ComponentScanner scanner = new ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("GET / 应该被注册", 
            routeRegistry.hasRoute("/", "GET"));
        assertTrue("GET /login 应该被注册", 
            routeRegistry.hasRoute("/login", "GET"));
        assertTrue("GET /dashboard 应该被注册", 
            routeRegistry.hasRoute("/dashboard", "GET"));
        assertTrue("GET /old-page 应该被注册", 
            routeRegistry.hasRoute("/old-page", "GET"));
        assertTrue("GET /new-page 应该被注册", 
            routeRegistry.hasRoute("/new-page", "GET"));
    }

    /**
     * 测试路由注解扫描和注册 - ApiController
     */
    @Test
    public void testApiRoutesRegistration() {
        ApiController controller = new ApiController();
        ComponentScanner scanner = new ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("GET /api/status 应该被注册", 
            routeRegistry.hasRoute("/api/status", "GET"));
        assertTrue("POST /api/echo 应该被注册", 
            routeRegistry.hasRoute("/api/echo", "POST"));
    }

    /**
     * 测试完整应用启动和路由注册
     */
    @Test
    public void testFullApplicationStartup() {
        // 启动应用，自动扫描当前包下的@Path 类
        app = new DApp(FullTest.class);
        
        // 验证应用已启动
        assertNotNull("DApp 应该被成功创建", app);
        
        // 验证路由已注册（至少应该有 20 条路由）
        int routeCount = app.getRouteRegistry().getRouteCount();
        assertTrue("应该注册至少 20 条路由", routeCount >= 20);
        
        System.out.println("成功注册 " + routeCount + " 条路由");
    }

    /**
     * 测试路径变量提取 - UserController
     */
    @Test
    public void testPathVariableExtraction() {
        UserController controller = new UserController();
        ComponentScanner scanner = new ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        // 测试单个路径变量
        java.util.Optional<com.da.web.router.RouteMapping> mapping1 = routeRegistry.getRouteMapping("/user/123", "GET");
        assertTrue("路由应该匹配", mapping1.isPresent());
        java.util.Map<String, String> variables1 = mapping1.get().extractPathVariables("/user/123");
        assertEquals("123", variables1.get("id"));

        // 测试多个路径变量
        java.util.Optional<com.da.web.router.RouteMapping> mapping2 = routeRegistry.getRouteMapping("/user/100/posts/200", "GET");
        assertTrue("路由应该匹配", mapping2.isPresent());
        java.util.Map<String, String> variables2 = mapping2.get().extractPathVariables("/user/100/posts/200");
        assertEquals("100", variables2.get("id"));
        assertEquals("200", variables2.get("postId"));
    }

    /**
     * 测试 RequestMapping 注解
     */
    @Test
    public void testRequestMappingAnnotation() {
        ApiController controller = new ApiController();
        ComponentScanner scanner = new ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("GET /api/status 应该被注册", 
            routeRegistry.hasRoute("/api/status", "GET"));
        assertTrue("POST /api/echo 应该被注册", 
            routeRegistry.hasRoute("/api/echo", "POST"));
    }

    // ==================== 示例 Controller 类（与 FullExample 中相同）====================

    /**
     * 用户控制器示例
     * 展示路径变量、查询参数、请求头绑定
     */
    @Path("/user")
    public static class UserController {

        /**
         * GET /user/123
         * 路径变量示例
         */
        @Get("/{id}")
        public void getUserById(@PathParam("id") Integer id, Context ctx) {
            ctx.sendJson(String.format("{\"id\":%d,\"name\":\"User%d\",\"email\":\"user%d@example.com\"}", id, id, id));
        }

        /**
         * GET /user/123/posts/456
         * 多个路径变量示例
         */
        @Get("/{id}/posts/{postId}")
        public void getUserPost(
                @PathParam("id") Integer userId,
                @PathParam("postId") Integer postId,
                Context ctx) {
            ctx.sendJson(String.format(
                    "{\"userId\":%d,\"postId\":%d,\"title\":\"Post Title\",\"content\":\"Post Content\"}",
                    userId, postId));
        }

        /**
         * GET /user/search?keyword=java&page=1&size=10
         * 查询参数示例
         */
        @Get("/search")
        public void searchUsers(
                @QueryParam("keyword") String keyword,
                @QueryParam("page") Integer page,
                @QueryParam("size") Integer size,
                Context ctx) {
            ctx.sendJson(String.format(
                    "{\"keyword\":\"%s\",\"page\":%d,\"size\":%d,\"results\":[]}",
                    keyword != null ? keyword : "",
                    page != null ? page : 1,
                    size != null ? size : 10));
        }

        /**
         * GET /user/profile
         * 请求头示例
         */
        @Get("/profile")
        public void getProfile(
                @Header("Authorization") String auth,
                @Header("User-Agent") String userAgent,
                Context ctx) {
            ctx.sendJson(String.format(
                    "{\"auth\":\"%s\",\"userAgent\":\"%s\"}",
                    auth != null ? auth : "N/A",
                    userAgent != null ? userAgent : "N/A"));
        }
    }

    /**
     * 认证控制器示例
     * 展示 Session 和 Cookie 的使用
     */
    @Path("/auth")
    public static class AuthController {

        /**
         * POST /auth/login
         * 登录：设置 Session 和 Cookie
         */
        @Post("/login")
        public void login(
                @BodyParam("username") String username,
                @BodyParam("password") String password,
                @BodyParam("remember") Boolean remember,
                Context ctx) {

            // 简单验证（实际项目中应该查询数据库）
            if ("admin".equals(username) && "123456".equals(password)) {
                // 设置 Session
                ctx.setSessionAttribute("loggedIn", true);
                ctx.setSessionAttribute("username", username);
                ctx.setSessionAttribute("loginTime", System.currentTimeMillis());
                ctx.setSessionAttribute("role", "admin");

                // 如果选择记住我，设置持久化 Cookie
                if (remember != null && remember) {
                    ctx.setCookie("rememberMe", username, 30 * 24 * 60 * 60); // 30 天
                }

                ctx.sendJson("{\"success\":true,\"message\":\"登录成功\",\"user\":\"" + username + "\"}");
            } else {
                ctx.sendJson("{\"success\":false,\"message\":\"用户名或密码错误\"}", 401);
            }
        }

        /**
         * GET /auth/logout
         * 登出：清除 Session 和 Cookie
         */
        @Get("/logout")
        public void logout(Context ctx) {
            ctx.invalidateSession();
            ctx.deleteCookie("rememberMe");
            ctx.redirect("/login");
        }

        /**
         * GET /auth/profile
         * 需要登录才能访问
         */
        @Get("/profile")
        public void profile(Context ctx) {
            Boolean loggedIn = (Boolean) ctx.getSessionAttribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                ctx.sendJson("{\"error\":\"未登录，请先登录\"}", 401);
                return;
            }

            String username = (String) ctx.getSessionAttribute("username");
            String role = (String) ctx.getSessionAttribute("role");
            Long loginTime = (Long) ctx.getSessionAttribute("loginTime");

            ctx.sendJson(String.format(
                    "{\"username\":\"%s\",\"role\":\"%s\",\"loginTime\":%d}",
                    username, role, loginTime));
        }

        /**
         * GET /auth/check
         * 检查登录状态
         */
        @Get("/check")
        public void checkLogin(Context ctx) {
            Boolean loggedIn = (Boolean) ctx.getSessionAttribute("loggedIn");
            boolean isLoggedIn = loggedIn != null && loggedIn;
            ctx.sendJson("{\"loggedIn\":" + isLoggedIn + "}");
        }
    }

    /**
     * 商品控制器示例
     * 展示 CRUD 操作的路由注解
     */
    @Path("/product")
    public static class ProductController {

        /**
         * GET /product/list
         * 获取商品列表
         */
        @Get("/list")
        public void listProducts(
                @QueryParam("category") String category,
                @QueryParam("page") Integer page,
                Context ctx) {
            ctx.sendJson(String.format(
                    "{\"category\":\"%s\",\"page\":%d,\"products\":[]}",
                    category != null ? category : "all",
                    page != null ? page : 1));
        }

        /**
         * GET /product/{id}
         * 获取单个商品
         */
        @Get("/{id}")
        public void getProduct(@PathParam("id") Integer id, Context ctx) {
            ctx.sendJson(String.format(
                    "{\"id\":%d,\"name\":\"Product%d\",\"price\":99.99,\"stock\":100}",
                    id, id));
        }

        /**
         * POST /product/create
         * 创建商品
         */
        @Post("/create")
        public void createProduct(
                @BodyParam("name") String name,
                @BodyParam("price") Double price,
                @BodyParam("stock") Integer stock,
                Context ctx) {
            ctx.sendJson(String.format(
                    "{\"status\":\"created\",\"name\":\"%s\",\"price\":%.2f,\"stock\":%d}",
                    name != null ? name : "Unknown",
                    price != null ? price : 0.0,
                    stock != null ? stock : 0));
        }

        /**
         * PUT /product/{id}
         * 更新商品
         */
        @Put("/{id}")
        public void updateProduct(
                @PathParam("id") Integer id,
                @BodyParam("name") String name,
                @BodyParam("price") Double price,
                Context ctx) {
            ctx.sendJson(String.format(
                    "{\"status\":\"updated\",\"id\":%d,\"name\":\"%s\",\"price\":%.2f}",
                    id, name != null ? name : "Unknown", price != null ? price : 0.0));
        }

        /**
         * DELETE /product/{id}
         * 删除商品
         */
        @Delete("/{id}")
        public void deleteProduct(@PathParam("id") Integer id, Context ctx) {
            ctx.sendJson(String.format("{\"status\":\"deleted\",\"id\":%d}", id));
        }
    }

    /**
     * 页面控制器示例
     * 展示重定向功能
     */
    @Path("/")
    public static class PageController {

        /**
         * GET /
         * 首页
         */
        @Get("")
        public void index(Context ctx) {
            ctx.sendHtml("<h1>欢迎访问！</h1><p>这是一个完整的 Web 框架示例</p>" +
                    "<ul>" +
                    "<li><a href='/user/1'>查看用户 1</a></li>" +
                    "<li><a href='/product/list'>商品列表</a></li>" +
                    "<li><a href='/auth/profile'>个人中心</a></li>" +
                    "</ul>");
        }

        /**
         * GET /login
         * 登录页面
         */
        @Get("/login")
        public void loginPage(Context ctx) {
            // 如果已经登录，重定向到个人中心
            Boolean loggedIn = (Boolean) ctx.getSessionAttribute("loggedIn");
            if (loggedIn != null && loggedIn) {
                ctx.redirect("/auth/profile");
                return;
            }

            ctx.sendHtml("<h1>登录</h1>" +
                    "<form action='/auth/login' method='POST'>" +
                    "<input type='text' name='username' placeholder='用户名' value='admin'/><br/>" +
                    "<input type='password' name='password' placeholder='密码' value='123456'/><br/>" +
                    "<label><input type='checkbox' name='remember'/> 记住我</label><br/>" +
                    "<button type='submit'>登录</button>" +
                    "</form>");
        }

        /**
         * GET /dashboard
         * 仪表盘（需要登录）
         */
        @Get("/dashboard")
        public void dashboard(Context ctx) {
            Boolean loggedIn = (Boolean) ctx.getSessionAttribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                ctx.redirect("/login");
                return;
            }

            ctx.sendHtml("<h1>仪表盘</h1><p>欢迎回来！</p><a href='/auth/logout'>退出登录</a>");
        }

        /**
         * GET /old-page
         * 旧页面永久重定向到新页面
         */
        @Get("/old-page")
        public void oldPage(Context ctx) {
            ctx.redirectPermanent("/new-page");
        }

        /**
         * GET /new-page
         * 新页面
         */
        @Get("/new-page")
        public void newPage(Context ctx) {
            ctx.sendHtml("<h1>新页面</h1><p>这是迁移后的新页面</p>");
        }
    }

    /**
     * API 示例 - 使用 RequestMapping
     */
    @Path("/api")
    public static class ApiController {

        /**
         * GET /api/status
         * 使用 RequestMapping 注解
         */
        @RequestMapping(value = "/status", method = "GET")
        public void status(Context ctx) {
            ctx.sendJson("{\"status\":\"ok\",\"timestamp\":" + System.currentTimeMillis() + "}");
        }

        /**
         * POST /api/echo
         * 回显请求体
         */
        @RequestMapping(value = "/echo", method = "POST")
        public void echo(@BodyParam("message") String message, Context ctx) {
            ctx.sendJson("{\"echo\":\"" + (message != null ? message : "") + "\"}");
        }
    }
}
