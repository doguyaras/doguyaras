package com.acme.order;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction siniri kurallari (referans Bolum 4.3, 11.2, 16). Her kural iki yonlu dogrulanir:
 * gercek kodda gecer, kasitli ihlal paketinde (archfixture) kirilir. Duz JUnit @Test (ArchUnit engine'i/@ArchTest degil).
 *
 * Sinir: ArchUnit dogrudan cagrilari gorur; TX'li metot -> yardimci bean -> client gibi DOLAYLI cagri yakalanmaz.
 * Bu kural ilk savunma hattidir; asil guvence review (proj-resilience-review) ve sicak yol tablosudur.
 */
class TransactionBoundaryRulesTest {

    /** Outbox yazicisinin append* metotlari Propagation.MANDATORY ile calisir (metot veya sinif duzeyinde). */
    static final ArchRule OUTBOX_APPEND_IS_MANDATORY = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("OutboxRepository")
            .or().areDeclaredInClassesThat().haveSimpleNameEndingWith("OutboxWriter")
            .and().arePublic().and().haveNameStartingWith("append")
            .should(runWithPropagation(Propagation.MANDATORY))
            .because("outbox satiri domain transaction'iyla birlikte commit/rollback olmalidir (Bolum 11.2)");

    /** @Transactional (metot veya sinif duzeyinde) bir metot, adi *Client olan siniflari ya da RestClient/RestTemplate'i dogrudan cagirmaz. */
    static final ArchRule NO_REMOTE_CALL_INSIDE_TRANSACTION = methods()
            .that().areAnnotatedWith(Transactional.class)
            .or().areDeclaredInClassesThat().areAnnotatedWith(Transactional.class)
            .should(notCallRemoteClients())
            .because("transaction tutulurken uzak cagri baglanti havuzunu ve satir kilitlerini uzak tarafin suresine baglar (Bolum 4.3)")
            .allowEmptyShould(true);

    @Test
    void realCode_outboxAppendIsMandatory() {
        JavaClasses real = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.acme.platform.messaging");
        assertThat(real.stream().anyMatch(c -> c.getSimpleName().equals("OutboxRepository")))
                .as("kural gercek outbox yaziciyi gormeli (bos kume = sahte yesil)").isTrue();
        OUTBOX_APPEND_IS_MANDATORY.check(real);
    }

    @Test
    void realCode_noRemoteCallInsideTransaction() {
        NO_REMOTE_CALL_INSIDE_TRANSACTION.check(new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.acme.order", "com.acme.platform"));
    }

    @Test
    void fixture_nonMandatoryOutboxWriterIsCaught_classLevelMandatoryIsAccepted() {
        JavaClasses fixture = fixture();
        assertThatThrownBy(() -> OUTBOX_APPEND_IS_MANDATORY.check(fixture))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("BadOutboxWriter.append")
                .hasMessageNotContaining("ClassLevelMandatoryOutboxWriter");
    }

    @Test
    void fixture_clientCallInsideTransactionIsCaught_methodAndClassLevel() {
        JavaClasses fixture = fixture();
        assertThatThrownBy(() -> NO_REMOTE_CALL_INSIDE_TRANSACTION.check(fixture))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("BadService.cancel")
                .hasMessageContaining("BadClassLevelService.cancel");
    }

    private static JavaClasses fixture() {
        return new ClassFileImporter().importPackages("com.acme.order.archfixture");
    }

    private static ArchCondition<JavaMethod> runWithPropagation(Propagation expected) {
        return new ArchCondition<>("run with @Transactional(propagation = " + expected + ")") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Optional<Transactional> tx = method.tryGetAnnotationOfType(Transactional.class);
                if (tx.isEmpty()) tx = method.getOwner().tryGetAnnotationOfType(Transactional.class);
                boolean ok = tx.isPresent() && tx.get().propagation() == expected;
                if (!ok) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName() + " propagation "
                            + tx.map(t -> t.propagation().name()).orElse("YOK") + ", beklenen " + expected));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> notCallRemoteClients() {
        return new ArchCondition<>("not call *Client, RestClient or RestTemplate directly") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                    String owner = call.getTargetOwner().getName();
                    String simple = call.getTargetOwner().getSimpleName();
                    boolean remote = simple.endsWith("Client") || owner.startsWith("org.springframework.web.client.");
                    if (remote) {
                        events.add(SimpleConditionEvent.violated(method,
                                method.getFullName() + " TX icinde uzak cagri: " + call.getDescription()));
                    }
                }
            }
        };
    }
}
