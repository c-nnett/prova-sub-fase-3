package br.com.fiap.agendabeleza.carga;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

/**
 * Requisições usadas por mais de uma simulação. A reserva lê o profissional e o início da sessão
 * ({@code profissionalId} e {@code inicio}), que cada cenário preenche do seu jeito: o mesmo par para todos na
 * disputa, um par diferente por usuário nas reservas distintas.
 */
public final class RequisicoesDaAgendaBeleza {

	private RequisicoesDaAgendaBeleza() {
	}

	public static HttpRequestActionBuilder reservar(String nomeDaRequisicao, SalaoPreparado salao) {
		return http(nomeDaRequisicao).post("/agendamentos")
				.header(ConfiguracaoDaCarga.USUARIO, ConfiguracaoDaCarga::clienteNovo)
				.body(StringBody("""
						{ "profissionalId": "#{profissionalId}", "servicoId": "%s", "inicio": "#{inicio}",
						  "emailCliente": "cliente@carga.com" }
						""".formatted(salao.servicoId())));
	}

}
