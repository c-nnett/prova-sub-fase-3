package br.com.fiap.agendabeleza.carga;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

/**
 * Vazão de reservas válidas: {@link ConfiguracaoDaCarga#RESERVAS_POR_SEGUNDO} clientes novos por segundo, cada um
 * num par profissional e início livre ({@link HorariosDistintos}). Toda reserva passa pela trava do profissional,
 * pela restrição de exclusão e grava quatro notificações na outbox.
 */
public class ReservasEmHorariosDistintosSimulation extends Simulation {

	private static final int TOTAL_DE_RESERVAS = ConfiguracaoDaCarga.RESERVAS_POR_SEGUNDO
			* (int) ConfiguracaoDaCarga.DURACAO_DAS_RESERVAS.toSeconds();

	private final SalaoPreparado salao = PreparacaoDaCarga.prepararSalao(ConfiguracaoDaCarga.PROFISSIONAIS);

	private final ScenarioBuilder reservas = scenario("Reservas em horários distintos")
			.feed(HorariosDistintos.para(salao, TOTAL_DE_RESERVAS))
			.exec(RequisicoesDaAgendaBeleza.reservar("Reserva de horário livre", salao).check(status().is(201)));

	{
		setUp(reservas.injectOpen(constantUsersPerSec(ConfiguracaoDaCarga.RESERVAS_POR_SEGUNDO)
				.during(ConfiguracaoDaCarga.DURACAO_DAS_RESERVAS)))
				.protocols(ConfiguracaoDaCarga.protocolo())
				.assertions(ConfiguracaoDaCarga.metas(ConfiguracaoDaCarga.META_P95_RESERVAS_MILISSEGUNDOS));
	}

}
