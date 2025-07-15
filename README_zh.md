# Server2MCP

[English](README.md)

[文档链接](https://theeterna.github.io/server2mcp-docs/)

## 这绝对是一个革命性的想法!!!

这是一个用于自动化集成 MCP (Model Control Protocol) 服务的 Spring Boot Starter。


# 设计思路
- 对Tool注册提供二级Filter, 因为对工具(几乎等同于方法)定义宽泛, 基本不需要专有注解即可注册出一个工具, 这必须提供更细粒度的过滤方式, 比如在一个类中, 我只想注册post接口的接口为工具. 但Resource等等更像是一种范式化的资源只是通过方法的方式表示和注册, 所以直接使用@McpResource等对应注解进行注册即可

# ✨功能特性

- 自动配置 MCP 服务，类似mybatis-plus于mybatis的关系，无侵入，纯增强
- 支持一切mcp的javaSdk原生功能，提供工具注册和回调机制等等
- 支持自定义 工具注册，通过@ToolScan注解
- 支持自定义 资源注册，通过@ResourceScan注解
- 支持自定义 提示词注册，通过@PromptScan注解
- 用户可自定义Parser，而无关责任链实现，完成独特接口注解的属性解析
- 拥有javadoc，swagger2，swagger3，springmvc（只负责部分解析逻辑），jackson 以及springai原生的tool 解析器

# ️🌟最新特性
- 对标最新mcp-sdk版本 0.11的快照版本, 自定义注解来体验最新特性, McpTool
- McpTool 完全隔离springai的Tool环境, 实现独属于Mcp的Tool体系, 方法自动注入exchange, 轻松完成mcp客户端交互
- 暂未支持springai的配置类mineType, 融合在McpTool的注解属性中

# 👀未完善点
已经具有较为成熟的解析逻辑，目前主流解析逻辑已经较为完善，如果有我没考虑到的，欢迎踊跃交流，后续将会提供强有力的自定义扩展功能，用户可以通过简单的配置类等方式，来完成自定义注释注解的解析。

# 🎯快速开始

   因为还没有推到中心仓库，可以把源码下载下来之后，在 server2mcp-starter-webmvc 文件夹下 进行 mvn clean install 后进行依赖引用

## 添加依赖

    <dependency>
        <groupId>com.ai.plug</groupId>
        <artifactId>server2mcp-starter-webmvc</artifactId>
        <version>1.0.0</version>
    </dependency>

然后在配置文件中添加配置：

```yaml
plugin:
  mcp:
    enabled: true
    parser:
      param: JAVADOC, TOOL, SPRINGMVC, JACKSON, SWAGGER2, SWAGGER3  # 可不填 ，默认注册除JAVADOC之外的解析器
      des:  JAVADOC, TOOL, JACKSON, SWAGGER3, SWAGGER2     # 可不填 ，默认注册除JAVADOC之外的解析器
    scope: interface # 有两种配置，custom和interface，默认interface，会预先注册controller下的接口为工具；custom 则不会预先注册工具

```

    以上就是该项目启动的最基本配置，它包含了所有的原生配置如spring.ai.mcp.server.name等等，interface配置它默认会将你的所有启动类路径下的所有controller注册为mcp接口，如果接口方法或类上有@Deprecated注解将不会注册。

## 注意：JAVADOC解析器

    javadoc的解析逻辑本质上就是通过解析源码文件，而上线之后java代码是以字节码 class文件的形式存在，所以javadoc就无法使用，但是javadoc的注解方式在开发者中间还是比较流行的，所以不能够完全舍弃。现给出解决方案，要使用javadoc方式的解析器，必须将源码打包到资源目录里，如果你使用maven，需要加一段打包配置，如下所示：

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <executions>
                    <execution>
                        <id>copy-java-sources</id>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>copy-resources</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                            <resources>
                                <resource>
                                    <directory>src/main/java</directory>
                                    <includes>
                                        <include>**/*.java</include>
                                    </includes>
                                </resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            
```



# 📚原理

    可以理解为把接口开放给ai，所以这些接口和普通接口一样，只是可以通过ai调用，相关知识文档：[Model Context Protocol (MCP) :: Spring AI Reference ](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) 和 [Introduction - Model Context Protocol](https://modelcontextprotocol.io/introduction)

# 💕最佳实践

    拥有了该框架，你不必再去从0-1重新建造一个mcp服务应用，也不必高耦合的复制代码添加@Tool注解，或者在源代码上添加mcp功能，你只需要根据一个配置类添加自定义的@ToolScan注解，就可以轻松的完成mcp接口的注册，如果遇到mcp的SDK 改版怎么办，这不用担心，核心内容由我维护，且使用方式不变

    1. 你可以轻松的去构建一个多智能体应用，只需要多个你自定义的ai对话接口，然后只需要在客户端进行相应的mcp接口调用即可轻松完成。

    2. 可以为你的管理系统，快速接入ai对话调用，高自定义性，你无需关注任何ai领域细节，只需要关注你最擅长的领域web及前端，就可以完成炫酷的效果
    3. 可以配合cursor这种简易的mcp客户端，轻松的完成接口调试



# 📄 版权声明/开源协议

    根据 [Apache 2.0 许可证](https://www.apache.org/licenses/LICENSE-2.0.html)发布的代码


