package com.acme.contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.MarkdownRender;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * PR gate'inin kendisi (CI'da kosan kisim): spec'i UYGULAMADAN uretir, baseline ile openapi-diff karsilastirir.
 * Spec elle yazilmaz; baseline yalniz bilincli bir komutla guncellenir (OpenApiContractGateTest javadoc'u).
 *
 * <p>Gate, springdoc'un OpenAPI 3.0 ciktisini karsilastirir; istemciye yayinlanan belge 3.1'dir (target/openapi.json).
 * Neden: openapi-diff (2.1.2 ve 2.1.7 denendi) 3.1 belgede sema tipini okumuyor (3.1'de tip "types" kumesinde,
 * arac getType()'a bakiyor): string -> integer degisikligi 3.1'de "degisiklik yok" cikar, 3.0'da kiricidir.
 * Bu sinir OpenApiDocumentTest'te sabitlenir; arac duzelirse o test kirilir ve gate 3.1'e alinabilir.
 */
final class OpenApiContract {

    /** Gate ve diff testlerinin spec uretim ayari (annotation'da kullanilabilmesi icin derleme zamani sabiti). */
    static final String GATE_SPEC_VERSION = "springdoc.api-docs.version=openapi_3_0";
    static final String API_DOCS_ENABLED = "springdoc.api-docs.enabled=true";

    /** Modul kokune gore (surefire calisma dizini = modul dizini). Classpath kopyasi degil: guncelleme buraya yazar. */
    static final Path BASELINE = Path.of("src/test/resources/openapi-baseline.json");
    static final Path GENERATED = Path.of("target/openapi.json");
    static final String UPDATE_FLAG = "contract.updateBaseline";

    static final JsonMapper JSON = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private OpenApiContract() {}

    /** Uygulamanin gercek /v3/api-docs ciktisi (springdoc), deterministik girintiyle. */
    static String generate(MockMvc mvc) throws Exception {
        String raw = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JSON.writeValueAsString(JSON.readTree(raw)) + "\n";
    }

    static ObjectNode tree(String spec) { return (ObjectNode) JSON.readTree(spec); }

    static String write(JsonNode node) { return JSON.writeValueAsString(node) + "\n"; }

    static ChangedOpenApi diff(String baseline, String current) { return OpenApiCompare.fromContents(baseline, current); }

    static String markdown(ChangedOpenApi diff) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            new MarkdownRender().render(diff, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Gate karari: kirici (INCOMPATIBLE) fark varsa AssertionError; mesaj PR yorumuna konacak markdown ozetidir.
     * Uyumlu fark (opsiyonel alan ekleme) gecer: eski istemci bilmedigi alani yok sayar (referans 18.4).
     */
    static ChangedOpenApi assertBackwardCompatible(String baseline, String current) {
        ChangedOpenApi diff = diff(baseline, current);
        if (!diff.isCompatible()) {
            throw new AssertionError("OpenAPI kirici degisiklik (baseline -> mevcut). Yeni versiyon (/v2) acin ya da "
                    + "degisikligi geri alin; bilincli ise baseline'i guncelleyin (-D" + UPDATE_FLAG + "=true).\n"
                    + markdown(diff));
        }
        return diff;
    }

    static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void save(Path path, String content) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
