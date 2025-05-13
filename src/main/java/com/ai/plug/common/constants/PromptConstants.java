package com.ai.plug.common.constants;

import org.springframework.ai.util.ParsingUtils;

import java.lang.reflect.Parameter;
import java.util.function.Function;

/**
 * @author: han
 * time: 2025/04/2025/4/1 23:08
 * des:
 */
public class PromptConstants {

    public final static Function<Parameter, String> DEFAULT_PARAM_DES = param -> {
        return ParsingUtils.reConcatenateCamelCase(param.getName(), " ");
    };
    public final static Function<Object, String> CUSTOM_PARAM_DES = null;

}
