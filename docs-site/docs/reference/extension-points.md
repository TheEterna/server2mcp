> 维护者：api2mcp4j Team · 创建：2026-06-24（继承 ../real-agent 文档体系心法）
> 📖 参考文档 — 扩展点手册，按需查阅。所有接口签名均取自 `server2mcp-core` 真实代码（已逐类核对）。

# 扩展点手册

本框架为「非侵入、纯增强」设计，预留了四类官方扩展点。每个扩展点给出 **何时用 → 怎么用 → 真实签名**。

---

## 扩展点速览

| 扩展点 | 基类 / 接口 | 挂载方式 | 典型场景 |
|---|---|---|---|
| 自定义描述解析器 | `AbstractDesParser` | Spring Bean + `@Order` | 解析自有注解的工具描述 |
| 自定义参数解析器 | `AbstractParamParser` | Spring Bean + `@Order` | 解析自有注解的参数 schema |
| 自定义结果转换器 | `McpCallToolResultConverter` | `@McpTool(converter = ...)` | 自定义返回值 → `CallToolResult` |
| 自定义工具过滤器 | `@ToolScan` 的 `ToolFilter` | 注解属性 | 按注解/元注解粒度筛选方法 |
| 自定义 Context | `IRootContext` 等 `I{Type}Context` | 实现接口 + 注册 Bean | 接管实体注册/生命周期 |

---

## 一、自定义解析器（描述 / 参数）

### 何时用

现有解析器链覆盖了 `@McpTool`、Spring AI `@Tool`、Jackson、Javadoc、Swagger2/3。当你的工具用了**框架不认识的自定义注解**来描述接口语义（比如公司内部的 `@ApiDesc`），就需要写一个解析器接入责任链。

### 1.1 描述解析器

继承 `AbstractDesParser`，实现 `doDesParse`：

```java
package com.ai.plug.core.parser.tool.des;

public abstract class AbstractDesParser {
    // 默认兜底：把方法名按驼峰拆词作为描述
    public static String doDefaultParse(Method toolMethod, Class<?> toolClass) {
        return ParsingUtils.reConcatenateCamelCase(toolMethod.getName(), " ");
    }
    // 子类必须实现：返回解析出的工具描述
    public abstract String doDesParse(Method method, Class<?> toolClass);
}
```

实现示例：

```java
@Component
@Order(6)  // 排在内置链（0-5）之后，作为补充
public class MyApiDescDesParser extends AbstractDesParser {
    @Override
    public String doDesParse(Method method, Class<?> toolClass) {
        ApiDesc anno = method.getAnnotation(ApiDesc.class);
        return anno != null ? anno.value() : null;  // 返回 null 则降级给下一个解析器
    }
}
```

### 1.2 参数解析器

继承 `AbstractParamParser`（位于 `core.parser.tool.param`），方式与描述解析器一致，用 `@Order` 指定优先级。内置链 order 占用 0-6，自定义建议从 7 起。

### 优先级与生效规则

- **order 越小越先尝试**；返回非空即采用，否则降级到下一个解析器。
- 内置解析器通过 `@ConditionalOnParser` 受配置开关控制：
  - 描述链：`plugin.mcp.parser.des`
  - 参数链：`plugin.mcp.parser.param`（注意：配置键是单数 `param`）
- 你自定义的 Bean 若不加条件注解，则**始终生效**，与配置开关无关。

> ⚠️ 内置 order 真实映射（来源 `McpConfig`）：
> Des：`McpTool=0, Tool=1, Jackson=2, JavaDoc=3, Swagger3=4, Swagger2=5`
> Param：`McpTool=0, Tool=1, Mvc=2, Jackson=3, JavaDoc=4, Swagger3=5, Swagger2=6`

---

## 二、自定义结果转换器（McpCallToolResultConverter）

### 何时用

默认 `DefaultMcpCallToolResultConverter` 把方法返回值序列化为标准 `CallToolResult`。当你需要**自定义返回结构**（如统一包装、特定 MIME、图片/二进制内容块）时，实现该接口。

### 真实接口签名

```java
package com.ai.plug.core.spec.callback.tool;

public interface McpCallToolResultConverter {
    McpSchema.CallToolResult convertToCallToolResult(
        Object result,                          // 工具方法的原始返回值
        java.lang.reflect.Type returnType,      // 方法返回类型（含泛型）
        AbstractMcpToolMethodCallback callback  // 回调上下文（可取 name/inputSchema 等）
    );
}
```

### 怎么用

实现接口，并在 `@McpTool` 上指定：

```java
public class MyResultConverter implements McpCallToolResultConverter {
    @Override
    public McpSchema.CallToolResult convertToCallToolResult(
            Object result, Type returnType, AbstractMcpToolMethodCallback callback) {
        // 自定义包装逻辑
        return McpSchema.CallToolResult.builder()
                .addTextContent(JacksonUtils.toJson(result))
                .build();
    }
}
```

