package com.ai.plug.component.parser.des.builder;

import com.ai.plug.component.config.PluginProperties;
import com.ai.plug.component.parser.des.AbstractDesParser;
import com.ai.plug.component.parser.des.EmptyDesParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 韩
 * time: 2025/03/2025/3/26
 * des: 执行策略接口
 */
@Slf4j
public abstract class ExecutorStrategy {


    @Autowired
    @Qualifier("EmptyParserHandler")
    private AbstractDesParser emptyParserHandler;

    @Autowired
    @Qualifier("JavaDocParserHandler")
    private AbstractDesParser javaDocParserHandler;

    @Autowired
    @Qualifier("EmptyParserHandler")
    protected AbstractDesParser header;


    private PluginProperties pluginProperties;

    protected List<AbstractDesParser> concreteDesParsers;

    public ExecutorStrategy(PluginProperties pluginProperties, List<AbstractDesParser> concreteParserHandlers) {
        this.pluginProperties = pluginProperties;

        this.concreteDesParsers = concreteParserHandlers.stream()
                .filter(handler -> !(handler.getClass() == AbstractDesParser.class))
                .filter(handler -> !(handler.getClass() == EmptyDesParser.class))
                .collect(Collectors.toList());

    }

    // 用了一个模板方法
    public void startTheApp() {

        // 构造解析责任链
        if (!preHandle()) {
            log.debug("未打开配置，解析链构造失败");
            return;
        }

//        handle();
    }

    // 用了一个模板方法
    protected boolean preHandle() {
        // 检查
        boolean enabled = pluginProperties.isEnabled();
        if (!enabled) {
            return false;
        }
        // 成功了就执行预处理操作

        // 构造解析注解责任链
        chainDesParsers();

        log.debug("解析方法责任链 构造成功！！！");
        // 还可以组装若干责任链

        return true;
    }


    public void chainDesParsers() {

        // 这里将组装Des责任链, 并提供一个类似链表头的启动
        pluginProperties.getParser().forEach((parserName) -> {
            // 组成责任链, 由于component不能直起别名, 在应用层使用一个映射map

            // 通过getName方法指定
            concreteDesParsers.forEach(AbstractDesParser::getName);

            // 如果一个handlers也没有绑定上, 会默认使用一个javadoc
            if (header.getNextParserHandler() == null) {
                // 报一个警报, 使用默认配置
                log.warn("Incorrect parsing method configuration, Will use default parsing configuration (Javadoc parsing)");
                header.setNextParserHandler(javaDocParserHandler);
            }


        });

    }





    protected abstract void handle();
}
