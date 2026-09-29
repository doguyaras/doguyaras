package com.acme.modulith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Seviye 1: verify() gercekten bir sinir ihlalini yakaliyor mu? (Negatif kanit olmadan "verify gecti"
 * bir sey soylemez.) Ihlalli sinif src/test/java altinda, order paketinde: LeakyOrderStockPeek ->
 * inventory.internal.StockRepository. Test siniflarini da iceren bir ImportOption ile modul modeli kurulur.
 */
class ModuleBoundaryViolationTest {

    // Uretim siniflari + yalniz order paketindeki test siniflari. Tum test siniflarini almak baska (alakasiz)
    // ihlaller de getirir: Modulith, test sinifindaki @Autowired alanlari "field injection" ihlali sayar.
    private static final ImportOption INCLUDE_TEST_CLASSES = location ->
            !location.contains("/test-classes/") || location.contains("/test-classes/com/acme/modulith/order/");

    @Test
    void verifyRejectsOrderReachingIntoInventoryInternals() {
        ApplicationModules withTests = ApplicationModules.of(ModulithApp.class, INCLUDE_TEST_CLASSES);

        // Ihlalli sinif gercekten order moduline atfedildi mi (yoksa test sessizce bosa gecer)
        assertThat(withTests.getModuleByName("order").orElseThrow()
                .contains("com.acme.modulith.order.LeakyOrderStockPeek")).isTrue();

        assertThatThrownBy(withTests::verify)
                .isInstanceOf(Violations.class)
                .hasMessageContaining("LeakyOrderStockPeek")
                .hasMessageContaining("com.acme.modulith.inventory.internal.StockRepository");

        // Ihlal tam olarak bu bagimlilik: baska (ornegin gecerli) baglar ihlal sayilmiyor.
        Violations violations = withTests.detectViolations();
        assertThat(violations.getMessages())
                .isNotEmpty()
                .allSatisfy(m -> assertThat(m).contains("LeakyOrderStockPeek"));
    }
}
