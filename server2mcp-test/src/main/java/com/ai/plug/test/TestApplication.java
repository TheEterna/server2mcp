package com.ai.plug.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Demo entry point for api2mcp4j protocol-2026-07-28 verification.
 *
 * <p>By default we skip the existing MyBatis-Plus scanned components
 * ({@code YiziController} / {@code YiziMapper}) so the application can
 * boot against the bundled H2 in-memory datasource without any external
 * MySQL dependency. Set the system property
 * {@code -Ddemo.full=true} to include those components (requires a real
 * MySQL datasource override).
 *
 * @author han
 * @time 2026/8/3
 */

@SpringBootConfiguration
@EnableAutoConfiguration(excludeName = {
    "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
    "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
@ComponentScan(
    basePackages = "com.ai.plug.test",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = {
            "com\\.ai\\.plug\\.test\\.test\\.mapper\\..*",
            "com\\.ai\\.plug\\.test\\.test\\.controller\\.YiziController.*",
            "com\\.ai\\.plug\\.test\\.test\\.controller\\.AutoNotScanController.*"
        }
    )
)
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
