package com.acme.order.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
 * Path-segment cozumleme (Boot property spring.mvc.apiversion.use.path-segment=0): ilk path parcasi surumdur
 * (/v1.1/...); SemanticApiVersionParser bastaki "v"yi atar. Uretimde header secildi (application.yml'deki
 * gerekce: path-segment /internal/** gibi surumsuz path'leri de parse eder); bu test property'nin calistigini
 * ve ayni deprecation handler'in path ile secilen 1.0'a da uygulandigini kanitlar.
 */
@WebMvcTest(controllers = ApiVersionPathSegmentTest.PathProbeController.class,
        properties = "spring.mvc.apiversion.use.path-segment=0")
@Import({ClockConfig.class, ApiVersioningConfig.class, ApiVersionPathSegmentTest.PathProbeController.class})
class ApiVersionPathSegmentTest {

    @RestController
    static class PathProbeController {
        @GetMapping(path = "/{version}/probe", version = "1.0")
        String v10() { return "handler=1.0"; }

        @GetMapping(path = "/{version}/probe", version = "1.1")
        String v11() { return "handler=1.1"; }
    }

    @Autowired MockMvc mvc;

    @Test
    void pathSegmentSelectsVersion11() throws Exception {
        mvc.perform(get("/v1.1/probe")).andExpect(status().isOk()).andExpect(content().string("handler=1.1"))
                .andExpect(header().doesNotExist("Deprecation"));
    }

    @Test
    void pathSegmentSelectsDeprecatedVersion10_withDeprecationHeaders() throws Exception {
        mvc.perform(get("/v1.0/probe")).andExpect(status().isOk()).andExpect(content().string("handler=1.0"))
                .andExpect(header().exists("Deprecation")).andExpect(header().exists("Sunset"));
    }

    @Test
    void unsupportedPathVersion_returns400() throws Exception {
        mvc.perform(get("/v9.9/probe")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.API_VERSION_INVALID.getCode()));
    }
}