```java
@McpTool(name = "my_tool", converter = MyResultConverter.class)
public MyVO doSomething(@McpArg(name = "id") Long id) { ... }
```

> `@McpTool.converter()` 的默认值就是 `DefaultMcpCallToolResultConverter.class`（来源 `McpTool.java`）。不写即用默认。

---

## 三、自定义工具过滤器（@ToolScan ToolFilter）

### 何时用

一个类里往往有多个方法，你只想把**符合特定条件**的方法暴露为工具（例如「只暴露 POST 接口」「排除 `@Internal` 方法」）。`@ToolScan` 提供**类级**和**方法级**两层过滤。

### 真实注解结构（来源 `ToolScan.java`）

```java
public @interface ToolScan {
    String[] value()        default {};   // = basePackages（@AliasFor）
    String[] basePackages() default {};

    Filter[] includeFilters()      default {};   // 类级：包含哪些类
    Filter[] excludeFilters()      default {};   // 类级：排除哪些类
    ToolFilter[] includeToolFilters() default {}; // 方法级：包含哪些方法
    ToolFilter[] excludeToolFilters() default {}; // 方法级：排除哪些方法

    // 类级过滤类型
    @interface Filter {
        FilterType type() default FilterType.CLASS;     // CLASS | ANNOTATION
        Class<?>[] value()   default {};                // = classes（@AliasFor）
        Class<?>[] classes() default {};
    }
    // 方法级过滤类型
    @interface ToolFilter {
        ToolFilterType type() default ToolFilterType.ANNOTATION; // ANNOTATION | META_ANNOTATION
        Class<?>[] value()   default {};
        Class<?>[] classes() default {};
    }
    enum FilterType     { CLASS, ANNOTATION; }
    enum ToolFilterType { ANNOTATION, META_ANNOTATION; }
}
```

### 怎么用

```java
@Configuration
@ToolScan(
    basePackages = "com.example.api",
    // 类级：只扫描带 @Controller 的类
    includeFilters = @ToolScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class),
    // 方法级：只暴露带 @RequestMapping 元注解的方法（@GetMapping/@PostMapping 都是其元注解）
    includeToolFilters = @ToolScan.ToolFilter(type = ToolFilterType.META_ANNOTATION, classes = RequestMapping.class)
)
public class McpToolScanConfig {}
```

- **`ANNOTATION`**：方法上直接标注了该注解。
- **`META_ANNOTATION`**：方法上的注解「自身被」该注解标注（如 `@PostMapping` 的元注解是 `@RequestMapping`）。

> 过滤元数据由 `ToolRegisterDefinition` 携带进注册链，在 `McpToolProvider` 创建 Specification 时按扫描组生效（见 architecture.md 模式 5）。

---

## 四、自定义 Context（IRootContext 等）

### 何时用

`Context` 接管某类 MCP 实体的注册定义与运行期生命周期。最典型的是 **Root 生命周期管理**——当你要自定义「客户端 Roots 的读取/更新/设置」逻辑时，实现 `IRootContext`。

### 真实接口签名（来源 `IRootContext.java`）

```java
package com.ai.plug.core.context.root;

public interface IRootContext {
    // 从客户端 exchange 读取 Roots
    List<McpSchema.Root> getRoots(Object exchange);
    // 更新 Roots
    void updateRoots(Object exchange, List<McpSchema.Root> roots);
    // 设置 Roots
    void setRoots(Object exchange, List<McpSchema.Root> roots);
}
```

### 怎么用

实现接口并注册为 Spring Bean，覆盖默认 `RootContext`：

```java
@Component
public class MyRootContext implements IRootContext {
    @Override public List<McpSchema.Root> getRoots(Object exchange) { ... }
    @Override public void updateRoots(Object exchange, List<McpSchema.Root> roots) { ... }
    @Override public void setRoots(Object exchange, List<McpSchema.Root> roots) { ... }
}
```

> 其余实体的 Context（`IToolContext` / `IResourceContext` / `IPromptContext` / `ICompleteContext`）遵循同样的「接口 → 工厂 → 实现」三件套，详见 architecture.md 模式 3。一般无需替换，除非你要彻底接管注册行为。

---

## 五、扩展时的红线提醒

1. **解析器返回 null 即降级**——不要在解析器里抛异常打断整条链，除非确实是致命错误。
2. **order 不要与内置冲突**——内置已占用 Des `0-5` / Param `0-6`，自定义从更大值开始。
3. **转换器要处理 null 返回值**——工具方法可能返回 `void` 或 `null`。
4. **Sync/Async 双模式**——若扩展涉及 Exchange，注意区分 `McpSyncServerExchange` 与 `McpAsyncServerExchange`（由回调子类区分，见 architecture.md 模式 4/6）。

---

## 延伸阅读

- 架构总览与处理链路：`docs/reference/architecture.md`
- 新人/新 AI 上手：`docs/reference/onboarding.md`
- 权威事实源：项目根 `CLAUDE.md`
