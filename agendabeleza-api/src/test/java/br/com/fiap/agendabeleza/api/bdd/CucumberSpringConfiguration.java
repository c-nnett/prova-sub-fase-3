package br.com.fiap.agendabeleza.api.bdd;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Contexto Spring dos cenários BDD: a aplicação inteira numa porta aleatória, com o PostgreSQL do Testcontainers
 * e as migrações do Flyway. Os passos chamam a API por HTTP com o RestAssured.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class CucumberSpringConfiguration {
}
