# 测试规范 [ENFORCED]

> 来源：继承自 ../real-agent docs/specs/TEST_SPEC.md · 董事长 2026-06-24 批准方案B全套继承
> 强制度：[ENFORCED]
>
> **适用范围**：本项目（api2mcp4j / server2mcp）是纯 Java 库。本规范只约束 Java/JUnit/Maven 测试，剥离全部前端（Vitest/Storybook/MSW）内容。

**版本**：1.0（继承自 real-agent v1.2，剥离前端，适配 Spring Boot Starter 库）
**最后更新**：2026-06-24

---

## 一、测试金字塔

| 层级 | 占比 | 定义（本项目语境） | 工具 |
|------|------|-------------------|------|
| **单元测试** | 80% | 单类/单方法，无 Spring 容器——解析器、工具类、Schema 生成、命名生成 | JUnit 5 + Mockito + AssertJ |
| **集成测试** | 15% | 多层协作——扫描注册链路、`@ConditionalOnParser` 激活、Context 注册、Provider 暴露 | `@SpringBootTest` + `mcp-test`（MCP SDK 官方测试支持） |
| **E2E 测试** | 5% | 端到端——`server2mcp-test` 演示应用真实启动并验证 MCP 工具可调用 | `server2mcp-test` 演示应用 |

> **测试集中在 server2mcp-core**（CLAUDE.md 已声明"测试位于 server2mcp-core"）。`server2mcp-test` 是演示/手动验证应用，不在根 pom modules 中，需单独构建。

---

## 二、依赖现状（真实勘察 2026-06-24）

| 依赖 | 来源 | 提供 |
|------|------|------|
| `spring-boot-starter-test` | `server2mcp-core/pom.xml:83`（`scope=test`） | JUnit 5 + Mockito + AssertJ + Spring Test（传递引入，开箱即用） |
| `mcp-test` | `server2mcp-core/pom.xml`（`scope=test`） | MCP Java SDK 官方测试支持，用于集成层验证 |

> **现状诚实声明**：当前 `server2mcp-core/src/test` 有 3 个测试类——`GenSchemaUtilsTest`（2 个 `@Test`）/ `ElicitationTests` / `WebMvcSseSyncServerTransportTests`（另有 `TestClass` 为夹具，非测试类）。部分仍使用 JUnit 原生 `Assertions` + `System.out` 打印式验证（探索性写法）。本规范确立**目标标准**（AssertJ 断言 + 无 `System.out` 断言），存量探索性测试渐进式补强，新增测试一律遵循本规范。

---

## 三、文件组织与命名

```
server2mcp-{module}/src/test/java/
└── com/ai/plug/{module}/
    └── {package}/                    # 与源文件同包结构
        └── {ClassName}Test.java
```

**命名铁律**：`{被测类名}Test.java`（JUnit 5 标准，与源文件同名 + `Test` 后缀）

**示例**：
- `GenSchemaUtilsTest`（测 `GenSchemaUtils`）
- `McpToolDesParserTest`（测 `McpToolDesParser`）
- `ClassPathToolScannerTest`（测扫描注册链路）

> **禁止**：`TestXxx`、`XxxTests`、`XxxIT` 混用——统一 `XxxTest`。

---

## 四、各类测试模式

### 4.1 解析器单元测试（最高频 · 责任链一环）

> 解析器是本框架最常新增的扩展点（见 [REGISTRATION_DISCIPLINE_SPEC.md](./REGISTRATION_DISCIPLINE_SPEC.md) 利器 A），每个新解析器必须有对应测试。

```java
@DisplayName("McpToolDesParser 单元测试")
class McpToolDesParserTest {

    private final McpToolDesParser parser = new McpToolDesParser();

    @Test
    @DisplayName("1. @McpTool.description 有值 — 返回该描述")
    void testDescriptionPresent() throws NoSuchMethodException {
        Method method = SampleTool.class.getMethod("annotated");
        String des = parser.doDesParse(method, SampleTool.class);
        assertThat(des).isEqualTo("查询天气");
    }

    @Test
    @DisplayName("2. 无 @McpTool 注解 — 返回 null 交给责任链下一环")
    void testNoAnnotation() throws NoSuchMethodException {
        Method method = SampleTool.class.getMethod("plain");
        String des = parser.doDesParse(method, SampleTool.class);
        assertThat(des).isNull();
    }
}
```

### 4.2 Mockito 单元测试（依赖隔离）

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("XxxBuilder 单元测试")
class XxxBuilderTest {
    @Mock private IToolContext toolContext;
    private XxxBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new XxxBuilder(toolContext);
    }

    @Test
    @DisplayName("正常场景 — 返回预期结果")
    void testNormalCase() {
        when(toolContext.getRawTools()).thenReturn(Map.of());
        assertThat(builder.build()).isNotNull();
    }
}
```

### 4.3 注册链路集成测试（@SpringBootTest）

> 验证本框架灵魂——扫描 → 注册 → 暴露 全链。

```java
@SpringBootTest
@DisplayName("工具扫描注册集成测试")
class ToolScanIntegrationTest {

    @Autowired private IToolContext toolContext;

