package com.cloud.framework.starter.autoconfigure.naming;

import com.cloud.framework.core.naming.Namespaced;
import com.cloud.framework.core.naming.NamespaceResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class SpringApplicationNamespaceResolver implements NamespaceResolver {

    private static final String APPLICATION_NAME_PROPERTY = "spring.application.name";
    private static final String DEFAULT_NAMESPACE = "application";

    private final Environment environment;

    @Override
    public String resolve(Namespaced namespaced) {
        if (StringUtils.hasText(namespaced.getNamespace())) {
            return namespaced.getNamespace();
        }
        String applicationName = environment.getProperty(APPLICATION_NAME_PROPERTY);
        return StringUtils.hasText(applicationName) ? applicationName : DEFAULT_NAMESPACE;
    }
}
