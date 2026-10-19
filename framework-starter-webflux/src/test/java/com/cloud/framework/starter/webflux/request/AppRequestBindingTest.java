package com.cloud.framework.starter.webflux.request;

import com.cloud.framework.core.request.AppRequest;
import com.cloud.framework.core.request.AuthenticatedSessionClientAppRequest;
import com.cloud.framework.core.http.RequestHeader;
import com.cloud.framework.starter.webflux.error.WebFluxErrorAutoConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class AppRequestBindingTest {
    @Test
    void shouldBindStandardRequestBodyBeforeValidation() {
        try (var context = context()) {
            WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
            headers(client.post().uri("/plain")).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"appId\":\"forged\",\"clientId\":\"forged\",\"userId\":\"forged\",\"name\":\"value\"}")
                    .exchange().expectStatus().isOk().expectBody(String.class)
                    .isEqualTo("app|WEB|1.0|client|user|session|value");
            client.post().uri("/plain").contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"appId\":\"forged\",\"clientId\":\"forged\",\"userId\":\"forged\",\"sessionId\":\"forged\",\"name\":\"value\"}")
                    .exchange().expectStatus().isBadRequest();
            headers(client.post().uri("/plain")).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"\"}").exchange().expectStatus().isBadRequest();
        }
    }

    @Test
    void shouldBindReactiveRequestBodiesWithoutChangingTheirShape() {
        try (var context = context()) {
            WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
            headers(client.post().uri("/mono")).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"mono\"}").exchange().expectStatus().isOk()
                    .expectBody(String.class).isEqualTo("app|WEB|1.0|client|user|session|mono");
            headers(client.post().uri("/flux")).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("[{\"name\":\"one\"},{\"name\":\"two\"}]").exchange().expectStatus().isOk()
                    .expectBody(String.class).isEqualTo("app|WEB|1.0|client|user|session|one,app|WEB|1.0|client|user|session|two");
            headers(client.post().uri("/mono")).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"\"}").exchange().expectStatus().isBadRequest();
        }
    }

    @Test
    void shouldPreserveOptionalBodiesAndOrdinaryPayloads() {
        try (var context = context()) {
            WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
            client.post().uri("/optional").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("absent");
            client.post().uri("/ordinary").contentType(MediaType.APPLICATION_JSON).bodyValue("{\"value\":\"unchanged\"}")
                    .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("unchanged");
            headers(client.post().uri("/plain")).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{").exchange().expectStatus().isBadRequest();
            client.post().uri("/plain").exchange().expectStatus().isBadRequest();
        }
    }

    @Test
    void shouldBindHeaderOnlyContextAndClearMissingOptionalHeaders() {
        try (var context = context()) {
            WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
            client.get().uri("/context").header(RequestHeader.APP_ID.headerName(), "app")
                    .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("app");
            client.get().uri("/context").exchange().expectStatus().isBadRequest();
            client.post().uri("/app").header(RequestHeader.APP_ID.headerName(), "app")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"appPlatform\":\"forged\",\"appVersion\":\"forged\"}")
                    .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("app|null|null");
        }
    }

    private static AnnotationConfigReactiveWebApplicationContext context() {
        var context = new AnnotationConfigReactiveWebApplicationContext();
        context.register(HttpConfiguration.class, AppRequestWebFluxAutoConfiguration.class, WebFluxErrorAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private static WebTestClient.RequestBodySpec headers(WebTestClient.RequestBodySpec request) {
        return request.header(RequestHeader.APP_ID.headerName(), "app")
                .header(RequestHeader.APP_PLATFORM.headerName(), "WEB").header(RequestHeader.APP_VERSION.headerName(), "1.0")
                .header(RequestHeader.CLIENT_ID.headerName(), "client").header(RequestHeader.USER_ID.headerName(), "user")
                .header(RequestHeader.SESSION_ID.headerName(), "session");
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
    }

    @Getter @Setter
    public static class Payload extends AuthenticatedSessionClientAppRequest {
        @NotBlank private String name;
        public String text() {
            return getAppId() + "|" + getAppPlatform() + "|" + getAppVersion() + "|" + getClientId()
                    + "|" + getUserId() + "|" + getSessionId() + "|" + name;
        }
    }

    public record Ordinary(String value) { }

    @RestController
    static class Controller {
        @PostMapping("/plain") String plain(@Valid @RequestBody Payload request) { return request.text(); }
        @PostMapping("/mono") Mono<String> mono(@Valid @RequestBody Mono<Payload> request) { return request.map(Payload::text); }
        @PostMapping("/flux") Mono<String> flux(@Valid @RequestBody Flux<Payload> request) {
            return request.map(Payload::text).collectList().map(values -> String.join(",", values));
        }
        @PostMapping("/optional") String optional(@RequestBody(required = false) AppRequest request) {
            return request == null ? "absent" : request.getAppId();
        }
        @PostMapping("/ordinary") String ordinary(@RequestBody Ordinary request) { return request.value(); }
        @GetMapping("/context") String context(@Valid AppRequest request) { return request.getAppId(); }
        @PostMapping("/app") String app(@Valid @RequestBody AppRequest request) {
            return request.getAppId() + "|" + request.getAppPlatform() + "|" + request.getAppVersion();
        }
    }
}
