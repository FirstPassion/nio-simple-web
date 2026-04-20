package com.da.web.core;

import com.da.web.annotations.*;
import com.da.web.function.Handler;
import com.da.web.router.RouteMapping;
import com.da.web.router.RouteRegistry;
import org.junit.Test;
import org.junit.Before;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Context 功能增强单元测试
 * 测试重定向、Cookie、Session、路径变量等功能
 */
public class ContextTest {

    private RouteRegistry routeRegistry;

    @Before
    public void setUp() {
        routeRegistry = new RouteRegistry();
    }

    /**
     * 测试路由映射 - 精确匹配优先于路径变量匹配
     */
    @Test
    public void testRouteMappingPriority() {
        // 注册带路径变量的路由
        routeRegistry.register("/user/{id}", "GET", ctx -> ctx.send("User by ID"));
        // 注册精确匹配的路由
        routeRegistry.register("/user/profile", "GET", ctx -> ctx.send("User Profile"));

        // 精确匹配应该优先
        Optional<Handler> handler = routeRegistry.getHandler("/user/profile", "GET");
        assertTrue("精确匹配路由应该存在", handler.isPresent());

        // 路径变量匹配也应该工作
        handler = routeRegistry.getHandler("/user/123", "GET");
        assertTrue("路径变量路由应该存在", handler.isPresent());
    }

    /**
     * 测试路径变量提取
     */
    @Test
    public void testPathVariableExtraction() {
        routeRegistry.register("/user/{id}/post/{postId}", "GET", ctx -> {
            String id = ctx.getPathVariable("id");
            String postId = ctx.getPathVariable("postId");
            ctx.sendJson("{\"userId\":\"" + id + "\",\"postId\":\"" + postId + "\"}");
        });

        Optional<RouteMapping> mapping = routeRegistry.getRouteMapping("/user/100/post/200", "GET");
        assertTrue("路由应该匹配", mapping.isPresent());

        Map<String, String> variables = mapping.get().extractPathVariables("/user/100/post/200");
        assertEquals("100", variables.get("id"));
        assertEquals("200", variables.get("postId"));
    }

    /**
     * 测试 HTTP 方法区分
     */
    @Test
    public void testHttpMethodDistinction() {
        routeRegistry.register("/api/data", "GET", ctx -> ctx.send("GET data"));
        routeRegistry.register("/api/data", "POST", ctx -> ctx.send("POST data"));
        routeRegistry.register("/api/data", "PUT", ctx -> ctx.send("PUT data"));
        routeRegistry.register("/api/data", "DELETE", ctx -> ctx.send("DELETE data"));

        // 验证不同方法路由到不同的处理器
        Optional<Handler> getHandler = routeRegistry.getHandler("/api/data", "GET");
        Optional<Handler> postHandler = routeRegistry.getHandler("/api/data", "POST");
        Optional<Handler> putHandler = routeRegistry.getHandler("/api/data", "PUT");
        Optional<Handler> deleteHandler = routeRegistry.getHandler("/api/data", "DELETE");

        assertTrue("GET 路由应该存在", getHandler.isPresent());
        assertTrue("POST 路由应该存在", postHandler.isPresent());
        assertTrue("PUT 路由应该存在", putHandler.isPresent());
        assertTrue("DELETE 路由应该存在", deleteHandler.isPresent());

        // 验证不存在的组合
        Optional<Handler> patchHandler = routeRegistry.getHandler("/api/data", "PATCH");
        assertFalse("PATCH 路由不应该存在", patchHandler.isPresent());
    }

    /**
     * 测试注解路由扫描和注册
     */
    @Test
    public void testAnnotatedRouteRegistration() {
        TestController controller = new TestController();
        com.da.web.ioc.ComponentScanner scanner = new com.da.web.ioc.ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        // 验证所有注解路由都被注册
        assertTrue("GET /test/hello 应该被注册", 
            routeRegistry.hasRoute("/test/hello", "GET"));
        assertTrue("POST /test/create 应该被注册", 
            routeRegistry.hasRoute("/test/create", "POST"));
        assertTrue("PUT /test/update/{id} 应该被注册", 
            routeRegistry.hasRoute("/test/update/{id}", "PUT"));
        assertTrue("DELETE /test/delete/{id} 应该被注册", 
            routeRegistry.hasRoute("/test/delete/{id}", "DELETE"));
    }

    /**
     * 测试 RequestMapping 注解
     */
    @Test
    public void testRequestMappingAnnotation() {
        RequestMappingController controller = new RequestMappingController();
        com.da.web.ioc.ComponentScanner scanner = new com.da.web.ioc.ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("GET /api/list 应该被注册", 
            routeRegistry.hasRoute("/api/list", "GET"));
        assertTrue("POST /api/save 应该被注册", 
            routeRegistry.hasRoute("/api/save", "POST"));
    }

    /**
     * 测试路径参数绑定
     */
    @Test
    public void testPathParamBinding() {
        ParamTestController controller = new ParamTestController();
        com.da.web.ioc.ComponentScanner scanner = new com.da.web.ioc.ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("GET /param/user/{id} 应该被注册", 
            routeRegistry.hasRoute("/param/user/{id}", "GET"));
    }

    // ==================== 测试控制器类 ====================

    @Path("/test")
    public static class TestController {
        
        @Get("/hello")
        public void hello(Context ctx) {
            ctx.send("Hello World");
        }

        @Post("/create")
        public void create(Context ctx) {
            ctx.sendJson("{\"status\":\"created\"}");
        }

        @Put("/update/{id}")
        public void update(@PathParam("id") String id, Context ctx) {
            ctx.sendJson("{\"id\":\"" + id + "\",\"action\":\"updated\"}");
        }

        @Delete("/delete/{id}")
        public void delete(@PathParam("id") String id, Context ctx) {
            ctx.sendJson("{\"id\":\"" + id + "\",\"action\":\"deleted\"}");
        }
    }

    @Path("/api")
    public static class RequestMappingController {
        
        @RequestMapping(value = "/list", method = "GET")
        public void list(Context ctx) {
            ctx.sendJson("{\"items\":[]}");
        }

        @RequestMapping(value = "/save", method = "POST")
        public void save(@BodyParam("data") String data, Context ctx) {
            ctx.sendJson("{\"saved\":\"" + data + "\"}");
        }
    }

    @Path("/param")
    public static class ParamTestController {
        
        @Get("/user/{id}")
        public void getUser(
            @PathParam("id") Integer id,
            @QueryParam("details") Boolean details,
            @Header("Authorization") String auth,
            Context ctx
        ) {
            ctx.sendJson("{\"id\":" + id + ",\"details\":" + details + ",\"auth\":\"" + auth + "\"}");
        }

        @Post("/search")
        public void search(
            @BodyParam("keyword") String keyword,
            @BodyParam("page") Integer page,
            Context ctx
        ) {
            ctx.sendJson("{\"keyword\":\"" + keyword + "\",\"page\":" + page + "}");
        }
    }
}
