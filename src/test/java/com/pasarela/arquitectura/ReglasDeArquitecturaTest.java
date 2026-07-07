package com.pasarela.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Guardián de la arquitectura hexagonal (T-003): si una frontera se viola,
 * el build falla. Las reglas reflejan la sección 4 de CLAUDE.md y
 * docs/02-arquitectura.md.
 *
 * Nota: allowEmptyShould(true) es necesario mientras los paquetes solo
 * contienen package-info; se puede retirar cuando haya clases reales.
 */
@AnalyzeClasses(packages = "com.pasarela", importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasDeArquitecturaTest {

	private static final String[] FRAMEWORKS_VETADOS_EN_DOMINIO = {
			"org.springframework..",
			"jakarta.persistence..",
			"jakarta.transaction..",
			"org.hibernate.."
	};

	@ArchTest
	static final ArchRule elDominioNoDependeDeFrameworks = noClasses()
			.that().resideInAPackage("..dominio..")
			.should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS_VETADOS_EN_DOMINIO)
			.because("el dominio es Java puro: sin Spring, sin JPA, sin Hibernate (CLAUDE.md §4)")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule elDominioNoDependeDeOtrasCapas = noClasses()
			.that().resideInAPackage("..dominio..")
			.should().dependOnClassesThat().resideInAnyPackage("..aplicacion..", "..infraestructura..")
			.because("las dependencias apuntan hacia el dominio, nunca al revés")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule laAplicacionNoDependeDeInfraestructura = noClasses()
			.that().resideInAPackage("..aplicacion..")
			.should().dependOnClassesThat().resideInAPackage("..infraestructura..")
			.because("la aplicación usa puertos del dominio; la infraestructura los implementa (inversión de dependencias)")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule pagosNoDependeDeOtrosContextos = contextoAislado("pagos", "comercios", "liquidaciones");

	@ArchTest
	static final ArchRule comerciosNoDependeDeOtrosContextos = contextoAislado("comercios", "pagos", "liquidaciones");

	@ArchTest
	static final ArchRule liquidacionesNoDependeDeOtrosContextos = contextoAislado("liquidaciones", "pagos", "comercios");

	@ArchTest
	static final ArchRule compartidoNoDependeDeNingunContexto = contextoAislado("compartido", "pagos", "comercios", "liquidaciones");

	private static ArchRule contextoAislado(String contexto, String... contextosVetados) {
		String[] paquetesVetados = new String[contextosVetados.length];
		for (int i = 0; i < contextosVetados.length; i++) {
			paquetesVetados[i] = "com.pasarela." + contextosVetados[i] + "..";
		}
		return noClasses()
				.that().resideInAPackage("com.pasarela." + contexto + "..")
				.should().dependOnClassesThat().resideInAnyPackage(paquetesVetados)
				.because("los contextos no se importan entre sí; lo común vive en compartido (CLAUDE.md §4)")
				.allowEmptyShould(true);
	}

}
