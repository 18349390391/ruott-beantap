# ruott-beantap 使用说明

在本机已启动的 Spring Boot 应用里，让 JUnit 测试类可以像调用本地对象一样，直接调用**运行中应用容器里的真实 bean**（含真实的数据源、缓存、RPC 等依赖），无需在测试侧启动 Spring 容器。

## 为什么做这个工具

**1. `@SpringBootTest` 太慢。** 传统集成测试每跑一次都要完整启动一遍 Spring 容器。想反复调一个单测、或分批跑多个单测时，容器就要反复启停，大量时间浪费在等待启动上。使用本工具：应用只启动一次，之后每个测试用例都是毫秒级的远程调用，随改随跑。

**2. service 层方法缺少顺手的测试入口。** 开发中会写大量 service 层方法，当这些方法通过 Feign / Dubbo 对外提供时，想本地验证一下就得专门写个 controller 接口、或者在本地搭 dubbo 消费端来调，非常繁琐。本工具让测试类直接拿到容器里的 bean 调用，一行多余代码都不用写。

**3. 顺带带来的便利：**

- **真实环境验证**：调用发生在正在运行的应用里，走的是真实的数据库连接、Redis、Dubbo 引用、Nacos 配置，不是 mock 出来的假象。
- **事务/AOP 行为可验证**：调的是 Spring 代理后的 bean，事务、切面逻辑和线上行为一致。
- **随手的"线上按钮"**：除了写单测，还可以用它来触发本地/开发环境应用里的任意 bean 方法——手动触发一次定时任务逻辑、修数、验证某个分支，都不需要临时暴露 HTTP 接口。
- **测试代码极简**：两个注解搞定，不写 `@ExtendWith`，不启容器，新同事看一眼就会用。

## 环境要求

- JDK 17+
- 被测应用：Spring Boot **2.7.x 及以上**
- 应用需配置 `spring.application.name`
- 应用和测试运行在**同一台机器、同一系统用户**下

## 引入依赖与启动方式

本工具仅供测试使用，**不允许打进业务 jar、不允许带到生产环境**。围绕这个约束，有两种引入 + 启动的组合方案，任选其一。

### 方案一（推荐）：`test` scope + `spring-boot:run` 启动

引入依赖，声明为 `test` scope：

```xml
<dependency>
    <groupId>com.ruott</groupId>
    <artifactId>ruott-beantap</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

同时在 `pom.xml` 的 `spring-boot-maven-plugin` 中加上 `useTestClasspath`：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <useTestClasspath>true</useTestClasspath>
            </configuration>
        </plugin>
    </plugins>
</build>
```

启动被测应用：

```bash
mvn spring-boot:run
```

在 IDEA 里也可以不用终端，两种方式任选：

- 右侧 **Maven 面板** → 项目 → `Plugins` → `spring-boot` → `spring-boot:run`，双击启动；
- `Run → Edit Configurations` → `+` → **Maven**，Command line 填 `spring-boot:run`，保存为运行配置。以后点一下就能启动，还能直接 Debug。

**原理**：`spring-boot:run` 不是运行打好的 jar，而是用 Maven 解析出的 classpath 原地启动 main 类。它默认用生产 classpath（compile + runtime），`useTestClasspath=true` 让它改用测试 classpath（额外包含 test scope 依赖），于是 beantap 被加进类路径。该参数只影响这次启动，完全不参与打包——`repackage` 永远只收 compile/runtime scope，test 依赖天然被排除。所以同一份 pom：启动时有 beantap，打出的 jar 里没有。

> **注意**：不要在 IDEA 里直接点 `main` 方法启动（Application / Spring Boot 运行配置都不行）。IDEA 的运行配置只使用生产 classpath，没有等价于 `useTestClasspath` 的开关，test scope 的 beantap 不会被加载，端点不会启动。

