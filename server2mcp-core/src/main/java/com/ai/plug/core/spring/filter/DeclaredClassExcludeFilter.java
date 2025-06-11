package com.ai.plug.core.spring.filter;

import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;

import java.util.HashSet;
import java.util.Set;

public class DeclaredClassExcludeFilter extends AbstractTypeHierarchyTraversingFilter {
    private final Set<String> classNames = new HashSet<>();

    public DeclaredClassExcludeFilter(boolean considerInherited, boolean considerInterfaces, Class<?>... sources) {
        super(considerInherited, considerInterfaces);
        for (Class<?> source : sources) {
            this.classNames.add(source.getName());
        }
    }

    public DeclaredClassExcludeFilter(Class<?>... sources) {
        super(false, false);

        for (Class<?> source : sources) {
            this.classNames.add(source.getName());
        }

    }

    public boolean matchClassName(String className) {
        return this.classNames.contains(className);
    }
}