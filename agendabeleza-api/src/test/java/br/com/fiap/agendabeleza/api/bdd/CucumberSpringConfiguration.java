package br.com.fiap.agendabeleza.api.bdd;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Contexto Spring dos cenários BDD: a aplicação inteira numa porta aleatória, com o PostgreSQL do Testcontainers
 * e as migrações do Flyway. Os passos chamam a API por HTTP com o RestAssured.
 * <p>
 * O {@link RelogioAjustavel} substitui o relógio da aplicação só neste contexto.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({ TestcontainersConfiguration.class, CucumberSpringConfiguration.Relogio.class })
class CucumberSpringConfiguration {

	@TestConfiguration(proxyBeanMethods = false)
	static class Relogio {

		@Bean
		@Primary
		RelogioAjustavel relogioAjustavel() {
			return new RelogioAjustavel();
		}

	}

}
