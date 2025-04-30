package com.ai.plug.component.parser.param;

import com.ai.plug.component.parser.AbstractParser;
import com.github.victools.jsonschema.generator.Module;
import org.springframework.ai.util.ParsingUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: han
 * time: 2025/04/2025/4/1 23:18
 * des:
 */
public abstract class AbstractParamAbstractParser extends AbstractParser  {

    public abstract String doRequiredParse(Method method, Class<?> toolClass) ;


}
