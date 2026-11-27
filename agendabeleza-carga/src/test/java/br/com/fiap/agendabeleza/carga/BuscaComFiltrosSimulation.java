package br.com.fiap.agendabeleza.carga;

import static io.gatling.javaapi.core.CoreDsl.listFeeder;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

/**
 * Busca com filtros combinados ({@code GET /busca/estabelecimentos}), alternando quatro combinações. A primeira é o
 * trecho do nome, que usa {@code LIKE '%trecho%'} sem índice (risco anotado em 26/11); as outras cobrem localização e
 * serviço, categoria com faixa de preço, e disponibilidade em uma data.
 */
public class BuscaComFiltrosSimulation extends Simulation {

	private static final Duration PAUSA_ENTRE_CONSULTAS = Duration.ofSeconds(1);

	private final SalaoPreparado salao = PreparacaoDaCarga.prepararSalao(1);

	private final List<Map<String, Object>> combinacoes = List.of(
			Map.of("parametros", Map.of("nome", "Carga")),
			Map.of("parametros", Map.of("cidade", PreparacaoDaCarga.CIDADE, "servico", "Corte")),
			Map.of("parametros", Map.of("categoria", "CABELO", "precoMinimo", "10", "precoMaximo", "100",
					"ordem", "NOME")),
			Map.of("parametros", Map.of("cidade", PreparacaoDaCarga.CIDADE, "data", salao.primeiroDia().toString(),
					"horaInicio", "09:00", "horaFim", "12:00")));

	private final ScenarioBuilder busca = scenario("Busca com filtros")
			.feed(listFeeder(combinacoes).circular())
			.exec(http("Busca de estabelecimentos").get("/busca/estabelecimentos")
					.queryParamMap(sessao -> sessao.getMap("parametros"))
					.check(status().is(200)))
			.pause(PAUSA_ENTRE_CONSULTAS);

	{
		setUp(busca.injectClosed(ConfiguracaoDaCarga.injecaoDasConsultas()))
				.protocols(ConfiguracaoDaCarga.protocolo())
				.assertions(ConfiguracaoDaCarga.metas(ConfiguracaoDaCarga.META_P95_CONSULTAS_MILISSEGUNDOS));
	}

}
