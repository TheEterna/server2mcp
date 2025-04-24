package com.ai.plug.component.parser.aggregater;

import com.ai.plug.component.config.PluginProperties;
import com.ai.plug.component.parser.des.AbstractDesParser;
import com.ai.plug.component.parser.des.EmptyDesParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author: 韩
 * time: 2025/03/2025/3/26
 * des: 执行策略接口
 */
@Slf4j
public class DesParserAggregater extends AbstractParserAggregater {


    @Autowired
    @Qualifier("EmptyDesParser")
    protected AbstractDesParser header;



    protected Map<Enum<PluginProperties.ParserType>, AbstractDesParser> concreteDesParsers;

    public DesParserAggregater(List<AbstractDesParser> concreteParserHandlers) {
        super();
        this.concreteDesParsers = concreteParserHandlers.stream()
                .filter(handler -> !(handler.getClass() == AbstractDesParser.class))
                .filter(handler -> !(handler.getClass() == EmptyDesParser.class))
                .collect(Collectors.toMap(AbstractDesParser::getName, item -> item));

    }


    // 用了一个模板方法


    public void chainDesParsers() {

        // 如果一个handlers也没有绑定上, 会默认使用一个
        if (header.getNextParserHandler() == null) {
            // 报一个警报, 使用默认配置
            log.warn("Incorrect parsing method configuration, Will use default parsing configuration (Javadoc parsing)");
//            header.setNextParserHandler(javaDocParserHandler);
            return;
        }

        // 这里将组装Des责任链, 并提供一个类似链表头的启动
        pluginProperties.getParser().forEach((parserEnum) -> {

            if (Objects.nonNull(parserEnum)) {
                header = header.setNextParserHandler(concreteDesParsers.get(parserEnum));
            }


        });
        log.debug("解析工具描述 DesParser 责任链 构造成功！！！");

    }





}
