package com.ai.plug.core.parser.tool.des;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Objects;


/**
 * @author: 韩
 * time: 2025/03/2025/3/27
 * des: sorry, i have a major mistake of the class, because i forget the javaParser can only parse Java file, but the desParser can still be used in IDE enironments
 *  so, i will take some measures to preserve this desParser for users to choose from
 * zh-des: 抱歉，在这个类中我有一个重大失误，因为我忘了javaParser只能针对java源文件，但是在jar包启动时没有java文件，会直接导致报错
 *
 * JAVADOC 还是一种很有用的注解方式, 不能够废弃, 后面会给他提供一种可用的方式改造
 */
@Deprecated
@Slf4j
public class JavaDocDesParser extends AbstractDesParser {


    @Override
    public String doDesParse(Method method, Class<?> toolClass) {
        // 尝试多种方式获取源代码
        CompilationUnit cu = null;
        
        // 1. 尝试从类路径资源加载
        String resourcePath = toolClass.getName().replace('.', '/') + ".java";
        try {

            InputStream inputStream = toolClass.getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream != null) {
                cu = StaticJavaParser.parse(inputStream);
                log.debug("成功从类路径资源加载源代码: {}", resourcePath);
            }
        } catch (Exception e) {
            log.debug("从类路径资源加载源代码失败: {}", e.getMessage());
        }
        
        // 2. 如果类路径加载失败，尝试从源代码目录加载
        if (cu == null) {
            try {
                File file = getJavaSourceFile(toolClass);
                if (file != null && file.exists()) {
                    cu = StaticJavaParser.parse(file);
                    log.debug("成功从源代码目录加载: {}", file.getAbsolutePath());
                }
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
        }

        if (cu == null) {
            log.warn("无法加载类 {} 的源代码及资源路径，JavaDoc解析失败", toolClass.getName());
            return null;
        }


        // 查找类声明
        for (MethodDeclaration itemMethod : cu.findAll(MethodDeclaration.class)) {
            // 由于每次只代理一个接口，就判断一下是不是该方法，如果是继续，不是就直接continue，且不能为空字符串造成的相等
            if (!Objects.equals(method.getName().isBlank() ? null : method.getName(), itemMethod.getNameAsString())) {
                continue;
            }

            // 获取方法的 Javadoc 注释
            if (itemMethod.getComment().isPresent()) {
                return itemMethod.getComment().get().getContent();
            }
        }
        // 没找到或者没有注释
        return null;
    }

    private static File getJavaSourceFile(Class<?> clazz) throws Exception {

        // 获取当前类的class文件位置
        URI classUri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
        File classLocation = new File(classUri);
        String jarSuffix = ".jar";
        // 情况1：类从JAR包加载（生产环境）
        if (classLocation.getName().endsWith(jarSuffix)) {
            log.debug("无法从JAR包中获取源文件路径");
            return null;
        }

        // 情况2：类从文件系统加载（开发环境）
        // 确定模块目录：假设target/classes的父目录是模块根目录
        File moduleRoot = classLocation.getParentFile();
        moduleRoot = moduleRoot.getParentFile();

        // 计算源文件路径
        String packagePath = clazz.getPackage().getName().replace('.', '/');
        String sourcePath = moduleRoot.getAbsolutePath() +
                "/src/main/java/" +
                packagePath + "/" +
                clazz.getSimpleName() + ".java";

        return new File(sourcePath);

    }
}