> **多模块工程注意：先 install 父工程。** `spring-boot:run` 启动前会先执行到 `test-compile` 阶段，需要能解析到所有依赖。如果启动模块依赖了同项目的兄弟模块（SNAPSHOT），而本地仓库里没有对应构件，会直接编译/解析失败（报"程序包 xxx 不存在"）。每次改动了兄弟模块后，先在父工程根目录执行：
>
> ```bash
> mvn install -DskipTests
> ```
>
> 再对启动模块执行 `mvn spring-boot:run`。单模块工程无此问题，直接 run 即可（run 会自动编译本模块代码，不需要提前 package）。

### 方案二：普通 scope + IDEA 直接启动（打包前需手动处理）

依赖不写 scope（即默认 compile）：

```xml
<dependency>
    <groupId>com.ruott</groupId>
    <artifactId>ruott-beantap</artifactId>
    <version>1.0.0</version>
</dependency>
```

此时不依赖 `useTestClasspath`，可以在 IDEA 里直接运行 `main` 方法启动，beantap 端点会随应用拉起。

**代价**：compile scope 会被打进业务 jar。所以每次 `mvn package` / `deploy` / 发布之前，**必须手动把这段依赖注释掉**，打完包再恢复。容易遗忘，有把测试工具带进生产环境的风险，只建议在临时调试时使用。

### 验证启动成功

启动日志中出现：

```
http server started on port xxxxx
```

说明端点已拉起，可以跑测试了。`java -jar xxx.jar` 方式启动时 test scope 依赖同样不在 classpath，端点不会启动（这也正是我们要的效果）。

## 编写测试

```java
import org.junit.jupiter.api.Test;

@BeantapInit
public class BeantapTest {

    @TestBeantap
    private ProcessInstanceApi processInstanceApi;

    @Test
    public void test() {
        SingleResponse<UserProcessCheckInfo> resp =
                processInstanceApi.checkUserProcessStatus("100081:1000810003", "2088140816333803520");
        System.out.println(resp.getData());
    }
}
```

- `@BeantapInit`：标在测试类上，开启本工具的字段注入。无需 `@ExtendWith`、无需 `@SpringBootTest`，测试本身不启动 Spring 容器。
- `@TestBeantap`：标在字段上，接口或具体类均可。
- `@Test`：@Test注解需要使用`import org.junit.jupiter.api.Test;`包下的，否则会不生效。

## bean 定位规则（`name` 属性）

`@TestBeantap` 的 `name` 决定服务端如何找到目标 bean：

| 场景 | 行为 |
|---|---|
| 显式写了 `name` | **严格**按该名字查找，找不到直接报错（写错名字不会被静默兜底到其他 bean） |
| 不写 `name` | 先按默认名找（字段类型首字母小写，如 `ProcessInstanceApi` → `processInstanceApi`） |
| 默认名不存在 | 按字段类型匹配容器中的实现 bean（如自动命中 `ProcessInstanceImpl`） |
| 类型匹配到多个实现 | 报错并提示实现列表，要求显式指定 `name` |

经验法则：**单实现的接口什么都不用写；一个接口有多个实现时，必须用 `name` 消歧。**

```java
// 多实现时
@TestBeantap(name = "processInstanceImpl")
private ProcessInstanceApi processInstanceApi;
```

## 注意事项与限制

- 方法参数和返回值需要可 JSON 序列化。
- 目标方法内部抛出的异常会原样带回测试端，以 `BeanTapRunTimeException` 抛出，可在测试控制台看到真实异常信息。
- 被事务/AOP 代理过的 bean 可以正常调用。
- 字段类型为 `final` 类或方法为 `final` 时无法代理，改用接口字段。
- 测试端报 `Failed to read beantap port file` 时，先检查被测应用是否按上文「方案一/方案二」正确启动、启动日志中是否出现 `http server started on port`（绝大多数情况是用 IDEA 直接跑了 `main` 方法，导致 test scope 的 beantap 不在 classpath）。
