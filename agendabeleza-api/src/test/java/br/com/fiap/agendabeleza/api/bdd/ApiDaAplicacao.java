package br.com.fiap.agendabeleza.api.bdd;

import static io.restassured.RestAssured.given;

import java.util.UUID;

import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Base dos passos BDD: monta as requisições para a aplicação na porta aleatória. Não declara passos nem ganchos,
 * porque o Cucumber não permite herdar de classes que os declaram.
 */
public abstract class ApiDaAplicacao {

	protected static final String USUARIO = "X-Usuario-Id";

	@LocalServerPort
	private int porta;

	/** @param usuarioId nulo para uma chamada pública, sem o cabeçalho de usuário */
	protected RequestSpecification api(UUID usuarioId) {
		RequestSpecification requisicao = given().port(porta).basePath("/api/v1").contentType(ContentType.JSON);
		return usuarioId == null ? requisicao : requisicao.header(USUARIO, usuarioId);
	}

}
