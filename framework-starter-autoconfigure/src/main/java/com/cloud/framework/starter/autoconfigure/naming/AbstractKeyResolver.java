package com.cloud.framework.starter.autoconfigure.naming;

import com.cloud.framework.core.naming.ResourceNameResolver;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractKeyResolver {

    private final ResourceNameResolver resourceNameResolver;

    protected abstract String[] prefixes();

    public String resolve(String... s) {
        String[] names = new String[prefixes().length + s.length];
        System.arraycopy(prefixes(), 0, names, 0, prefixes().length);
        System.arraycopy(s, 0, names, prefixes().length, s.length);
        return resourceNameResolver.resolve(names);
    }
}
