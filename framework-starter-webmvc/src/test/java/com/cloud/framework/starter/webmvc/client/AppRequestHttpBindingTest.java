package com.cloud.framework.starter.webmvc.client;

import com.cloud.framework.core.request.AppRequest;
import com.cloud.framework.core.request.ClientAppRequest;
import com.cloud.framework.core.http.RequestHeader;
import com.cloud.framework.starter.webmvc.error.WebMvcExceptionHandler;
import jakarta.validation.Valid;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppRequestHttpBindingTest {
    @Test
    void shouldBindStandardRequestBodyBeforeValidationAndIgnoreLegacyClientHeader() throws Exception {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.setMessageInterpolator(new ParameterMessageInterpolator());
            validator.afterPropertiesSet();
            var client = MockMvcBuilders.standaloneSetup(new Controller())
                    .setControllerAdvice(new AppRequestBodyAdvice(),
                            new WebMvcExceptionHandler())
                    .setValidator(validator).build();
            client.perform(post("/client").contentType(MediaType.APPLICATION_JSON)
                    .header(RequestHeader.APP_ID.headerName(), "app").header(RequestHeader.CLIENT_ID.headerName(), "client")
                    .content("{\"appId\":\"forged\",\"clientId\":\"forged\"}"))
                    .andExpect(status().isOk()).andExpect(content().string("app|client"));
            client.perform(post("/client").contentType(MediaType.APPLICATION_JSON)
                    .header(RequestHeader.APP_ID.headerName(), "app").header("X-Channel-Code", "legacy")
                    .content("{\"appId\":\"forged\",\"clientId\":\"forged\"}"))
                    .andExpect(status().isBadRequest());
            client.perform(post("/app").contentType(MediaType.APPLICATION_JSON).content("{\"appId\":\"forged\"}"))
                    .andExpect(status().isBadRequest());
            client.perform(post("/app").header(RequestHeader.APP_ID.headerName(), "app")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isOk()).andExpect(content().string("app"));
        }
    }

    @RestController
    static class Controller {
        @PostMapping("/client") String client(@Valid @RequestBody ClientAppRequest request) {
            return request.getAppId() + "|" + request.getClientId();
        }
        @PostMapping("/app") String app(@Valid @RequestBody AppRequest request) { return request.getAppId(); }
    }
}
