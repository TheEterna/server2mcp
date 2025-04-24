package com.ai.plug;

import com.ai.plug.component.config.PluginProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import static com.ai.plug.component.config.PluginProperties.ScopeType.INTERFACE;

/**
 * @ author 韩
 * time: 2025/4/18 9:33
 */

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    public static void doTest() {


    }
}
