package com.ai.plug.component.parser.des;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author: 韩
 * time: 2025/03/2025/3/27
 * des:
 */
@Component("JavaDocParserHandler")
public class JavaDocDesParser extends AbstractDesParser {



    public String getName(){
        return "javadoc";
    }



    @Override
    public String handleLogic(Method method, Class<?> handlerType) throws FileNotFoundException {



        String className = handlerType.getName().replace('.', '/') + ".java";
        File file = new File("src/main/java/"  + className);

        CompilationUnit cu = StaticJavaParser.parse(file);

        // 查找类声明
        for (MethodDeclaration itemMethod : cu.findAll(MethodDeclaration.class)) {
            // 由于每次只代理 一个接口, 就判断一下是不是该方法, 如果是继续, 不是就直接continue, 且不能为空字符串造成的相等
            if (!Objects.equals(method.getName().isBlank() ? null : method.getName(), itemMethod.getNameAsString())) {
                continue;
            }

            // 获取方法的 Javadoc 注释
            if (itemMethod.getComment().isPresent() && itemMethod.getComment().get() instanceof JavadocComment) {
                return itemMethod.getComment().get().getContent();
            }

        }
        // 没找到或者没有javadoc
        return "";
    }
}
