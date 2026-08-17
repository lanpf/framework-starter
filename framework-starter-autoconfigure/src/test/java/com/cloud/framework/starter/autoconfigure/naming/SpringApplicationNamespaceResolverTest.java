package com.cloud.framework.starter.autoconfigure.naming;

import com.cloud.framework.core.naming.NamespaceResolver;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringApplicationNamespaceResolverTest {

    @Test
    void shouldPreferExplicitNamespace() {
        SpringApplicationNamespaceResolver resolver = new SpringApplicationNamespaceResolver(
                environmentWithApplicationName("order-service")
        );

        assertEquals("shared-order", resolver.resolve(() -> "shared-order"));
    }

    @Test
    void shouldUseApplicationNameWhenNamespaceIsBlank() {
        SpringApplicationNamespaceResolver resolver = new SpringApplicationNamespaceResolver(
                environmentWithApplicationName("order-service")
        );

        assertEquals("order-service", resolver.resolve(() -> " "));
    }

    @Test
    void shouldUseStableFallbackWhenApplicationNameIsMissing() {
        SpringApplicationNamespaceResolver resolver = new SpringApplicationNamespaceResolver(
                new StandardEnvironment()
        );

        assertEquals("application", resolver.resolve(() -> null));
    }

    @Test
    void shouldUseStableFallbackWhenApplicationNameIsBlank() {
        SpringApplicationNamespaceResolver resolver = new SpringApplicationNamespaceResolver(
                environmentWithApplicationName(" ")
        );

        assertEquals("application", resolver.resolve(() -> null));
    }

    @Test
    void shouldAutoConfigureApplicationNamespaceResolver() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "test",
                    Map.of("spring.application.name", "order-service")
            ));
            context.register(NamespaceAutoConfiguration.class);
            context.refresh();

            assertEquals(
                    "order-service",
                    context.getBean(NamespaceResolver.class).resolve(() -> null)
            );
        }
    }

    @Test
    void shouldBackOffWhenNamespaceResolverIsProvided() {
        NamespaceResolver customResolver = namespaced -> "shared-service";
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(NamespaceResolver.class, () -> customResolver);
            context.register(NamespaceAutoConfiguration.class);
            context.refresh();

            assertEquals(customResolver, context.getBean(NamespaceResolver.class));
        }
    }

    private ConfigurableEnvironment environmentWithApplicationName(String applicationName) {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of("spring.application.name", applicationName)
        ));
        return environment;
    }
}
