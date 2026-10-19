package com.cloud.framework.starter.webflux.error;

import com.cloud.framework.core.error.FrameworkError;
import com.cloud.framework.core.error.FrameworkException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class WebFluxExceptionHttpTest {
    @Test
    void shouldPreserveBusinessErrorsAndReturnServerErrorsForUnexpectedFailures() throws Exception {
        try (var context = context()) {
            WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
            byte[] business = client.get().uri("/business").exchange().expectStatus().isOk()
                    .expectBody().returnResult().getResponseBody();
            assertFailure(business, FrameworkError.OBJECT_REQUIRED);
            byte[] unexpected = client.get().uri("/unexpected").exchange().expectStatus().is5xxServerError()
                    .expectBody().returnResult().getResponseBody();
            assertFailure(unexpected, FrameworkError.FRAMEWORK_ERROR);
        }
    }

    @Test
    void shouldTranslateMalformedBodiesAndParameterConversionFailures() throws Exception {
        try (var context = context()) {
            WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
            byte[] malformed = client.post().uri("/body").contentType(MediaType.APPLICATION_JSON).bodyValue("{")
                    .exchange().expectStatus().isBadRequest().expectBody().returnResult().getResponseBody();
            assertFailure(malformed, FrameworkError.OBJECT_INVALID);
            byte[] missing = client.post().uri("/body").contentType(MediaType.APPLICATION_JSON)
                    .exchange().expectStatus().isBadRequest().expectBody().returnResult().getResponseBody();
            assertFailure(missing, FrameworkError.OBJECT_INVALID);
            byte[] conversion = client.get().uri("/parameter?count=not-a-number").exchange().expectStatus().isBadRequest()
                    .expectBody().returnResult().getResponseBody();
            assertFailure(conversion, FrameworkError.OBJECT_INVALID);
        }
    }

    @Test
    void shouldPreserveProtocolStatusesAndHeaders() throws Exception {
        try (var context = context()) {
            WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
            byte[] method = client.get().uri("/body").exchange().expectStatus().isEqualTo(405)
                    .expectHeader().valueEquals(HttpHeaders.ALLOW, "POST").expectBody().returnResult().getResponseBody();
            assertFailure(method, FrameworkError.OBJECT_INVALID);
            byte[] media = client.post().uri("/body").contentType(MediaType.TEXT_PLAIN).bodyValue("value")
                    .exchange().expectStatus().isEqualTo(415)
                    .expectHeader().valueEquals(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .expectBody().returnResult().getResponseBody();
            assertFailure(media, FrameworkError.OBJECT_INVALID);
            byte[] explicit = client.get().uri("/explicit-status").exchange().expectStatus().isEqualTo(429)
                    .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "7").expectBody().returnResult().getResponseBody();
            assertFailure(explicit, FrameworkError.OBJECT_INVALID);
            byte[] unavailable = client.get().uri("/unavailable").exchange().expectStatus().isEqualTo(503)
                    .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "2").expectBody().returnResult().getResponseBody();
            assertFailure(unavailable, FrameworkError.FRAMEWORK_ERROR);
        }
    }

    @Test
    void shouldDistinguishInvalidInputsFromInvalidReturnValues() throws Exception {
        try (var context = context()) {
            WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
            byte[] body = client.post().uri("/body").contentType(MediaType.APPLICATION_JSON).bodyValue("{\"name\":\"\"}")
                    .exchange().expectStatus().isBadRequest().expectBody().returnResult().getResponseBody();
            assertFailure(body, FrameworkError.OBJECT_INVALID);
            byte[] parameter = client.get().uri("/parameter?count=0").exchange().expectStatus().isBadRequest()
                    .expectBody().returnResult().getResponseBody();
            assertFailure(parameter, FrameworkError.OBJECT_INVALID);
            assertThrows(jakarta.validation.ConstraintViolationException.class,
                    () -> context.getBean(ReturnService.class).value());
            byte[] returned = client.get().uri("/invalid-return").exchange().expectStatus().is5xxServerError()
                    .expectBody().returnResult().getResponseBody();
            assertFailure(returned, FrameworkError.FRAMEWORK_ERROR);
        }
    }

    private static AnnotationConfigReactiveWebApplicationContext context() {
        var context = new AnnotationConfigReactiveWebApplicationContext();
        context.register(HttpConfiguration.class, WebFluxErrorAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private static void assertFailure(byte[] body, FrameworkError error) throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        assertEquals(error.errorCode(), json.get("code").asText());
        assertEquals(error.errorMessage(), json.get("message").asText());
        assertTrue(json.get("data").isNull());
    }

    @Configuration
    @EnableWebFlux
    @Import(Controller.class)
    static class HttpConfiguration implements WebFluxConfigurer {
        @Bean
        LocalValidatorFactoryBean requestValidator() {
            var validator = new LocalValidatorFactoryBean();
            validator.setMessageInterpolator(new ParameterMessageInterpolator());
            return validator;
        }
        @Override
        public org.springframework.validation.Validator getValidator() { return requestValidator(); }

        @Bean
        static org.springframework.validation.beanvalidation.MethodValidationPostProcessor methodValidationPostProcessor(
                @org.springframework.beans.factory.annotation.Qualifier("requestValidator")
                org.springframework.beans.factory.ObjectProvider<jakarta.validation.Validator> validators) {
            var processor = new org.springframework.validation.beanvalidation.MethodValidationPostProcessor();
            processor.setValidatorProvider(validators);
            return processor;
        }

        @Bean
        ReturnService returnService() { return new ReturnService(); }
    }

    public record Payload(@NotBlank String name) { }

    @RestController
    @lombok.RequiredArgsConstructor
    static class Controller {
        private final ReturnService returns;
        @PostMapping(value = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String body(@Valid @RequestBody Payload payload) { return payload.name(); }
        @GetMapping("/parameter")
        public String parameter(@RequestParam("count") @Min(1) int count) { return String.valueOf(count); }
        @GetMapping("/invalid-return")
        public String invalidReturn() { return returns.value(); }
        @GetMapping("/business")
        public String business() { throw new FrameworkException(FrameworkError.OBJECT_REQUIRED); }
        @GetMapping("/unexpected")
        public String unexpected() { throw new IllegalStateException("internal detail"); }
        @GetMapping("/explicit-status")
        public String explicitStatus() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.RETRY_AFTER, "7");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "internal detail") {
                @Override
                public HttpHeaders getHeaders() { return headers; }
            };
        }
        @GetMapping("/unavailable")
        public String unavailable() {
            var exception = new org.springframework.web.ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE);
            exception.getHeaders().set(HttpHeaders.RETRY_AFTER, "2");
            throw exception;
        }
    }

    @org.springframework.validation.annotation.Validated
    public static class ReturnService {
        @NotNull
        public String value() { return null; }
    }
}
