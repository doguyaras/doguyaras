package com.acme.modulith;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import com.acme.modulith.order.OrderPlaced;

/** Seviye 1: gercek (uretim) kodun modul yapisi Bolum 16'daki kuralla dogrulanir. */
class ModuleStructureTest {

    // Varsayilan ImportOption test siniflarini disarida birakir: yalniz uretim kodu dogrulanir.
    private final ApplicationModules modules = ApplicationModules.of(ModulithApp.class);

    @Test
    void productionCodeRespectsModuleBoundaries() {
        modules.verify(); // ihlal varsa Violations firlatir
        assertThat(modules.detectViolations().hasViolations()).isFalse();
    }

    @Test
    void detectsOrderAndInventoryAndOnlyInventoryDependsOnOrder() {
        assertThat(modules.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
                .containsExactlyInAnyOrder("order", "inventory");

        ApplicationModule order = modules.getModuleByName("order").orElseThrow();
        ApplicationModule inventory = modules.getModuleByName("inventory").orElseThrow();

        // Olay order'in API'sinde (kok paket); inventory ona bagli, ters yon yok.
        assertThat(order.contains(OrderPlaced.class)).isTrue();
        assertThat(inventory.getBootstrapDependencies(modules)).isEmpty();
        assertThat(inventory.getDirectDependencies(modules).contains(order)).isTrue();
        assertThat(order.getDirectDependencies(modules).contains(inventory)).isFalse();
    }

    @Test
    void writesModuleDocumentation() throws Exception {
        Path out = Path.of("target", "spring-modulith-docs");
        new Documenter(modules, Documenter.Options.defaults().withOutputFolder(out.toString()))
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
        try (Stream<Path> files = Files.list(out)) {
            assertThat(files.map(p -> p.getFileName().toString()))
                    .contains("components.puml", "module-order.puml", "module-inventory.puml");
        }
    }
}
