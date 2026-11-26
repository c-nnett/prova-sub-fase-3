package br.com.fiap.agendabeleza.carga;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.time.LocalTime;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

/**
 * Disputa pelo mesmo horário: {@link ConfiguracaoDaCarga#CLIENTES_NA_DISPUTA} clientes diferentes pedem, ao mesmo
 * tempo, o mesmo horário da mesma profissional. Exatamente um recebe 201 e os demais 409 (arquitetura, seção 9.3).
 * <p>
 * A primeira verificação aceita 201 ou 409, para que um erro de servidor apareça com outra mensagem no relatório; a
 * segunda exige 201, então cada 409 conta como falha esperada. A asserção exige uma única reserva bem-sucedida, e
 * {@link #after()} confere no painel do gestor que só um agendamento foi gravado.
 */
public class DisputaPeloMesmoHorarioSimulation extends Simulation {

	private static final String RESERVA_DISPUTADA = "Reserva do mesmo horário";
	private static final LocalTime HORARIO_DISPUTADO = LocalTime.of(10, 0);

	private final SalaoPreparado salao = PreparacaoDaCarga.prepararSalao(1);

	private final ScenarioBuilder disputa = scenario("Disputa pelo mesmo horário")
			.exec(sessao -> sessao
					.set("profissionalId", salao.profissionais().getFirst().toString())
					.set("inicio", HorariosDistintos.inicio(salao.primeiroDia(), HORARIO_DISPUTADO)))
			.exec(RequisicoesDaAgendaBeleza.reservar(RESERVA_DISPUTADA, salao)
					.check(status().in(201, 409), status().is(201)));

	{
		setUp(disputa.injectOpen(atOnceUsers(ConfiguracaoDaCarga.CLIENTES_NA_DISPUTA)))
				.protocols(ConfiguracaoDaCarga.protocolo())
				.assertions(
						details(RESERVA_DISPUTADA).successfulRequests().count().is(1L),
						details(RESERVA_DISPUTADA).failedRequests().count()
								.is((long) ConfiguracaoDaCarga.CLIENTES_NA_DISPUTA - 1));
	}

	@Override
	public void after() {
		int gravados = PreparacaoDaCarga.totalDeAgendamentosNoDia(salao, salao.primeiroDia());
		System.out.println("Conferência no painel do gestor: " + gravados + " agendamento(s) gravado(s) para "
				+ ConfiguracaoDaCarga.CLIENTES_NA_DISPUTA + " pedidos simultâneos");
		if (gravados != 1) {
			throw new IllegalStateException("Esperado 1 agendamento gravado, encontrados " + gravados);
		}
	}

}
