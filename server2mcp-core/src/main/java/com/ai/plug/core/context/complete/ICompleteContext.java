package com.ai.plug.core.context.complete;

import java.util.Set;

/**
 * @author han
 * @time 2025/7/14 11:34
 */

public interface ICompleteContext {

    Set<String> getRawCompletes();

    void addComplete(String name);

}
