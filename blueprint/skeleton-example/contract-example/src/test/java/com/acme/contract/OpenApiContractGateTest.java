package com.acme.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.node.ObjectNode;

/**
 * Senaryo 3: PR gate'i. CI bu testi her PR'da kosar; uretilen spec baseline'a gore KIRICI ise build kirilir ve hata
 * mesaji openapi-diff markdown ozetini tasir (PR yorumuna konur). Uyumlu fark (opsiyonel alan) gecer.
 *
 * <p>Baseline'i BILINCLI guncelleme komutu (kirici degisiklik onaylandiysa: yeni versiyon/uyumluluk matrisi, referans
 * 18.4; ya da uyumlu degisiklikten sonra baseline'i tazelemek icin), repo kokunden:
 *
 * <pre>
 * mvn -f blueprint/skeleton-example/pom.xml -pl contract-example -am test \
 *     -Dtest=OpenApiContractGateTest -Dsurefire.failIfNoSpecifiedTests=false -Dcontract.updateBaseline=true
 * git add blueprint/skeleton-example/contract-example/src/test/resources/openapi-baseline.json
 * </pre>
 *
 * Guncelleme modu yalniz dosyayi yazar; gate kontrolu atlanmaz, cunku yazilan baseline mevcut spec'in kendisidir
 * (fark sifir). CI bu bayragi asla vermez; baseline degisikligi PR diff'inde gorunur ve review'a girer.
 * Baseline springdoc'un OpenAPI 3.0 ciktisidir (neden: OpenApiContract javadoc'u); istemciye 3.1 belge yayinlanir.
 */
@SpringBootTest(properties = {OpenApiContract.API_DOCS_ENABLED, OpenApiContract.GATE_SPEC_VERSION})
@AutoConfigureMockMvc
class OpenApiContractGateTest {

    @Autowired MockMvc mvc;

    @Test
    void generatedSpecIsBackwardCompatibleWithCommittedBaseline() throws Exception {
        String current = OpenApiContract.generate(mvc);
        if (Boolean.getBoolean(OpenApiContract.UPDATE_FLAG)) {
            OpenApiContract.save(OpenApiContract.BASELINE, current);
            System.out.println("[contract-gate] baseline guncellendi: " + OpenApiContract.BASELINE.toAbsolutePath());
        }
        assertThat(OpenApiContract.BASELINE).as("baseline commit'lenmis olmali").exists();
        String baseline = OpenApiContract.read(OpenApiContract.BASELINE);
        // 3.1 baseline'da openapi-diff tip degisikliklerini gormez; gate sessizce korlesmesin
        assertThat(OpenApiContract.tree(baseline).get("openapi").asString()).startsWith("3.0.");

        ChangedOpenApi diff = OpenApiContract.assertBackwardCompatible(baseline, current);

        String summary = OpenApiContract.markdown(diff);
        OpenApiContract.save(OpenApiContract.GENERATED.resolveSibling("contract-diff.md"), summary);
        System.out.println("[contract-gate] sonuc=" + (diff.isUnchanged() ? "DEGISIKLIK YOK" : "UYUMLU FARK")
                + "\n" + summary);
    }

    /** Gate'in kendisinin kirici farkta gercekten kirildigini kanitlar (gate hep yesil donen bir no-op olmasin). */
    @Test
    void gateFailsWithMarkdownSummaryWhenAResponseFieldDisappears() throws Exception {
        String baseline = OpenApiContract.read(OpenApiContract.BASELINE);
        ObjectNode broken = OpenApiContract.tree(OpenApiContract.generate(mvc));
        SpecVariants.removeResponseProperty(broken, "OrderResponse", "sku");

        assertThatThrownBy(() -> OpenApiContract.assertBackwardCompatible(baseline, OpenApiContract.write(broken)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("OpenAPI kirici degisiklik")
                .hasMessageContaining("/v1/orders");
        assertThat(Files.isRegularFile(OpenApiContract.BASELINE)).isTrue();
    }
}
