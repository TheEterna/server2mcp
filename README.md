# Server2MCP Spring Boot Starter

This is a Spring Boot Starter used for integrating MCP (Model Control Protocol) services.

## ✨  function characteristics

* Automatic configuration of MCP service, similar to the relationship between mybatis plus and mybatis, non-invasive, purely enhanced
* Supports all native functions of MCP in JavaSdk, providing tool registration and callback mechanisms, etc
* Support custom tool parsing
* Users can customize the Parser without relying on chain of responsibility implementation to complete attribute parsing for unique interface annotations

## Unfinished points

There are still many parsing extension points of Springai that have not been integrated. For example, currently only Javadoc version annotations have been completed, but the parsing architecture uses responsibility chains and templates, which are extremely easy to extend. In the future, various mainstream methods will be integrated to describe annotation parsing

##  🎯  Quick start

Since it has not been pushed to the central repository yet, you can download the source code, perform an mvn clean install, and then make dependency references

### Add Dependency

<dependency>
<groupId>com.ai.plug</groupId>
<artifactId>server2mcp-spring-boot-starter</artifactId>
<version>0.0.1-SNAPSHOT</version>
</dependency>

Then add the configuration in the configuration file:

plugin.mcp.enabled=true
    
# If it is yml, then it is
plugin:
mcp:
enabled: true

The above is the basic configuration for starting the project, which includes all native configurations such as spring.ai.mcp.server-side, etc. By default, it will register all controllers under your startup class path as MCP interfaces. If there is a @ Depreciated annotation on the interface method or class, it will not be registered.

### Principle

It can be understood as opening up interfaces to AI, so these interfaces are the same as regular interfaces, except that they can be called through AI. Relevant knowledge documents: [Model Context Protocol (MCP) :: Spring AI Reference ]( https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html ）And [Introduction - Model Context Protocol]（ https://modelcontextprotocol.io/introduction )

### Best practices

With this framework, you no longer need to rebuild an MCP service application from scratch, nor do you need to add @ Tool annotations to highly coupled copied code, or add MCP functionality to source code. You only need to add custom @ ToolScan annotations based on a configuration class to easily complete the registration of MCP interfaces. What should you do if you encounter MCP SDK revisions? Don't worry, the core content is maintained by me, and the usage method remains unchanged

1. You can easily build a multi-agent application by using multiple AI dialogue interfaces that you have customized, and then simply calling the corresponding MCP interface on the client side.

2. It can quickly access AI dialogue calls for your management system, with high customization. You don't need to pay attention to any details in the AI field, just focus on your favorite areas of web and front-end, and you can achieve cool effects. Compared to this, applications like DB-GPT may seem bulky and difficult to expand

3. It can be used in conjunction with simple MCP clients like cursor to easily complete interface debugging

## Summary

This framework is actually very simple, and there may be many vulnerabilities and shortcomings in the code. Please forgive me.
