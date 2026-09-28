package com.acme.order;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mimari kurallarin makine zorlamasi. Her core modulde bu sinifin bir kopyasi bulunur;
 * yalniz paket koku (com.acme.order) degisir. Referans: mikroservis-mimari-referans.md Bolum 4, 16, 19.5.
 *
 * Bagimlilik: com.tngtech.archunit:archunit-junit5 (test scope).
 */
@AnalyzeClasses(packages = ArchitectureRulesTest.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    static final String ROOT = "com.acme.order";

    /** Katmanlar: controller → service → repository. Controller repository'ye dokunamaz. */
    @ArchTest
    static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy(ROOT + ".controller..")
            .layer("Service").definedBy(ROOT + ".service..")
            .layer("Repository").definedBy(ROOT + ".repository..")
            .layer("ReadModel").definedBy(ROOT + ".readmodel..")
            .layer("Outbox").definedBy(ROOT + ".outbox..", ROOT + ".worker..", ROOT + ".saga..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "ReadModel", "Outbox");

    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that().resideInAPackage(ROOT + ".controller..")
            .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".repository..", ROOT + ".entity..")
            .because("controller ince katmandir; mapping ve veri erisimi serviste yapilir");

    /** service.impl altinda yalniz *ServiceImpl bulunur. */
    @ArchTest
    static final ArchRule implPackageOnlyHoldsImpls = classes()
            .that().resideInAPackage(ROOT + ".service.impl..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("ServiceImpl");

    /** @Configuration yalniz config/ altinda. */
    @ArchTest
    static final ArchRule configurationsLiveInConfigPackage = classes()
            .that().areAnnotatedWith(Configuration.class)
            .should().resideInAPackage(ROOT + ".config..");

    /**
     * core → baska core yasak. Yalniz kendi paketi, platform starter'lari, *-api modulleri ve
     * ucuncu taraf kutuphaneler. (Maven enforcer bannedDependencies bunun birincil kontroludur.)
     */
    @ArchTest
    static final ArchRule noOtherCoreDependencies = classes()
            .that().resideInAPackage(ROOT + "..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    ROOT + "..",
                    "com.acme.platform..",
                    "com.acme..api..",
                    "java..", "javax..", "jakarta..", "org..", "com.fasterxml..", "lombok..",
                    "io..", "net..", "reactor..", "kotlin..")
            .because("baska bir *-core'a bagimlilik modul sinirini ihlal eder");

    /** Paket dongusu yok. */
    @ArchTest
    static final ArchRule noCycles = slices().matching(ROOT + ".(*)..").should().beFreeOfCycles();

    /** Her @RequestBody parametresi @Valid tasir. */
    @ArchTest
    static final ArchRule requestBodiesAreValidated = methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .should(haveValidOnEveryRequestBodyParameter());

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
    @ArchTest
    static final ArchRule apiDoesNotSeeEntities = noClasses()
            .that().resideInAPackage("com.acme..api..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + ".entity..");
}
