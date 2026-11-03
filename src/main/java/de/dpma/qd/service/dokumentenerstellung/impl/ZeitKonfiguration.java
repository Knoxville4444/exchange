package de.dpma.qd.service.dokumentenerstellung.impl;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Zeitquelle für Status und Veröffentlichung; in Tests durch eine feste Uhr ersetzbar. */
@Configuration(proxyBeanMethods = false)
public class ZeitKonfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock uhr() {
        return Clock.systemUTC();
    }
}
