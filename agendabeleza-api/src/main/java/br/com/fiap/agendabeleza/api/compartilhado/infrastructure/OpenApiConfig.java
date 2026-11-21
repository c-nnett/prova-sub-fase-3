package br.com.fiap.agendabeleza.api.compartilhado.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Metadados da documentação publicada pelo springdoc em {@code /swagger-ui.html}.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

	@Bean
	OpenAPI agendaBelezaOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Agenda Beleza API")
				.version("v1")
				.description("""
						Agendamento e gerenciamento de serviços de beleza e bem-estar (Tech Challenge Java, Fase 3).
						O usuário é identificado pelo cabeçalho X-Usuario-Id. Os erros seguem o formato
						application/problem+json (RFC 9457)."""));
	}

}
