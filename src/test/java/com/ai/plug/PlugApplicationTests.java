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

        TestObject testObject = new TestObject("111");
        try {
            testObject.doTest();
        } catch (NullPointerException e) {
            throw new RuntimeException(e);
        }
        System.out.println("6666");
    }
}
class TestObject {
    String name;

    public TestObject(String name) {
        this.name = name;
    }
    public void doTest() {
    }
}