    @Test
    @DisplayName("interface 作用域下 @RestController 方法被注册为工具")
    void controllerMethodsRegistered() {
        assertThat(toolContext.getRawTools())
            .containsKey("demoController_hello");
    }
}
```

### 4.4 `@ConditionalOnParser` 激活测试（配置驱动）

> 验证 [REGISTRATION_DISCIPLINE_SPEC.md](./REGISTRATION_DISCIPLINE_SPEC.md) V-2 缺口——配置正确时解析器激活、配置不含时不激活。

```java
@SpringBootTest(properties = "plugin.mcp.parser.des=MCPTOOL,JAVADOC")
class ParserActivationTest {
    @Autowired private List<AbstractDesParser> desParsers;

    @Test
    @DisplayName("配置仅含 MCPTOOL,JAVADOC 时 — Swagger 解析器不激活")
    void swaggerNotActivated() {
        assertThat(desParsers)
            .noneMatch(p -> p instanceof Swagger3DesParser);
    }
}
```

### 4.5 WebFlux Starter 响应式断言（仅 webflux 场景）

> `server2mcp-starter-webflux` 引入 `spring-ai-starter-mcp-server-webflux`（响应式）。涉及 `AsyncMcpToolMethodCallback` 等异步回调的测试，**禁止 `.block()`**，用 Reactor `StepVerifier`（经 `reactor-test` / `spring-boot-starter-test` 提供）：

```java
StepVerifier.create(asyncCallback.call(request))
    .expectNextMatches(result -> result.isSuccess())
    .verifyComplete();
```

> SYNC 模式（webmvc）的回调走同步路径，直接 AssertJ 断言即可，无需 StepVerifier。

---

## 五、断言库

| 场景 | 库 | 风格 |
|------|------|------|
| 标准断言 | AssertJ | `assertThat(value).isEqualTo(expected)` ✅ 目标标准 |
| 异常断言 | AssertJ | `assertThatThrownBy(() -> ...).isInstanceOf(X.class).hasMessageContaining("...")` |
| 响应式（webflux） | Reactor StepVerifier | `StepVerifier.create(mono).expectNext(...).verifyComplete()` |
| ❌ 避免 | `System.out.println` 式"肉眼验证" | 必须改为断言（存量 `GenSchemaUtilsTest` 待补强） |

---

## 六、测试文件头要求

所有测试类必须有文件头，`@layer` 标记为 `test`（见 [FILE_HEADER_SPEC.md](./FILE_HEADER_SPEC.md) §六）：

```java
/**
 * @header-start
 * @module McpToolDesParserTest
 * @keywords 单元测试 unit-test 描述解析 McpToolDesParser 责任链 @McpTool JUnit5 AssertJ
 * @updated 2026-06-24 11:10
 * @layer test
 * @purpose 验证 McpToolDesParser 在有/无 @McpTool 注解时的描述解析行为
 * @header-end
 */
```

---

## 七、覆盖率目标

| 阶段 | 目标 | 执行方式 |
|------|------|---------|
| **当前**（2026-06） | 不强制（仅 1 个测试类，处于起步期） | 手动运行，无门禁 |
| **目标** | 核心注册链路 + 全部解析器有测试；新增代码 60% 行覆盖 | 新增 PR 必带对应 `XxxTest` |

> **优先补测对象**（核心引擎，AI 最常改动）：解析器责任链（des/param）、`ClassPathToolScanner` 扫描、`IToolContext` 注册、`McpToolProvider` 两级过滤、`GenSchemaUtils` Schema 生成。

---

## 八、本地运行命令（本项目真实命令）

```bash
# 全部测试（测试位于 server2mcp-core）
cd server2mcp-core && mvn test

# 运行单个测试类
cd server2mcp-core && mvn test -Dtest=GenSchemaUtilsTest

# 运行单个测试方法
cd server2mcp-core && mvn test -Dtest=GenSchemaUtilsTest#testMcpSchemaGenerator

# 构建全部模块（SNAPSHOT 依赖须本地安装）
mvn clean install

