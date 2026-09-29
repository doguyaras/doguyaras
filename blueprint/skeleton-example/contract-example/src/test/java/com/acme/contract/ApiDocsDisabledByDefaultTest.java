package com.acme.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Referans 15.1: prod'da api-docs kapali. Varsayilan konfigurasyonla (ozellik acilmadan) /v3/api-docs yoktur;
 * spec yalniz CI/test'te ozellik acikca verilerek uretilir.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiDocsDisabledByDefaultTest {

    @Autowired MockMvc mvc;

    @Test
    void apiDocsEndpointIsNotExposedWithDefaultConfiguration() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }
}
