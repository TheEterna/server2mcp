package com.ai.plug;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;

@Slf4j

class PlugApplicationTests {


    @Test
    public void test() {

//        context.doParse();
        Configuration annotation = AnnotationUtils.findAnnotation(SpringBootApplication.class, Configuration.class);
        System.out.println(annotation);


//        System.out.println(new PlugApplication().getClass().isAnnotationPresent(EnableAutoConfiguration.class));
    }
}
