package com.acme.platform.security;

import static com.acme.platform.security.support.SecurityFixture.AUDIENCE;
import static com.acme.platform.security.support.SecurityFixture.BACKOFFICE;
import static com.acme.platform.security.support.SecurityFixture.GATEWAY;
import static com.acme.platform.security.support.SecurityFixture.ORDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.platform.security.support.SecurityFixture;
import com.acme.platform.security.web.ErrorResponse;
import com.acme.platform.security.web.InternalAccessPolicy;
import com.acme.platform.security.web.InternalAccessPolicy.Rule;
import com.acme.platform.security.web.ServiceJwtVerificationFilter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Bolum 9.5 "ilk eslesen kural kazanir": ayni iki kural, iki sira. Dar kural once yazilinca yalniz dar aktor gecer;
 * catch-all once yazilinca dar kurala hic bakilmaz ve catch-all'daki herkes (dar aktor haric!) gecer. Ikinci
 * konfigurasyon bilincli olarak yanlis: kural sirasinin kendisinin bir guvenlik karari oldugunu gosterir.
 */
class InternalAccessOrderingTest {

    static final Rule NARROW = new Rule("/internal/subscription/accounts/*/balance", Set.of(ORDER));
    static final Rule CATCH_ALL = new Rule("/internal/subscription/**", Set.of(BACKOFFICE, GATEWAY));

    final SecurityFixture fx = new SecurityFixture();
    final String balance = "/internal/subscription/accounts/" + UUID.randomUUID() + "/balance";

    int statusOf(MockMvc mvc, String token) throws Exception {
        return mvc.perform(get(balance).header(ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER, token))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void narrowBeforeCatchAll_onlyNarrowActorPasses() throws Exception {
        MockMvc mvc = fx.mockMvc(List.of(NARROW, CATCH_ALL));
        assertThat(statusOf(mvc, fx.order.mint(AUDIENCE, null))).isEqualTo(200);
        assertThat(statusOf(mvc, fx.backoffice.mint(AUDIENCE, null))).isEqualTo(403);   // catch-all'a ulasilmaz
        assertThat(statusOf(mvc, fx.gateway.mint(AUDIENCE, null))).isEqualTo(403);
        assertThat(fx.controller.hits).hasValue(1);
    }

    @Test
    void catchAllFirst_shadowsNarrowRule_everyoneInCatchAllPasses_narrowActorDenied() throws Exception {
        MockMvc mvc = fx.mockMvc(List.of(CATCH_ALL, NARROW));
        assertThat(statusOf(mvc, fx.backoffice.mint(AUDIENCE, null))).isEqualTo(200);
        assertThat(statusOf(mvc, fx.gateway.mint(AUDIENCE, null))).isEqualTo(200);
        assertThat(statusOf(mvc, fx.order.mint(AUDIENCE, null))).isEqualTo(403);        // dar kural golgelendi
        mvc.perform(get(balance).header(ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER, fx.order.mint(AUDIENCE, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorResponse.INTERNAL_ACCESS_DENIED));
        assertThat(fx.controller.hits).hasValue(2);
    }

    @Test
    void policyUnit_firstMatchDecidesEvenWhenLaterRuleWouldAllow() {
        var policy = new InternalAccessPolicy(List.of(CATCH_ALL, NARROW));
        assertThat(policy.allows("/internal/subscription/accounts/x/balance", ORDER)).isFalse();
        assertThat(policy.allows("/internal/subscription/accounts/x/balance", BACKOFFICE)).isTrue();
        var reversed = new InternalAccessPolicy(List.of(NARROW, CATCH_ALL));
        assertThat(reversed.allows("/internal/subscription/accounts/x/balance", ORDER)).isTrue();
        assertThat(reversed.allows("/internal/subscription/accounts/x/balance", BACKOFFICE)).isFalse();
        assertThat(reversed.allows("/internal/other", BACKOFFICE)).as("default deny").isFalse();
        // '*' tek segment: iki segmentli hesap yolu dar kurala uymaz, catch-all'a duser
        assertThat(reversed.allows("/internal/subscription/accounts/x/y/balance", ORDER)).isFalse();
        assertThat(reversed.allows("/internal/subscription/accounts/x/y/balance", BACKOFFICE)).isTrue();
    }

    @Test
    void ruleWithoutActorsOrPath_isRejectedAtConstruction() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new Rule("/internal/x", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new Rule(" ", Set.of(ORDER)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
