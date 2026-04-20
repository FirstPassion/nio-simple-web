package com.da.web.core;

import com.da.web.annotations.*;
import com.da.web.router.RouteRegistry;
import org.junit.Test;
import org.junit.Before;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Cookie 和 Session 功能单元测试
 */
public class CookieSessionTest {

    private RouteRegistry routeRegistry;

    @Before
    public void setUp() {
        routeRegistry = new RouteRegistry();
    }

    /**
     * 测试 Cookie 设置和获取注解路由
     */
    @Test
    public void testCookieAnnotationRoutes() {
        CookieController controller = new CookieController();
        com.da.web.ioc.ComponentScanner scanner = new com.da.web.ioc.ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("GET /cookie/set 应该被注册", 
            routeRegistry.hasRoute("/cookie/set", "GET"));
        assertTrue("GET /cookie/get 应该被注册", 
            routeRegistry.hasRoute("/cookie/get", "GET"));
        assertTrue("GET /cookie/delete 应该被注册", 
            routeRegistry.hasRoute("/cookie/delete", "GET"));
    }

    /**
     * 测试 Session 设置和获取注解路由
     */
    @Test
    public void testSessionAnnotationRoutes() {
        SessionController controller = new SessionController();
        com.da.web.ioc.ComponentScanner scanner = new com.da.web.ioc.ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("GET /session/set 应该被注册", 
            routeRegistry.hasRoute("/session/set", "GET"));
        assertTrue("GET /session/get 应该被注册", 
            routeRegistry.hasRoute("/session/get", "GET"));
        assertTrue("GET /session/invalidate 应该被注册", 
            routeRegistry.hasRoute("/session/invalidate", "GET"));
    }

    /**
     * 测试重定向注解路由
     */
    @Test
    public void testRedirectAnnotationRoutes() {
        RedirectController controller = new RedirectController();
        com.da.web.ioc.ComponentScanner scanner = new com.da.web.ioc.ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("GET /redirect/temp 应该被注册", 
            routeRegistry.hasRoute("/redirect/temp", "GET"));
        assertTrue("GET /redirect/permanent 应该被注册", 
            routeRegistry.hasRoute("/redirect/permanent", "GET"));
    }

    /**
     * 测试综合场景 - 登录流程
     */
    @Test
    public void testLoginFlow() {
        AuthController controller = new AuthController();
        com.da.web.ioc.ComponentScanner scanner = new com.da.web.ioc.ComponentScanner("");
        scanner.registerAnnotatedRoutes(controller, routeRegistry);

        assertTrue("POST /auth/login 应该被注册", 
            routeRegistry.hasRoute("/auth/login", "POST"));
        assertTrue("GET /auth/logout 应该被注册", 
            routeRegistry.hasRoute("/auth/logout", "GET"));
        assertTrue("GET /auth/profile 应该被注册", 
            routeRegistry.hasRoute("/auth/profile", "GET"));
    }

    // ==================== 测试控制器类 ====================

    @Path("/cookie")
    public static class CookieController {
        
        @Get("/set")
        public void setCookie(Context ctx) {
            // 设置会话 Cookie
            ctx.setCookie("user", "john");
            // 设置持久化 Cookie（7 天）
            ctx.setCookie("theme", "dark", 7 * 24 * 60 * 60, "/");
            ctx.sendJson("{\"status\":\"cookies set\"}");
        }

        @Get("/get")
        public void getCookie(Context ctx) {
            String user = ctx.getCookie("user");
            String theme = ctx.getCookie("theme");
            ctx.sendJson("{\"user\":\"" + user + "\",\"theme\":\"" + theme + "\"}");
        }

        @Get("/delete")
        public void deleteCookie(Context ctx) {
            ctx.deleteCookie("user");
            ctx.sendJson("{\"status\":\"cookie deleted\"}");
        }
    }

    @Path("/session")
    public static class SessionController {
        
        @Get("/set")
        public void setSession(Context ctx) {
            ctx.setSessionAttribute("userId", 123);
            ctx.setSessionAttribute("username", "alice");
            ctx.setSessionAttribute("role", "admin");
            ctx.sendJson("{\"status\":\"session set\"}");
        }

