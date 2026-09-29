package com.acme.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.node.ObjectNode;

/**
 * Senaryo 2: openapi-diff (openapi-diff-core 2.1.2) siniflandirmasinin referans 18.4 sozlesmesiyle ortustugu.
 * Mevcut (uretilen) spec baseline ile uyumlu; mevcut spec'ten turetilen varyantlarda kirici/uyumlu karar dogru.
 * Her varyantin markdown ozeti target/contract-diff/*.md'ye ve stdout'a yazilir (kanit).
 */
@SpringBootTest(properties = {OpenApiContract.API_DOCS_ENABLED, OpenApiContract.GATE_SPEC_VERSION})
@AutoConfigureMockMvc
class OpenApiDiffTest {

    @Autowired MockMvc mvc;
    String current;

    @BeforeEach
    void generateCurrentSpec() throws Exception { current = OpenApiContract.generate(mvc); }

    @Test
    void currentSpecIsCompatibleWithCommittedBaseline() {
        ChangedOpenApi diff = OpenApiContract.diff(OpenApiContract.read(OpenApiContract.BASELINE), current);
        report("baseline-vs-current", diff);
        assertThat(diff.isCompatible()).isTrue();
    }

    @Test
    void removingAResponsePropertyIsBreaking() {
        ChangedOpenApi diff = variant("remove-response-property",
                spec -> SpecVariants.removeResponseProperty(spec, "OrderResponse", "sku"));
        assertThat(diff.isIncompatible()).isTrue();
        assertThat(diff.getChangedOperations()).hasSize(2); // GET ve POST ayni DTO'yu doner
    }

    @Test
    void renamingARequiredRequestPropertyIsBreaking() {
        ChangedOpenApi diff = variant("rename-request-property",
                spec -> SpecVariants.renameProperty(spec, "CreateOrderRequest", "sku", "productCode"));
        assertThat(diff.isIncompatible()).isTrue();
    }

    /**
     * ARAC SINIRI (bilincli belgelenir): OPSIYONEL bir istek alaninin adini degistirmek openapi-diff'e gore UYUMLUDUR
     * (istekten alan kaldirmak "sunucu artik okumuyor" sayilir). Oysa eski istemci "note" gondermeye devam eder ve
     * veri sessizce kaybolur. Bu yuzden gate tek basina yetmez: yeniden adlandirma review'da yakalanir ya da eski ad
     * bir surum boyunca alias olarak kabul edilir.
     */
    @Test
    void renamingAnOptionalRequestPropertyIsNotFlaggedByTheTool() {
        ChangedOpenApi diff = variant("rename-optional-request-property",
                spec -> SpecVariants.renameProperty(spec, "CreateOrderRequest", "note", "comment"));
        assertThat(diff.isCompatible()).isTrue();
        assertThat(diff.isUnchanged()).isFalse();
    }

    @Test
    void addingAnOptionalResponsePropertyIsCompatible() {
        ChangedOpenApi diff = variant("add-optional-response-property",
                spec -> SpecVariants.addOptionalProperty(spec, "OrderResponse", "variantOnlyEstimatedDelivery", "string"));
        assertThat(diff.isCompatible()).isTrue();
        assertThat(diff.isUnchanged()).isFalse(); // fark gorulur ama kirici degil
    }

    @Test
    void makingAnOptionalRequestFieldRequiredIsBreaking() {
        ChangedOpenApi diff = variant("make-request-field-required",
                spec -> SpecVariants.makeRequired(spec, "CreateOrderRequest", "note"));
        assertThat(diff.isIncompatible()).isTrue();
    }

    @Test
    void addingARequiredRequestPropertyIsBreaking() {
        ChangedOpenApi diff = variant("add-required-request-property",
                spec -> SpecVariants.addRequiredProperty(spec, "CreateOrderRequest", "variantOnlyChannel", "string"));
        assertThat(diff.isIncompatible()).isTrue();
    }

    /** Referans 18.4 "enum degeri ekleme: tuketici once": yanitta yeni enum degeri eski istemci icin kiricidir. */
    @Test
    void addingAnEnumValueToAResponseIsBreaking() {
        ChangedOpenApi diff = variant("add-response-enum-value",
                spec -> SpecVariants.addEnumValue(spec, "OrderResponse", "status", "SHIPPED"));
        assertThat(diff.isIncompatible()).isTrue();
    }

    /** Gate'in 3.0 ciktisini karsilastirmasinin nedeni: ayni degisiklik 3.1 belgede gorunmez (OpenApiDocumentTest). */
    @Test
    void changingARequestPropertyTypeIsBreaking() {
        ChangedOpenApi diff = variant("change-request-property-type",
                spec -> ((ObjectNode) SpecVariants.properties(spec, "CreateOrderRequest").get("sku")).put("type", "integer"));
        assertThat(diff.isIncompatible()).isTrue();
    }

    /**
     * ARAC SINIRI: operationId istemci kodundaki metod adidir (getOrder -> findOrder istemcide derleme hatasi), ama
     * openapi-diff bunu yalniz "degisti" olarak gorur, kirici saymaz. Controller metod adini degistirmek review konusudur
     * (ya da operationId @Operation ile sabitlenir).
     */
    @Test
    void renamingAnOperationIdIsNotFlaggedAsBreakingByTheTool() {
        ChangedOpenApi diff = variant("rename-operation-id", spec -> ((ObjectNode) spec.get("paths")
                .get("/v1/orders/{id}").get("get")).put("operationId", "findOrder"));
        assertThat(diff.isUnchanged()).isFalse();
        assertThat(diff.isCompatible()).isTrue();
    }

    private ChangedOpenApi variant(String name, Consumer<ObjectNode> change) {
        ObjectNode spec = OpenApiContract.tree(current);
        change.accept(spec);
        ChangedOpenApi diff = OpenApiContract.diff(current, OpenApiContract.write(spec));
        report(name, diff);
        return diff;
    }

    private static void report(String name, ChangedOpenApi diff) {
        String markdown = OpenApiContract.markdown(diff);
        OpenApiContract.save(OpenApiContract.GENERATED.resolveSibling("contract-diff").resolve(name + ".md"), markdown);
        System.out.println("[openapi-diff] " + name + " -> " + diff.isChanged() + "\n" + markdown);
    }
}
