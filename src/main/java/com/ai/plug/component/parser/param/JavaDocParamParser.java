package com.ai.plug.component.parser.param;

import com.ai.plug.component.config.PluginProperties;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * @ author 韩
 * time: 2025/5/13 2:05
 */
@Deprecated
@Slf4j
public class JavaDocParamParser extends AbstractParamParser {
    @Override
    protected PluginProperties.ParserType getName() {
        return null;
    }

    @Override
    public Boolean doParamRequiredParse(Method method, Class<?> toolClass, int index) {
        return null;
    }

    @Override
    public String doParamDesParse(Method method, Class<?> toolClass, int index) {
        Parameter parameter = method.getParameters()[index];
        String parameterName = parameter.getName();

        String className = method.getDeclaringClass().getName().replace('.', '/') + ".java";
        File file = new File("src/main/java/"  + className);

        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(file);
            // 遍历所有方法声明
            for (MethodDeclaration methodDeclaration : compilationUnit.findAll(MethodDeclaration.class)) {
                if (methodDeclaration.getNameAsString().equals(method.getName())
                        && methodDeclaration.getParameters().size() == method.getParameterCount()
                        && methodDeclaration.hasJavaDocComment()
                ) {
                    // 检查参数类型是否匹配
                    boolean match = true;
                    // 对比每个参数
                    for (int i = 0; i < methodDeclaration.getParameters().size(); i++) {
                        String paramType = methodDeclaration.getParameters().get(i).getType().toString();
                        String actualParamType = method.getParameterTypes()[i].getSimpleName();
                        if (!paramType.equals(actualParamType)) {
                            match = false;
                            break;
                        }
                    }
                    // 如果不匹配， 就continue
                    if (!match) continue;

                    // 到这里了，说明找到了匹配方法，并有注释
                    Javadoc javadoc = methodDeclaration.getJavadoc().get();
                    // 带 tag的信息 例如@param
                    List<JavadocBlockTag> blockTags = javadoc.getBlockTags();
                    for (JavadocBlockTag item : blockTags) {
                        if (item.getName().equals(parameterName) && JavadocBlockTag.Type.PARAM == item.getType()) {
                            return item.getContent().getElements().get(0).toText();
                        }
                    }

                    return "暂不了解该参数意义, 我将返回其javadoc和方法体,供你了解 javadoc：" + methodDeclaration.getJavadoc() + "方法体：" + methodDeclaration.getBody().map(Object::toString).orElse("");
                }

                else {
                    if (methodDeclaration.getComment().isPresent()) {
                        return "暂不了解该参数意义, 我将返回其方法体和注释,供你了解 方法体：" +  methodDeclaration.getBody().map(Object::toString).orElse("") + "注释：" + methodDeclaration.getComment().get();
                    } else {
                        return "暂不了解该参数意义, 我将返回其方法体,供你了解" +  methodDeclaration.getBody().map(Object::toString).orElse("");
                    }
                }
            }


        } catch (FileNotFoundException e) {
            log.error("没找到该文件: " + "classPath" + ", 请检查");
        }

        return null;
    }
}
