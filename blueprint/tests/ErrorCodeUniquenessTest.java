package com.acme.platform.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tum servislerin ErrorCode enum'lari global olarak tekil ve kendi bloklarinda olmali.
 * Bu test, tum core modulleri classpath'ine alan bir "aggregate" test modulunde (veya platform-core'da
 * test-jar bagimliliklariyla) calisir. Bloklar README'deki tabloyla ayni tutulur.
 *
 * Referans: mikroservis-mimari-referans.md Bolum 7.2.
 */
class ErrorCodeUniquenessTest {

    /** service → [min, max]. README'deki hata kodu bloklariyla birebir. */
    private static final Map<String, int[]> BLOCKS = Map.of(
            "validation", new int[]{90000, 90099},
            "security", new int[]{90100, 90199},
            "system", new int[]{99998, 99999},
            "auth", new int[]{10000, 10999},
            "order", new int[]{11000, 11999},
            "notification", new int[]{16000, 16999},
            "backoffice", new int[]{17000, 17999});

    @Test
    void allErrorCodesAreGloballyUniqueAndInsideTheirBlock() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.acme");

        Map<Integer, String> seen = new HashMap<>();
        List<String> problems = new java.util.ArrayList<>();

        for (JavaClass jc : classes) {
            if (!jc.isEnum() || !jc.isAssignableTo(ErrorCode.class)) continue;
            Class<?> enumClass = jc.reflect();
            for (Object constant : enumClass.getEnumConstants()) {
                ErrorCode code = (ErrorCode) constant;
                String owner = enumClass.getName() + "." + ((Enum<?>) constant).name();
                String prev = seen.putIfAbsent(code.getCode(), owner);
                if (prev != null) {
                    problems.add("cakisma: " + code.getCode() + " → " + prev + " ve " + owner);
                }
                int[] block = BLOCKS.get(code.getService());
                if (block == null) {
                    problems.add("bilinmeyen service: " + code.getService() + " (" + owner + ")");
                } else if (code.getCode() < block[0] || code.getCode() > block[1]) {
                    problems.add("blok disi: " + owner + " = " + code.getCode()
                            + " (beklenen " + block[0] + "–" + block[1] + ")");
                }
                if (code.getMessage() == null || code.getMessage().isBlank() || !code.getMessage().endsWith(".")) {
                    problems.add("mesaj formati: " + owner + " → Ingilizce, nokta ile biten kisa cumle olmali");
                }
            }
        }
        assertThat(problems).as("ErrorCode kurallari").isEmpty();
        assertThat(seen).isNotEmpty();
    }
}
