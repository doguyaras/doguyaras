package com.acme.runtime.subscription.chaos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** YALNIZ TEST kablolamasi; runtime.chaos.enabled=true degilse hicbir bean olusmaz. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "runtime.chaos.enabled", havingValue = "true")
public class ChaosConfig {

    @Bean
    public ChaosState chaosState() { return new ChaosState(); }

    @Bean
    public FilterRegistrationBean<ChaosFilter> chaosFilter(ChaosState state) {
        var reg = new FilterRegistrationBean<>(new ChaosFilter(state));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);                  // servis JWT filtresinden (+10) sonra
        reg.addUrlPatterns("/internal/subscription/*");
        return reg;
    }
}
