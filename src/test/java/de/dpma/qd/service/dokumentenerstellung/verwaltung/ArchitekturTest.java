package de.dpma.qd.service.dokumentenerstellung.verwaltung;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Schichtentrennung der Vorlagenverwaltung per Paket:
 * <pre>
 *   api ──▶ service ◀── persistence
 *              │
 *              ▼
 *           domain   (reines Java)
 * </pre>
 */
@AnalyzeClasses(packages = "de.dpma.qd.service.dokumentenerstellung.verwaltung", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitekturTest {

    @ArchTest
    static final ArchRule schichten = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("API").definedBy("..verwaltung.api..")
            .layer("Service").definedBy("..verwaltung.service..")
            .layer("Domain").definedBy("..verwaltung.domain..")
            .layer("Persistenz").definedBy("..verwaltung.persistence..")
            .whereLayer("API").mayNotBeAccessedByAnyLayer()
            .whereLayer("Persistenz").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("API", "Persistenz")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "API", "Persistenz");

    @ArchTest
    static final ArchRule domaeneIstReinesJava = noClasses().that().resideInAPackage("..verwaltung.domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages("..verwaltung.domain..", "java..");

    @ArchTest
    static final ArchRule serviceKenntWederJpaNochWeb = noClasses().that().resideInAPackage("..verwaltung.service..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
                    "org.springframework.web..", "org.springframework.http..", "org.springframework.security..");
}