        @Get("/get")
        public void getSession(Context ctx) {
            Integer userId = (Integer) ctx.getSessionAttribute("userId");
            String username = (String) ctx.getSessionAttribute("username");
            String role = (String) ctx.getSessionAttribute("role");
            
            String json = String.format(
                "{\"userId\":%d,\"username\":\"%s\",\"role\":\"%s\"}",
                userId != null ? userId : 0,
                username != null ? username : "",
                role != null ? role : ""
            );
            ctx.sendJson(json);
        }

        @Get("/invalidate")
        public void invalidateSession(Context ctx) {
            ctx.invalidateSession();
            ctx.sendJson("{\"status\":\"session invalidated\"}");
        }

        @Get("/remove")
        public void removeSessionAttribute(Context ctx) {
            ctx.removeSessionAttribute("role");
            ctx.sendJson("{\"status\":\"attribute removed\"}");
        }
    }

    @Path("/redirect")
    public static class RedirectController {
        
        @Get("/temp")
        public void redirectTemp(Context ctx) {
            ctx.redirect("/dashboard");
        }

        @Get("/permanent")
        public void redirectPermanent(Context ctx) {
            ctx.redirectPermanent("/new-location");
        }

        @Get("/conditional")
        public void conditionalRedirect(Context ctx) {
            String user = ctx.getCookie("user");
            if (user == null) {
                ctx.redirect("/login");
            } else {
                ctx.sendJson("{\"user\":\"" + user + "\"}");
            }
        }
    }

    @Path("/auth")
    public static class AuthController {
        
        @Post("/login")
        public void login(
            @BodyParam("username") String username,
            @BodyParam("password") String password,
            Context ctx
        ) {
            // 简单的模拟验证
            if ("admin".equals(username) && "123456".equals(password)) {
                // 设置 Session
                ctx.setSessionAttribute("loggedIn", true);
                ctx.setSessionAttribute("username", username);
                ctx.setSessionAttribute("loginTime", System.currentTimeMillis());
                
                // 设置记住我的 Cookie
                String remember = ctx.getBodyParam("remember") != null ? 
                    ctx.getBodyParam("remember").toString() : null;
                if ("true".equals(remember)) {
                    ctx.setCookie("rememberMe", username, 30 * 24 * 60 * 60);
                }
                
                ctx.sendJson("{\"success\":true,\"message\":\"登录成功\"}");
            } else {
                ctx.sendJson("{\"success\":false,\"message\":\"用户名或密码错误\"}", 401);
            }
        }

        @Get("/logout")
        public void logout(Context ctx) {
            ctx.invalidateSession();
            ctx.deleteCookie("rememberMe");
            ctx.redirect("/login");
        }

        @Get("/profile")
        public void profile(Context ctx) {
            Boolean loggedIn = (Boolean) ctx.getSessionAttribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                ctx.sendJson("{\"error\":\"未登录\"}", 401);
                return;
            }
            
            String username = (String) ctx.getSessionAttribute("username");
            Long loginTime = (Long) ctx.getSessionAttribute("loginTime");
            
            String json = String.format(
                "{\"username\":\"%s\",\"loginTime\":%d}",
                username != null ? username : "",
                loginTime != null ? loginTime : 0
            );
            ctx.sendJson(json);
        }
    }

    @Path("/user")
    public static class UserController {
        
        @Get("/{id}")
        public void getUserById(@PathParam("id") Integer id, Context ctx) {
            ctx.sendJson(String.format("{\"id\":%d,\"name\":\"User%d\"}", id, id));
        }

        @Get("/{id}/posts/{postId}")
        public void getUserPost(
            @PathParam("id") Integer userId,
            @PathParam("postId") Integer postId,
            Context ctx
        ) {
            ctx.sendJson(String.format(
                "{\"userId\":%d,\"postId\":%d,\"title\":\"Post Title\"}",
                userId, postId
            ));
        }

        @Get("/{id}/settings")
        public void getUserSettings(
            @PathParam("id") Integer userId,
            @QueryParam("include") String include,
            Context ctx
        ) {
            String json = String.format(
                "{\"userId\":%d,\"include\":\"%s\",\"settings\":{}}",
                userId,
                include != null ? include : ""
            );
            ctx.sendJson(json);
        }
    }
}
