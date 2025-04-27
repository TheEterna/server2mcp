# Server2MCP Spring Boot Starter

[English](README.md)

## 这绝对是一个革命性的想法!!!

这是一个用于自动化集成 MCP (Model Control Protocol) 服务的 Spring Boot Starter。

# ✨功能特性

- 自动配置 MCP 服务，类似mybatis-plus于mybatis的关系，无侵入，纯增强
- 支持一切mcp的javaSdk原生功能，提供工具注册和回调机制等等
- 支持自定义工具解析
- 用户可自定义Parser，而无关责任链实现，完成独特接口注解的属性解析

# 👀未完善点

    仍有许多springai的解析扩展点未接入，比如目前只完成了javadoc版（注意：并不成熟，后续将提供解决方案，因为jar包部署，源码中不含注释），相关解析逻辑不断更新中，一周后将会推出第一版v0.0.1，但解析架构使用责任链加模板，极易扩展，后续将集成各种主流方法描述注解的解析

# 🎯快速开始

   因为还没有推到中心仓库，可以把源码下载下来之后，进行 mvn clean install 后进行依赖引用

## 添加依赖

    <dependency>
        <groupId>com.ai.plug</groupId>
        <artifactId>server2mcp-spring-boot-starter</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

然后在配置文件中添加配置：

```
plugin.mcp.enabled=true

# 如果是yml，则为
plugin:
  mcp:
    enabled: true
```

    以上就是该项目启动的最基本配置，它包含了所有的原生配置如spring.ai.mcp.server.name等等，它默认会将你的所有启动类路径下的所有controller注册为mcp接口，如果接口方法或类上有@Deprecated注解将不会注册。

# 📚原理

    可以理解为把接口开放给ai，所以这些接口和普通接口一样，只是可以通过ai调用，相关知识文档：[Model Context Protocol (MCP) :: Spring AI Reference ](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) 和 [Introduction - Model Context Protocol](https://modelcontextprotocol.io/introduction)

# 💕最佳实践

    拥有了该框架，你不必再去从0-1重新建造一个mcp服务应用，也不必高耦合的复制代码添加@Tool注解，或者在源代码上添加mcp功能，你只需要根据一个配置类添加自定义的@ToolScan注解，就可以轻松的完成mcp接口的注册，如果遇到mcp的SDK 改版怎么办，这不用担心，核心内容由我维护，且使用方式不变

    1. 你可以轻松的去构建一个多智能体应用，只需要多个你自定义的ai对话接口，然后只需要在客户端进行相应的mcp接口调用即可轻松完成。

    2. 可以为你的管理系统，快速接入ai对话调用，高自定义性，你无需关注任何ai领域细节，只需要关注你最擅长的领域web及前端，就可以完成炫酷的效果，这样一比，DB-GPT这样的应用是不是显得高度笨重且难以扩展

    3. 可以配合cursor这种简易的mcp客户端，轻松的完成接口调试

# 🔔总结

    这个框架实际上非常简单，可能代码有很多漏洞与不足，还请见谅。

# 📄 版权声明/开源协议

    根据 [Apache 2.0 许可证](https://www.apache.org/licenses/LICENSE-2.0.html)发布的代码
