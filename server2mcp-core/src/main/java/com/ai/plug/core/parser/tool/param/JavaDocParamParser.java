package com.ai.plug.core.parser.tool.param;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * @author 韩
 * time: 2025/5/13 2:05
 * JAVADOC 还是一种很有用的注解方式, 不能够废弃, 后面会给他提供一种可用的方式改造
 */
@Deprecated
@Slf4j
public class JavaDocParamParser extends AbstractParamParser {

    @Override
    public Boolean doParamRequiredParse(Method method, Class<?> toolClass, int index) {
        return null;
    }

    @Override
    public String doParamDesParse(Method method, Class<?> toolClass, int index) {
        Parameter parameter = method.getParameters()[index];
        String parameterName = parameter.getName();

        CompilationUnit compilationUnit = loadCompilationUnit(method.getDeclaringClass());
        if (compilationUnit == null) {
            return null;
        }

        // 查找匹配的方法声明
        Optional<MethodDeclaration> matchedMethod = findMatchingMethod(compilationUnit, method);
        
        if (matchedMethod.isPresent() && matchedMethod.get().getJavadoc().isPresent()) {

            MethodDeclaration methodDeclaration = matchedMethod.get();
            Javadoc javadoc = methodDeclaration.getJavadoc().get();

            // 带 tag的信息 例如@param
            List<JavadocBlockTag> blockTags = javadoc.getBlockTags();
            for (JavadocBlockTag tag : blockTags) {
                if (tag.getName().orElse("").equals(parameterName) && JavadocBlockTag.Type.PARAM == tag.getType()) {
                    return tag.getContent().getElements().get(0).toText();
                }
            }
        }

        return null;
    }
    
    /**
     * 查找匹配的方法声明
     * @param compilationUnit 编译单元
     * @param method 反射方法对象
     * @return 匹配的方法声明，如果没有找到则返回空
     */
    private Optional<MethodDeclaration> findMatchingMethod(CompilationUnit compilationUnit, Method method) {
        List<MethodDeclaration> methodDeclarations = compilationUnit.findAll(MethodDeclaration.class);
        
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
            // 检查方法名是否匹配
            if (!methodDeclaration.getNameAsString().equals(method.getName())) {
                continue;
            }
            
            // 检查参数数量是否匹配
            if (methodDeclaration.getParameters().size() != method.getParameterCount()) {
                continue;
            }
            
            // 检查参数类型是否匹配
            boolean match = true;
            for (int i = 0; i < methodDeclaration.getParameters().size(); i++) {
                String paramType = methodDeclaration.getParameters().get(i).getType().toString();
                String actualParamType = method.getParameterTypes()[i].getSimpleName();
                if (!paramType.equals(actualParamType)) {
                    match = false;
                    break;
                }
            }
            
            if (match) {
                return Optional.of(methodDeclaration);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 加载编译单元
     * @param clazz 类对象
     * @return 编译单元，如果加载失败则返回null
     */
    private CompilationUnit loadCompilationUnit(Class<?> clazz) {
        String className = clazz.getName().replace('.', '/') + ".java";
        
        // 尝试从类路径资源加载
        try {
            InputStream inputStream = clazz.getClassLoader().getResourceAsStream(className);
            if (inputStream != null) {
                return StaticJavaParser.parse(inputStream);
            }
        } catch (Exception e) {
            log.debug("从类路径资源加载源代码失败: {}", e.getMessage());
        }
        
        // 尝试从源代码目录加载
        try {
            // 尝试多个可能的源代码位置
            File file = getJavaSourceFile(clazz);
            if (file != null && file.exists()) {
                return StaticJavaParser.parse(file);
            }
        } catch (Exception e) {
            log.error("没找到源文件: {}, 请检查", className);
        }
        
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

        // target
        File moduleRoot = classLocation.getParentFile();
        // 模块根目录
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