# 演示应用手动验证（不在根 pom modules，需单独构建）
cd server2mcp-test && mvn clean package
```

> **环境要求**：Java 17；根 pom 全局启用 `-parameters` 编译参数（`pom.xml:175`，反射获取方法参数名，测试同样依赖此参数）。

---

## 九、TDD 可追溯性铁律 [ENFORCED]

> 来源：继承自 real-agent · **TDD 的可审计性由 commit 快照提供，不是由本地执行记忆提供。**

### 9.1 核心原则

断言语义上的"Red 意图"不能替代 git 历史上的"Red 事实"。审计者必须能 `git show HEAD~1` 看到失败测试，`git checkout <red-hash> && mvn test` 必须复现 Red。

### 9.2 双 commit 签名模式（最少双 commit）

```
<hash-A>  [RED]   feat(core): 新增 XX 解析器 — 写失败断言验证 ${行为}
<hash-B>  [GREEN] feat(core): 新增 XX 解析器 — 最小实现通过 ${行为}
```

可选第三 commit：

```
<hash-C>  [REFACTOR] refactor(core): XX 解析器重构 — 保持行为不变
```

### 9.3 签名前缀含义

| 前缀 | 内容约束 | 测试预期 |
|------|---------|---------|
| `[RED]` | 仅新增/修改测试，无生产代码变更 | `mvn test` 红（允许） |
| `[GREEN]` | 生产代码变更，使已有 Red 测试转绿 | `mvn test` 绿 |
| `[REFACTOR]` | 结构调整，无行为变化 | `mvn test` 绿 |

> **禁止**：Red/Green 合并为单 commit。御史台一票否决，须拆分或明示豁免理由。

### 9.4 验证方式

- `git log --grep="\[RED\]"` 可枚举所有 Red commit
- `git show <red-hash>` 必须展示失败断言
- `git checkout <red-hash> && cd server2mcp-core && mvn test -Dtest=<TestClass>` 必须复现 Red

### 9.5 豁免场景

- 纯文档修改
- 紧急 hotfix（PR 描述声明豁免理由）
- 回归测试补齐（明示"非 TDD · 历史欠账"标签，如为存量 `GenSchemaUtilsTest` 补强断言）

---

## 十、P0/P1 修复方法论（继承核心心法）

> 来源：继承自 real-agent · 适配本框架 SDK SNAPSHOT 易变特性。

### 10.1 分支完备性原则（Branch Completeness）

P0/P1 级修复的单测必须同时满足：
1. **正常路径**（happy path）完整
2. **所有异常路径**覆盖（null / 边界 / 无注解 / 解析失败 / 责任链穿透到底无人处理）
3. 至少一条**真容器集成测试**（`@SpringBootTest`，非纯 mock）

**审查清单**（reviewer 必列三栏）：

| 分支类别 | 是否覆盖 | 单测文件:行号 |
|---------|---------|-------------|
| 正常路径 | ✓/✗ | ... |
| 异常路径（null/边界/无注解/解析失败） | ✓/✗ | ... |
| 端到端（真容器 @SpringBootTest） | ✓/✗ | ... |

### 10.2 SDK 破坏性变更专项（本框架特有风险）

> Spring AI 1.1.0-SNAPSHOT + MCP Java SDK 0.14.0-SNAPSHOT，**预期会有 API 破坏性变更**（CLAUDE.md 已警示）。

- 升级 SDK 版本后，**必须全量 `mvn clean install`** 再跑测试，禁止靠本地缓存判断
- 涉及 `McpSchema.Tool` 等 SDK 类型签名变更时（参见 `McpToolProvider.java:87` OutputSchema 被注释的历史），RCA 须基于真实 jar 反编译 / SDK 源码，不靠推理
- SDK 升级专题工作总结落 `docs/logs/sdk-upgrade/`（见 [WORK_LOG_SPEC.md](./WORK_LOG_SPEC.md)）

### 10.3 审查时序原则（Review Temporal Alignment）

审查员的静态审查必须基于**最后一次 `mvn test/install` 跑的源码状态**。审查前三步：
1. **最新声明回溯**：查 executor 最新 SendMessage / 工作总结确认当前声明状态
2. **当前状态核查**：`ls` / `grep` / `mvn test` 验证文件系统与 build artifact
3. **一致性对比**：#1 与 #2 一致 → 正常；不一致 → 才可能存在"交付声明与事实不符"

### 10.4 正面案例：异常类型变化断言模式

不断言"不抛异常"，断言"异常类型从致命变可恢复 / 走合理 fail-fast"：

```java
assertThatThrownBy(() -> parser.doDesParse(badMethod, BadClass.class))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("参数名缺失")
    .hasMessageNotContaining("NullPointerException");
```

---

## 十一、验证方法

- [ ] `cd server2mcp-core && mvn test` 全绿
- [ ] 新增解析器/扫描器/Context 是否有对应 `XxxTest`？（防"只建类不测"）
- [ ] 测试类命名是否统一 `XxxTest`（无 `TestXxx`/`XxxTests` 混用）？
- [ ] 测试是否用 AssertJ 断言而非 `System.out` 肉眼验证？
- [ ] webflux 异步回调测试是否用 `StepVerifier` 而非 `.block()`？
- [ ] TDD 任务：`git log --grep="\[RED\]"` 能否枚举到 Red commit，且可 checkout 复现 Red？

---

## 十二、与其他规范的关系

| 规范 | 关系 |
|------|------|
| [REGISTRATION_DISCIPLINE_SPEC.md](./REGISTRATION_DISCIPLINE_SPEC.md) | 新增解析器/Context 必带测试是注册纪律的验收闭环；§4.4 测 V-2 缺口 |
| [WORK_LOG_SPEC.md](./WORK_LOG_SPEC.md) | 交付声明三件套含 `mvn test`；TDD 双 commit 是兑现率代码源的可追溯凭证 |
| [FILE_HEADER_SPEC.md](./FILE_HEADER_SPEC.md) | 测试文件 `@layer` 统一标 `test` |

---

**维护者**：api2mcp4j Team
**文档版本**：1.0（2026-06-24 · 继承 real-agent v1.2，剥离前端，适配 Java 库 + SDK SNAPSHOT 风险专项）
