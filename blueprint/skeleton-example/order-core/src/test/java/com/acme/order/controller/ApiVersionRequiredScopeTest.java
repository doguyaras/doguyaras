package com.acme.order.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.order.config.ApiVersioningConfig;
import com.acme.order.config.ClockConfig;
import com.acme.order.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * spring.mvc.apiversion.required=true'nun KAPSAMI (referans Bolum 20): zorunluluk yalniz version= tasiyan
 * handler'lara degil, strateji tanimli DispatcherServlet'teki TUM route'lara uygulanir. Surumsuz bir handler
 * (orn. /internal/**, ayni porttaki actuator) header'siz cagrilinca 400 API_VERSION_INVALID doner.
 * Bu, ApiVersioningConfig ve application.yml'deki uyarinin kanitidir.
 */
@WebMvcTest(controllers = ApiVersionRequiredScopeTest.UnversionedProbeController.class)
@Import({ClockConfig.class, ApiVersioningConfig.class, ApiVersionRequiredScopeTest.UnversionedProbeController.class})
class ApiVersionRequiredScopeTest {

    @RestController
    static class UnversionedProbeController {
        @GetMapping("/internal/probe")
        String probe() { return "unversioned"; }
    }

    @Autowired MockMvc mvc;

    @Test
    void unversionedHandler_withoutHeader_isRejectedBecauseRequiredIsGlobal() throws Exception {
        mvc.perform(get("/internal/probe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.API_VERSION_INVALID.getCode()));
    }

    @Test
    void unversionedHandler_withSupportedHeader_isServed() throws Exception {
        mvc.perform(get("/internal/probe").header("API-Version", "1.1"))
                .andExpect(status().isOk()).andExpect(content().string("unversioned"));
    }
}
