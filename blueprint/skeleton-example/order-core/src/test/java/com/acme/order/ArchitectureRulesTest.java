package com.acme.order;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mimari kurallarin makine zorlamasi. Her core modulde bu sinifin bir kopyasi bulunur;
 * yalniz ROOT (paket koku) degisir. Referans: mikroservis-mimari-referans.md Bolum 4, 16, 19.5.
 *
 * Bagimlilik: com.tngtech.archunit:archunit-junit5 (test scope) — kurallar duz JUnit @Test olarak
 * calisir; ArchUnit'in kendi JUnit engine'ine (@ArchTest) bagimli DEGILDIR. Neden: engine, JUnit
 * Platform major surumleriyle uyumsuz kalabiliyor ve testler sessizce "0 test" olarak gecebiliyor.
 * Spring Boot 4.1 + ArchUnit 1.5.1 ile dogrulandi (bos iskelette kasitli ihlaller yakalandi).
 */
class ArchitectureRulesTest {

    static final String ROOT = "com.acme.order";
    static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    /** Katmanlar: controller → service → repository. Controller repository'ye dokunamaz. */
    @Test
    void layersAreRespected() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true) // readmodel/outbox paketi henuz yoksa "Layer is empty" ihlali uretmesin
                .layer("Controller").definedBy(ROOT + ".controller..")
                .layer("Service").definedBy(ROOT + ".service..")
                .layer("Repository").definedBy(ROOT + ".repository..")
                .layer("ReadModel").definedBy(ROOT + ".readmodel..")
                .layer("Outbox").definedBy(ROOT + ".outbox..", ROOT + ".worker..", ROOT + ".saga..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "ReadModel", "Outbox")
                .check(classes);
    }

    @Test
    void controllersDoNotUseRepositoriesOrEntities() {
        noClasses().that().resideInAPackage(ROOT + ".controller..")
                .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".repository..", ROOT + ".entity..")
                .because("controller ince katmandir; mapping ve veri erisimi serviste yapilir")
                .check(classes);
    }

    /** service.impl altinda yalniz *ServiceImpl bulunur. */
    @Test
    void implPackageOnlyHoldsServiceImpls() {
        classes().that().resideInAPackage(ROOT + ".service.impl..").and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("ServiceImpl")
                .check(classes);
    }

    /** @Configuration yalniz config/ altinda. */
    @Test
    void configurationsLiveInConfigPackage() {
        classes().that().areAnnotatedWith(Configuration.class)
                .should().resideInAPackage(ROOT + ".config..")
                .check(classes);
    }

    /**
     * core → baska core yasak. Yalniz kendi paketi, platform starter'lari, *-api modulleri ve
     * ucuncu taraf kutuphaneler. (Maven enforcer bannedDependencies bunun birincil kontroludur.)
     */
    @Test
    void noOtherCoreDependencies() {
        classes().that().resideInAPackage(ROOT + "..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        ROOT + "..",
                        "com.acme.platform..",
                        "com.acme..api..",
                        "java..", "javax..", "jakarta..", "org..", "com.fasterxml..", "lombok..",
                        "io..", "net..", "reactor..", "kotlin..")
                .because("baska bir *-core'a bagimlilik modul sinirini ihlal eder")
                .check(classes);
    }

    /** Paket dongusu yok. */
    @Test
    void noPackageCycles() {
        slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(classes);
    }

    /** Her @RequestBody parametresi @Valid tasir. */
    @Test
    void requestBodiesAreValidated() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .should(haveValidOnEveryRequestBodyParameter());
        rule.check(classes);
    }

    private static ArchCondition<JavaMethod> haveValidOnEveryRequestBodyParameter() {
        return new ArchCondition<>("have @Valid on every @RequestBody parameter") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getParameters().forEach(p -> {
                    boolean body = p.isAnnotatedWith(RequestBody.class);
                    boolean valid = p.isAnnotatedWith(Valid.class);
                    if (body && !valid) {
                        events.add(SimpleConditionEvent.violated(method,
                                method.getFullName() + " parametresi @RequestBody ama @Valid degil"));
                    }
                });
            }
        };
    }

    /** Entity'ler servisler arasi contract olamaz: api paketinden entity'ye referans yok. */
    @Test
    void apiDoesNotSeeEntities() {
        JavaClasses api = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.acme");
        noClasses().that().resideInAPackage("com.acme..api..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".entity..")
                .check(api);
    }
}
