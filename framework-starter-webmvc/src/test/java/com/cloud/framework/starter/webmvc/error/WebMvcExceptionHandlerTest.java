package com.cloud.framework.starter.webmvc.error;

import com.cloud.framework.core.error.FrameworkError;
import com.cloud.framework.core.error.FrameworkException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebMvcExceptionHandlerTest {
    @Test
    void shouldPreserveBusinessErrorsAndReturnServerErrorsForUnexpectedFailures() throws Exception {
        try (var validator = validator()) {
            MockMvc client = client(validator);
            var business = client.perform(get("/business")).andExpect(status().isOk()).andReturn();
            assertFailure(business.getResponse().getContentAsString(), FrameworkError.OBJECT_REQUIRED);
            var unexpected = client.perform(get("/unexpected")).andExpect(status().isInternalServerError()).andReturn();
            assertFailure(unexpected.getResponse().getContentAsString(), FrameworkError.FRAMEWORK_ERROR);
        }
    }

    @Test
    void shouldTranslateMalformedBodiesAndParameterConversionFailures() throws Exception {
        try (var validator = validator()) {
            MockMvc client = client(validator);
            var malformed = client.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                    .andExpect(status().isBadRequest()).andReturn();
            assertFailure(malformed.getResponse().getContentAsString(), FrameworkError.OBJECT_INVALID);
            var missing = client.perform(post("/body").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest()).andReturn();
            assertFailure(missing.getResponse().getContentAsString(), FrameworkError.OBJECT_INVALID);
            var conversion = client.perform(get("/parameter").param("count", "not-a-number"))
                    .andExpect(status().isBadRequest()).andReturn();
            assertFailure(conversion.getResponse().getContentAsString(), FrameworkError.OBJECT_INVALID);
        }
    }

    @Test
    void shouldPreserveProtocolStatusesAndHeaders() throws Exception {
        try (var validator = validator()) {
            MockMvc client = client(validator);
            var method = client.perform(get("/body")).andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string(HttpHeaders.ALLOW, "POST")).andReturn();
            assertFailure(method.getResponse().getContentAsString(), FrameworkError.OBJECT_INVALID);
            var media = client.perform(post("/body").contentType(MediaType.TEXT_PLAIN).content("value"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(header().string(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)).andReturn();
            assertFailure(media.getResponse().getContentAsString(), FrameworkError.OBJECT_INVALID);
            var explicit = client.perform(get("/explicit-status")).andExpect(status().isTooManyRequests())
                    .andExpect(header().string(HttpHeaders.RETRY_AFTER, "7")).andReturn();
            assertFailure(explicit.getResponse().getContentAsString(), FrameworkError.OBJECT_INVALID);
            var unavailable = client.perform(get("/unavailable")).andExpect(status().isServiceUnavailable())
                    .andExpect(header().string(HttpHeaders.RETRY_AFTER, "2")).andReturn();
            assertFailure(unavailable.getResponse().getContentAsString(), FrameworkError.FRAMEWORK_ERROR);
        }
    }

    @Test
    void shouldDistinguishInvalidInputsFromInvalidReturnValues() throws Exception {
        try (var validator = validator()) {
            MockMvc client = client(validator);
            var body = client.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest()).andReturn();
            assertFailure(body.getResponse().getContentAsString(), FrameworkError.OBJECT_INVALID);
            var parameter = client.perform(get("/parameter").param("count", "0"))
                    .andExpect(status().isBadRequest()).andReturn();
            assertFailure(parameter.getResponse().getContentAsString(), FrameworkError.OBJECT_INVALID);
            var returned = client.perform(get("/invalid-return")).andExpect(status().isInternalServerError()).andReturn();
            assertFailure(returned.getResponse().getContentAsString(), FrameworkError.FRAMEWORK_ERROR);
        }
    }

    private static MockMvc client(LocalValidatorFactoryBean validator) {
        return MockMvcBuilders.standaloneSetup(new Controller()).setControllerAdvice(new WebMvcExceptionHandler())
                .setValidator(validator).build();
    }

    private static LocalValidatorFactoryBean validator() {
        var validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        return validator;
    }

    private static void assertFailure(String body, FrameworkError error) throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        assertEquals(error.errorCode(), json.get("code").asText());
        assertEquals(error.errorMessage(), json.get("message").asText());
        assertTrue(json.get("data").isNull());
    }

    public record Payload(@NotBlank String name) { }

    @RestController
    static class Controller {
        @PostMapping(value = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String body(@Valid @RequestBody Payload payload) { return payload.name(); }
        @GetMapping("/parameter")
        public String parameter(@RequestParam("count") @Min(1) int count) { return String.valueOf(count); }
        @GetMapping("/invalid-return") @NotNull
        public String invalidReturn() { return null; }
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
}
