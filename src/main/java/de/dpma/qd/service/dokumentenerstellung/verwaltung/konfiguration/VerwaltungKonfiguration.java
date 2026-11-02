package de.dpma.qd.service.dokumentenerstellung.verwaltung.konfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Infrastruktur der Vorlagenverwaltung. */
@Configuration(proxyBeanMethods = false)
public class VerwaltungKonfiguration {

    /** Zeitquelle für Status und Veröffentlichung; in Tests durch eine verstellbare Uhr ersetzbar. */
    @Bean
    @ConditionalOnMissingBean
    public Clock uhr() {
        return Clock.systemUTC();
    }
}
