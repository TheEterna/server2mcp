# Server2MCP Spring Boot Starter

[简体中文](README_zh.md)

## This is definitely a revolutionary idea!!!

  This is a Spring Boot Starter used for automated integration of MCP (Model Control Protocol) services.

# ✨function characteristics

* Automatic configuration of MCP service, similar to the relationship between mybatis plus and mybatis, non-invasive, purely enhanced
* Supports all native functions of MCP in JavaSdk, providing tool registration and callback mechanisms, etc
* Support custom tool registration, annotated through @ ToolScan
* Users can customize the Parser without relying on chain of responsibility implementation to complete attribute parsing for unique interface annotations
* Having Javadoc, Swagger2, Swagger3, SpringMVC (only responsible for partial parsing logic), Jackson, and Springai native tool parsers

# 👀Unfinished points

  There are still many parsing extension points of Springai that have not been integrated. For example, currently only Javadoc version annotations have been completed, but the parsing architecture uses responsibility chains and templates, which are extremely easy to extend. In the future, various mainstream methods will be integrated to describe annotation parsing

# 🎯Quick start

  Since it has not been pushed to the central repository yet, you can download the source code, In the folder 'server 2mcp starter webmvc' perform an mvn clean install, and then make dependency references

## Add Dependency

    <dependency>
        <groupId>com.ai.plug</groupId>
        <artifactId>server2mcp-starter-webmvc</artifactId>
        <version>1.0.0</version>
    </dependency>

Then add the configuration in the configuration file:

```yaml
plugin:
  mcp:
    enabled: true
    parser:
      params: JAVADOC, TOOL, SpringMVC, JACKSON, SWAGGER2, SWAGGER3 # optional, default registration for parsers other than JAVADOC
      des: JAVADOC, TOOL, JACKSON, SWAGGER3, SWAGGER2 # optional, default registration for parsers other than JAVADOC
    scope: interface # There are two configurations, custom and interface. The default interface is pre registered as a tool under the controller; Custom does not pre register tools
```

  The above is the basic configuration for starting the project, which includes all native configurations such as spring.ai.mcp.server-side, etc. The interface configuration will register all controllers under your startup class path as MCP interfaces by default. If there is an @ Depreciated annotation on the interface method or class, it will not be registered.

## NOTICE：JAVADOC Parser

  The parsing logic of Javadoc is essentially to parse source code files, and after going live, Java code exists in the form of bytecode class files, so Javadoc cannot be used. However, Javadoc's annotation method is still quite popular among developers, so it cannot be completely abandoned. Here is a solution. To use a Javadoc parser, you must package the source code into a resource directory. If you use Maven, you need to add a packaging configuration as follows:

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

# 📚principle

  It can be understood as opening up interfaces to AI, so these interfaces are the same as regular interfaces, except that they can be called through AI. Relevant knowledge documents:[Model Context Protocol (MCP) :: Spring AI Reference ](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) , [Introduction - Model Context Protocol](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html）And [Introduction - Model Context Protocol]（https://modelcontextprotocol.io/introduction)

# 💕Best practices

  With this framework, you no longer need to rebuild an MCP service application from scratch, nor do you need to add @ Tool annotations to highly coupled copied code, or add MCP functionality to source code. You only need to add custom @ ToolScan annotations based on a configuration class to easily complete the registration of MCP interfaces. What should you do if you encounter MCP SDK revisions? Don't worry, the core content is maintained by me, and the usage method remains unchanged

1. You can easily build a multi-agent application by using multiple AI dialogue interfaces that you have customized, and then simply calling the corresponding MCP interface on the client side.

2. It can quickly access AI dialogue calls for your management system, with high customization. You don't need to pay attention to any details in the AI field, just focus on your favorite areas of web and front-end, and you can achieve cool effects. Compared to this

3. It can be used in conjunction with simple MCP clients like cursor to easily complete interface debugging

# 🔔summarize

This framework is actually very simple, and there may be many vulnerabilities and shortcomings in the code. Please forgive me.

# 📄 Copyright Statement/Open Source Agreement

According to the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html) Published code


