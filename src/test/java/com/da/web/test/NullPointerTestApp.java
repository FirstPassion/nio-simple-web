package com.da.web.test;

import com.da.web.annotations.Component;
import com.da.web.annotations.Inject;
import com.da.web.core.DApp;

import java.util.ArrayList;
import java.util.List;


class User {
    String name;
    String password;

    public User(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}

@Component("userService")
class UserService {
    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        users.add(new User("admin", "admin"));
        users.add(new User("user", "user"));
        return users;
    }
}

//@Component("app")
public class NullPointerTestApp {
    @Inject("userService")
    private static UserService userService;

    public static void main(String[] args) {
        System.out.println("Starting test application...");

        DApp app = new DApp(NullPointerTestApp.class);
        app.use("/", ctx -> {
            System.out.println("Request received, userService=" + userService);
            if (userService != null) {
                System.out.println(userService.getUsers());
                ctx.send("hello world - " + userService.getUsers());
            } else {
                // 这个分支应该不会被执行，因为注入失败会抛出异常
                ctx.send("userService is null");
            }
        });
        app.listen();
        
        System.out.println("Server started. Test with: curl http://localhost:8080/");
    }
}
