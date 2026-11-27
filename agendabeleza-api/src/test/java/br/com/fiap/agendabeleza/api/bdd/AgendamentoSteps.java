package br.com.fiap.agendabeleza.api.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.restassured.response.Response;

/**
 * Passos da feature de agendamento: reserva, disputa pelo mesmo horário, atendimento fora da jornada e cancelamento,
 * tudo pela API, sobre o salão do {@link SalaoDoCenario}.
 * <p>
 * Na disputa, as duas reservas partem juntas de uma {@link CyclicBarrier}, como no {@code ConcorrenciaAgendaTest},
 * e chegam ao servidor ao mesmo tempo. Quem garante uma única reserva é a trava por profissional
 * ({@code ExecucaoSerializada}) com a restrição {@code agendamento_sem_conflito} (modelagem, seção 4.1).
 */
public class AgendamentoSteps extends ApiDaAplicacao {

	private static final int CLIENTES_NA_DISPUTA = 2;
	private static final long ESPERA_MAXIMA_EM_SEGUNDOS = 30;

	@Autowired
	private SalaoDoCenario salao;

	private UUID clienteId;
	private UUID agendamentoId;
	private Response resposta;
	private List<Response> respostasDaDisputa;

	@Before
	public void prepararCliente() {
		clienteId = UUID.randomUUID();
	}

	private static Instant instante(String dataHora) {
		return OffsetDateTime.parse(dataHora).toInstant();
	}

	private List<Instant> horariosLivresNoDia(String dataHora) {
		return salao.horariosLivresEm(LocalDate.parse(dataHora.substring(0, 10)));
	}

	@Dado("que o horário de {string} está livre")
	public void queOHorarioEstaLivre(String dataHora) {
		assertThat(horariosLivresNoDia(dataHora)).contains(instante(dataHora));
	}

	@Dado("que reservei o horário de {string} e o gestor confirmou")
	public void queReserveiEOGestorConfirmou(String dataHora) {
		agendamentoId = idCriado(salao.reservar(clienteId, dataHora));
		exigirSucesso(api(salao.gestorId()).post("/agendamentos/{id}/confirmar", agendamentoId));
	}

	@Quando("reservo o horário de {string}")
	public void reservoOHorario(String dataHora) {
		resposta = salao.reservar(clienteId, dataHora);
	}

	@Quando("dois clientes solicitam o horário de {string} ao mesmo tempo")
	public void doisClientesSolicitamAoMesmoTempo(String dataHora) throws Exception {
		CyclicBarrier largada = new CyclicBarrier(CLIENTES_NA_DISPUTA);
		ExecutorService clientes = Executors.newFixedThreadPool(CLIENTES_NA_DISPUTA);
		try {
			List<Future<Response>> pedidos = List.of(
					clientes.submit(() -> reservarNaLargada(largada, dataHora)),
					clientes.submit(() -> reservarNaLargada(largada, dataHora)));
			respostasDaDisputa = List.of(
					pedidos.get(0).get(ESPERA_MAXIMA_EM_SEGUNDOS, TimeUnit.SECONDS),
					pedidos.get(1).get(ESPERA_MAXIMA_EM_SEGUNDOS, TimeUnit.SECONDS));
		} finally {
			clientes.shutdownNow();
		}
	}

	/** Cada pedido é de um cliente diferente e só sai quando os dois estão prontos. */
	private Response reservarNaLargada(CyclicBarrier largada, String dataHora) throws Exception {
		UUID outroCliente = UUID.randomUUID();
		largada.await(ESPERA_MAXIMA_EM_SEGUNDOS, TimeUnit.SECONDS);
		return salao.reservar(outroCliente, dataHora);
	}

	@Quando("cancelo o agendamento com o motivo {string}")
	public void canceloOAgendamento(String motivo) {
		resposta = api(clienteId).body("""
				{ "motivo": "%s" }
				""".formatted(motivo)).post("/agendamentos/{id}/cancelar", agendamentoId);
	}

	@Entao("o agendamento é criado com status {string}")
	public void oAgendamentoECriado(String status) {
		assertThat(resposta.statusCode()).as(resposta.asString()).isEqualTo(201);
		assertThat(resposta.jsonPath().getString("status")).isEqualTo(status);
	}

	@Entao("o agendamento fica com status {string}")
	public void oAgendamentoFicaComStatus(String status) {
		assertThat(resposta.statusCode()).as(resposta.asString()).isEqualTo(200);
		assertThat(resposta.jsonPath().getString("status")).isEqualTo(status);
	}

	@Entao("exatamente uma reserva é criada com status {int}")
	public void exatamenteUmaReservaECriada(int status) {
		assertThat(respostasDaDisputa).filteredOn(respostaDaDisputa -> respostaDaDisputa.statusCode() == status)
				.hasSize(1);
	}

	@E("a outra é recusada com status {int} e a mensagem {string}")
	public void aOutraERecusada(int status, String mensagem) {
		Response recusada = respostasDaDisputa.stream()
				.filter(respostaDaDisputa -> respostaDaDisputa.statusCode() != 201)
				.findFirst()
				.orElseThrow(() -> new AssertionError("As duas reservas foram aceitas"));
		exigirRecusa(recusada, status, mensagem);
	}

	@Entao("a reserva é recusada com status {int} e a mensagem {string}")
	public void aReservaERecusada(int status, String mensagem) {
		exigirRecusa(resposta, status, mensagem);
	}

	@E("o horário de {string} não aparece entre os horários livres")
	public void oHorarioNaoAparece(String dataHora) {
		assertThat(horariosLivresNoDia(dataHora)).doesNotContain(instante(dataHora));
	}

	@E("o horário de {string} volta a aparecer entre os horários livres")
	public void oHorarioVoltaAAparecer(String dataHora) {
		queOHorarioEstaLivre(dataHora);
	}

	@E("outro cliente consegue reservar o horário de {string}")
	public void outroClienteConsegueReservar(String dataHora) {
		Response reservaDoOutroCliente = salao.reservar(UUID.randomUUID(), dataHora);
		assertThat(reservaDoOutroCliente.statusCode()).as(reservaDoOutroCliente.asString()).isEqualTo(201);
	}

}
