# Server2MCP Spring Boot Starter

这是一个用于集成 MCP (Model Control Protocol) 服务的 Spring Boot Starter。

## 功能特性

- 自动配置 MCP 服务
- 提供工具注册和回调机制
- 支持自定义工具解析

## 使用方法

### 添加依赖

```xml
<dependency>
    <groupId>com.ai.plug</groupId>
    <artifactId>server2mcp-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 总结

完成以上步骤后，您的项目将成为一个标准的 Spring Boot Starter，可以被其他项目引用并自动配置。主要改造点包括：

1. 创建自动配置类
2. 添加 spring.factories 文件
3. 完善配置元数据
4. 创建配置属性类
5. 更新 POM 文件
6. 移除主应用类
7. 添加使用说明文档

这样，其他项目只需要添加您的 Starter 依赖，就可以自动获得 MCP 相关的功能，无需额外配置。