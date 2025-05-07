package com.ai.plug.component.parser.des;

import com.ai.plug.component.config.PluginProperties;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.Objects;

import static com.ai.plug.common.constants.ConfigConstants.PARSER_PREFIX;
import static com.ai.plug.common.constants.ConfigConstants.VARIABLE_PREFIX;
import static com.ai.plug.component.config.PluginProperties.ParserType.JAVADOC;

/**
 * @author: 韩
 * time: 2025/03/2025/3/27
 * des: sorry, i have a major mistake of the class, because i forget the javaParser can only parse Java file, but the desParser can still be used in IDE enironments
 *  so, i will take some measures to preserve this desParser for users to choose from
 * zh-des: 抱歉，在这个类中我有一个重大失误，因为我忘了javaParser只能针对java源文件，但是在jar包启动时没有java文件，会直接导致报错
 */
@Component("JavaDocParserHandler")
@Deprecated
@ConditionalOnProperty(prefix = VARIABLE_PREFIX, name = PARSER_PREFIX, havingValue = "JAVADOC")
public class JavaDocDesParser extends AbstractDesParser {


    @Override
    public PluginProperties.ParserType getName(){
        return JAVADOC;
    }



    @Override
    public String doDesParse(Method method, Class<?> toolClass) {



        String className = toolClass.getName().replace('.', '/') + ".java";
        File file = new File("src/main/java/" + className);
        CompilationUnit cu = null;
        try {
            cu = StaticJavaParser.parse(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // 查找类声明
        for (MethodDeclaration itemMethod : cu.findAll(MethodDeclaration.class)) {
            // 由于每次只代理 一个接口, 就判断一下是不是该方法, 如果是继续, 不是就直接continue, 且不能为空字符串造成的相等
            if (!Objects.equals(method.getName().isBlank() ? null : method.getName(), itemMethod.getNameAsString())) {
                continue;
            }

            // 获取方法的 Javadoc 注释
            if (itemMethod.getComment().isPresent()) {
                return itemMethod.getComment().get().getContent();
            }

        }
        // 没找到或者没有注释
        return "";
    }
}
