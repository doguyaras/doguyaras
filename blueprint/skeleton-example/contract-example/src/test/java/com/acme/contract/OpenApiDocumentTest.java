package com.acme.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Senaryo 1 (seviye 1, MockMvc): springdoc'un urettigi /v3/api-docs gecerli bir OpenAPI 3.1 belgesidir, iki
 * operasyonu ve DTO semalarini (required listeleri dahil) icerir; CI artefakti olarak target/openapi.json yazilir.
 * Ayrica spec'in runtime ile tutarli oldugu (alan adlari, 201) kanitlanir: yalan soyleyen spec'ten uretilen istemci
 * derlenir ama calismaz.
 */
@SpringBootTest(properties = OpenApiContract.API_DOCS_ENABLED)
@AutoConfigureMockMvc
@Import(OpenApiDocumentTest.FixedClock.class)
class OpenApiDocumentTest {

    @TestConfiguration
    static class FixedClock {
        @Bean @Primary
        Clock fixedClock() { return Clock.fixed(Instant.parse("2026-09-29T10:00:00Z"), ZoneOffset.UTC); }
    }

    @Autowired MockMvc mvc;

    @Test
    void apiDocsIsAValidOpenApi31DocumentWithBothOperationsAndDtoSchemas() throws Exception {
        String spec = OpenApiContract.generate(mvc);
        OpenApiContract.save(OpenApiContract.GENERATED, spec);

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult parsed = new OpenAPIV3Parser().readContents(spec, null, options);
        assertThat(parsed.getMessages()).as("parser mesajlari (gecersiz spec)").isEmpty();
        OpenAPI api = parsed.getOpenAPI();
        assertThat(api.getOpenapi()).isEqualTo("3.1.0");
        assertThat(api.getSpecVersion()).isEqualTo(SpecVersion.V31);
        assertThat(api.getInfo().getTitle()).isEqualTo("contract-example");
        assertThat(api.getInfo().getVersion()).isEqualTo("v1");

        Operation getOrder = api.getPaths().get("/v1/orders/{id}").getGet();
        assertThat(getOrder.getOperationId()).isEqualTo("getOrder");
        assertThat(getOrder.getParameters()).singleElement().satisfies(p -> {
            assertThat(p.getName()).isEqualTo("id");
            assertThat(p.getIn()).isEqualTo("path");
            assertThat(p.getRequired()).isTrue();
            assertThat(p.getSchema().getFormat()).isEqualTo("uuid");
        });
        assertThat(getOrder.getResponses().get("200").getContent().get("application/json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/OrderResponse");

        Operation createOrder = api.getPaths().get("/v1/orders").getPost();
        assertThat(createOrder.getOperationId()).isEqualTo("createOrder");
        assertThat(createOrder.getRequestBody().getRequired()).isTrue();
        assertThat(createOrder.getRequestBody().getContent().get("application/json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/CreateOrderRequest");
        assertThat(createOrder.getResponses()).containsOnlyKeys("201");
        assertThat(createOrder.getResponses().get("201").getContent().get("application/json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/OrderResponse");

        Schema<?> request = api.getComponents().getSchemas().get("CreateOrderRequest");
        assertThat(request.getProperties()).containsKeys("customerId", "sku", "quantity", "note");
        assertThat(request.getRequired()).containsExactlyInAnyOrder("customerId", "sku", "quantity");
        assertThat(request.getProperties().get("sku").getMaxLength()).isEqualTo(64);
        assertThat(request.getProperties().get("quantity").getMinimum()).isEqualByComparingTo("1");

        Schema<?> response = api.getComponents().getSchemas().get("OrderResponse");
        assertThat(response.getProperties())
                .containsKeys("id", "customerId", "sku", "quantity", "status", "createdAt", "note");
        assertThat(response.getRequired())
                .containsExactlyInAnyOrder("id", "customerId", "sku", "quantity", "status", "createdAt");
        assertThat(response.getProperties().get("status").getEnum())
                .containsExactly("PENDING", "CONFIRMED", "CANCELLED");
        assertThat(response.getProperties().get("createdAt").getFormat()).isEqualTo("date-time");
    }

    /**
     * ARAC SINIRI SABITLEMESI: yayinlanan 3.1 belgede istek alaninin tipini string -> integer yapmak openapi-diff'e
     * gore "degisiklik yok"tur. Gate bu yuzden 3.0 ciktisini karsilastirir (OpenApiContract javadoc'u). Bu test
     * kirilirsa arac 3.1 tiplerini okumaya baslamistir: gate 3.1'e alinabilir, bu test silinir.
     */
    @Test
    void openApiDiffIsBlindToTypeChangesInOpenApi31Documents() throws Exception {
        String published = OpenApiContract.generate(mvc);
        ObjectNode changed = OpenApiContract.tree(published);
        ((ObjectNode) SpecVariants.properties(changed, "CreateOrderRequest").get("sku")).put("type", "integer");

        ChangedOpenApi diff = OpenApiContract.diff(published, OpenApiContract.write(changed));
        assertThat(diff.isUnchanged()).as("3.1'de tip degisikligi gorunmuyor").isTrue();
    }

    @Test
    void runtimeResponsesMatchTheDocumentedContract() throws Exception {
        String spec = OpenApiContract.generate(mvc);
        Set<String> documented = new HashSet<>();
        OpenApiContract.tree(spec).get("components").get("schemas").get("OrderResponse").get("properties")
                .propertyNames().forEach(documented::add);

        var created = mvc.perform(post("/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId":"0b8f5c1e-1111-4a5e-9c1a-000000000001","sku":"SKU-7","quantity":3,"note":"kapida"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andReturn().getResponse();
        JsonNode body = OpenApiContract.JSON.readTree(created.getContentAsString(StandardCharsets.UTF_8));
        Set<String> actual = new HashSet<>(body.propertyNames());
        assertThat(actual).isEqualTo(documented);
        assertThat(body.get("createdAt").asString()).isEqualTo("2026-09-29T10:00:00Z");
        assertThat(created.getHeader("Location")).isEqualTo("/v1/orders/" + body.get("id").asString());

        mvc.perform(get(created.getHeader("Location"))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, private"));
        // Spec'te required olan alan eksikse istek servise ulasmadan 400 (sozlesme = dogrulama)
        mvc.perform(post("/v1/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"0b8f5c1e-1111-4a5e-9c1a-000000000001\",\"quantity\":3}"))
                .andExpect(status().isBadRequest());
    }
}
