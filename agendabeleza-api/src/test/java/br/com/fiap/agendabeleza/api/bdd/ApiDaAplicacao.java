package br.com.fiap.agendabeleza.api.bdd;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Base dos passos BDD: monta as requisições para a aplicação na porta aleatória e confere as respostas. Não declara
 * passos nem ganchos, porque o Cucumber não permite herdar de classes que os declaram.
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

	/** Exige sucesso e devolve o {@code id} do recurso criado. */
	protected static UUID idCriado(Response resposta) {
		exigirSucesso(resposta);
		return UUID.fromString(resposta.jsonPath().getString("id"));
	}

	protected static void exigirSucesso(Response resposta) {
		assertThat(resposta.statusCode()).as(resposta.asString()).isBetween(200, 299);
	}

	/** Confere o status e o {@code detail} do problem+json devolvido pelo {@code TratadorDeErros}. */
	protected static void exigirRecusa(Response resposta, int status, String mensagem) {
		assertThat(resposta.statusCode()).as(resposta.asString()).isEqualTo(status);
		assertThat(resposta.jsonPath().getString("detail")).isEqualTo(mensagem);
	}

}
