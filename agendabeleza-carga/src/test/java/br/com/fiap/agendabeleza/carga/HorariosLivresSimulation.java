package br.com.fiap.agendabeleza.carga;

import static io.gatling.javaapi.core.CoreDsl.listFeeder;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

/**
 * Consulta de horários livres ({@code GET /profissionais/{id}/horarios-livres}) de um profissional e de um dia
 * sorteados entre os do salão preparado. Cada consulta calcula a agenda no domínio
 * ({@code AgendaProfissional}): jornada e funcionamento menos bloqueios e agendamentos, em passos de 15 minutos.
 */
public class HorariosLivresSimulation extends Simulation {

	private static final Duration PAUSA_ENTRE_CONSULTAS = Duration.ofSeconds(1);
	private static final int DIAS_CONSULTADOS = 7;

	private final SalaoPreparado salao = PreparacaoDaCarga.prepararSalao(ConfiguracaoDaCarga.PROFISSIONAIS);

	private final List<Map<String, Object>> consultas = salao.profissionais().stream()
			.flatMap(profissionalId -> IntStream.range(0, DIAS_CONSULTADOS)
					.mapToObj(deslocamento -> Map.<String, Object>of(
							"profissionalId", profissionalId.toString(),
							"data", salao.primeiroDia().plusDays(deslocamento).toString())))
			.toList();

	private final ScenarioBuilder horariosLivres = scenario("Horários livres")
			.feed(listFeeder(consultas).random())
			.exec(http("Horários livres do profissional").get("/profissionais/#{profissionalId}/horarios-livres")
					.queryParam("servicoId", salao.servicoId().toString())
					.queryParam("data", "#{data}")
					.check(status().is(200)))
			.pause(PAUSA_ENTRE_CONSULTAS);

	{
		setUp(horariosLivres.injectClosed(ConfiguracaoDaCarga.injecaoDasConsultas()))
				.protocols(ConfiguracaoDaCarga.protocolo())
				.assertions(ConfiguracaoDaCarga.metas(ConfiguracaoDaCarga.META_P95_CONSULTAS_MILISSEGUNDOS));
	}

}
