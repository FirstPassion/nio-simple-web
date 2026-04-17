# AGENTS.md

## 构建命令
```bash
mvn clean package     # 构建 JAR 到 target/
mvn test              # 运行测试 (46 个用例)
mvn deploy            # 构建并发布到本地 repo/ 目录（非标准 Maven 行为）
```

## 关键约束

- **Java 8 严格兼容**: 禁止使用 Java 9+ API（如 `readAllBytes()`），编译使用 `-Dmaven.compiler.source=1.8`
- **版本**: 1.0.2，发布时需同步更新 `pom.xml` 中的 `<version>`

## 架构要点

- **入口类**: `com.da.web.core.DApp`
- **启动**: `new DApp(MyConfig.class).listen()` 或 `listen(port)`
- **路由优先级**: 显式路由 → 静态文件 → `@Path` Bean → 404
- **静态资源**: `src/main/resources/static/`（可通过 `app.properties` 配置）
- **自动端口探测**: 8080 起，端口占用时自动递增

## Bean 注入规则

- `@Path("/path")` - 自动将请求参数注入同名字段
- `@Inject("beanName")` - 注入其他 Bean 或配置值
- 字段无需 getter/setter，反射注入

## 测试
- 框架: JUnit 4
- 测试报告: `target/surefire-reports/`

## 路径处理
- Windows 使用 `\\`，Unix 使用 `/`，`Utils` 类自动处理