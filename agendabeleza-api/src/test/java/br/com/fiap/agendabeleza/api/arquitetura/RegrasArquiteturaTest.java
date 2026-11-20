package br.com.fiap.agendabeleza.api.arquitetura;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import java.util.Optional;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import jakarta.persistence.Entity;

/**
 * Regras da Clean Architecture descritas na seção 4 de {@code artefatos/arquitetura-do-sistema.md}.
 */
@AnalyzeClasses(packages = RegrasArquiteturaTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class RegrasArquiteturaTest {

	static final String BASE = "br.com.fiap.agendabeleza.api";

	private static final String DOMAIN = BASE + "..domain..";
	private static final String APPLICATION = BASE + "..application..";
	private static final String ADAPTERS = BASE + "..adapters..";
	private static final String INFRASTRUCTURE = BASE + "..infrastructure..";

	private static final String[] MODULOS_DE_NEGOCIO = {
			BASE + ".identidade..",
			BASE + ".estabelecimentos..",
			BASE + ".profissionais..",
			BASE + ".agendamentos..",
			BASE + ".avaliacoes..",
			BASE + ".busca..",
			BASE + ".notificacoes..",
			BASE + ".calendario.."
	};

	@ArchTest
	static final ArchRule camadasApontamParaDentro = layeredArchitecture()
			.consideringOnlyDependenciesInLayers()
			.withOptionalLayers(true)
			.layer("Domain").definedBy(DOMAIN)
			.layer("Application").definedBy(APPLICATION)
			.layer("Adapters").definedBy(ADAPTERS)
			.layer("Infrastructure").definedBy(INFRASTRUCTURE)
			.whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Infrastructure")
			.whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Infrastructure")
			.whereLayer("Adapters").mayOnlyBeAccessedByLayers("Infrastructure")
			.whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
			.because("as dependências sempre apontam para dentro: infrastructure -> adapters -> application -> domain")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule applicationDependeSomenteDeDomainEJava = classes()
			.that().resideInAPackage(APPLICATION)
			.should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, APPLICATION, "java..")
			.because("casos de uso e portas dependem somente do domínio e da biblioteca padrão do Java")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule domainDependeSomenteDeDomainEJava = classes()
			.that().resideInAPackage(DOMAIN)
			.should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..")
			.because("o domínio contém apenas regras de negócio puras")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule nucleoSemFrameworks = noClasses()
			.that().resideInAnyPackage(DOMAIN, APPLICATION)
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "com.google.api..")
			.because("domain e application não conhecem Spring, JPA ou APIs do Google")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule modulosSeAcessamSomentePorContratos = classes()
			.that().resideInAPackage(BASE + "..")
			.should(naoAcessarAdaptersOuInfrastructureDeOutroModulo())
			.because("um módulo só acessa outro pelos contratos de application ou por eventos")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule compartilhadoNaoDependeDeModulos = noClasses()
			.that().resideInAPackage(BASE + ".compartilhado..")
			.should().dependOnClassesThat().resideInAnyPackage(MODULOS_DE_NEGOCIO)
			.because("compartilhado é a base comum e não pode depender dos módulos de negócio")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule controllersNaoAcessamRepositorios = noClasses()
			.that().resideInAPackage(BASE + "..adapters.in..")
			.should().dependOnClassesThat(
					resideInAPackage(BASE + "..adapters.out..")
							.or(assignableTo(org.springframework.data.repository.Repository.class))
							.or(simpleNameEndingWith("Repository")))
			.because("controllers chamam casos de uso, nunca repositórios")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule entidadesJpaFicamNaPersistencia = classes()
			.that().areAnnotatedWith(Entity.class)
			.should().resideInAPackage(BASE + "..adapters.out.persistence..")
			.because("a entidade JPA é separada da entidade de domínio e fica no adaptador de persistência")
			.allowEmptyShould(true);

	private static ArchCondition<JavaClass> naoAcessarAdaptersOuInfrastructureDeOutroModulo() {
		return new ArchCondition<>("não acessar adapters ou infrastructure de outro módulo") {
			@Override
			public void check(JavaClass origem, ConditionEvents events) {
				Optional<String> moduloOrigem = modulo(origem);
				if (moduloOrigem.isEmpty()) {
					return;
				}
				for (Dependency dependencia : origem.getDirectDependenciesFromSelf()) {
					JavaClass alvo = dependencia.getTargetClass();
					Optional<String> moduloAlvo = modulo(alvo);
					boolean outroModulo = moduloAlvo.isPresent() && !moduloAlvo.equals(moduloOrigem);
					if (outroModulo && camadaInterna(alvo)) {
						events.add(SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
					}
				}
			}
		};
	}

	private static Optional<String> modulo(JavaClass classe) {
		String pacote = classe.getPackageName();
		if (!pacote.startsWith(BASE + ".")) {
			return Optional.empty();
		}
		String resto = pacote.substring(BASE.length() + 1);
		int ponto = resto.indexOf('.');
		return Optional.of(ponto < 0 ? resto : resto.substring(0, ponto));
	}

	private static boolean camadaInterna(JavaClass classe) {
		String pacote = classe.getPackageName() + ".";
		return pacote.contains(".adapters.") || pacote.contains(".infrastructure.");
	}

}
