package com.da.web.test;

import com.da.web.annotations.Component;
import com.da.web.annotations.Inject;
import com.da.web.core.DApp;

import java.util.ArrayList;
import java.util.List;

@Component("userService")
class UserService2 {
    public List<String> getUsers() {
        List<String> users = new ArrayList<>();
        users.add("admin");
        users.add("user");
        return users;
    }
}

public class TestInjectApp {
    @Inject("userService")
    private UserService2 userService;

    public static void main(String[] args) {
        System.out.println("Starting test application...");
        DApp app = new DApp(TestInjectApp.class);
        
        // 检查 userService 是否被注入
        TestInjectApp instance = new TestInjectApp();
        System.out.println("userService after creation: " + instance.userService);
        
        // 尝试从容器获取 bean
        Object bean = app.getBean("userService");
        System.out.println("userService from container: " + bean);
        
        app.use("/", ctx -> {
            ctx.send("hello world");
        });
        app.listen();
        
        System.out.println("Server started.");
    }
}
