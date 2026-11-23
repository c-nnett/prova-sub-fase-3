package br.com.fiap.agendabeleza.api.agendamentos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class AgendamentoTest {

	private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID GESTOR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
	private static final UUID ESTABELECIMENTO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

	private static final Instant AGORA = Instant.parse("2026-11-23T12:00:00Z");
	private static final Instant INICIO = Instant.parse("2026-11-24T13:00:00Z");
	private static final Instant FIM = INICIO.plus(Duration.ofMinutes(60));
	private static final Instant DEPOIS_DO_INICIO = INICIO.plus(Duration.ofMinutes(70));

	private Agendamento reservar(Instant inicio, int duracaoMinutos, BigDecimal preco, String observacoes) {
		return Agendamento.reservar(CLIENTE_ID, ESTABELECIMENTO_ID, PROFISSIONAL_ID, SERVICO_ID, inicio,
				duracaoMinutos, preco, observacoes, AGORA);
	}

	private Agendamento reservado() {
		return reservar(INICIO, 60, new BigDecimal("80"), "Prefiro tesoura");
	}

	private Agendamento comStatus(StatusAgendamento status) {
		return Agendamento.reconstituir(UUID.randomUUID(), CLIENTE_ID, ESTABELECIMENTO_ID, PROFISSIONAL_ID,
				SERVICO_ID, INICIO, FIM, status, new BigDecimal("80.00"), null, 3L);
	}

	// Reserva

	@Test
	void reservaComStatusAgendadoEFimCalculadoPelaDuracao() {
		Agendamento agendamento = reservado();

		assertThat(agendamento.id()).isNotNull();
		assertThat(agendamento.clienteId()).isEqualTo(CLIENTE_ID);
		assertThat(agendamento.estabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
		assertThat(agendamento.profissionalId()).isEqualTo(PROFISSIONAL_ID);
		assertThat(agendamento.servicoId()).isEqualTo(SERVICO_ID);
		assertThat(agendamento.inicio()).isEqualTo(INICIO);
		assertThat(agendamento.fim()).isEqualTo(FIM);
		assertThat(agendamento.duracaoMinutos()).isEqualTo(60);
		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.AGENDADO);
		assertThat(agendamento.ocupaHorario()).isTrue();
		assertThat(agendamento.preco()).isEqualTo(new BigDecimal("80.00"));
		assertThat(agendamento.observacoes()).isEqualTo("Prefiro tesoura");
		assertThat(agendamento.versao()).isNull();
		assertThat(agendamento.pertenceAoCliente(CLIENTE_ID)).isTrue();
		assertThat(agendamento.pertenceAoCliente(UUID.randomUUID())).isFalse();
	}

	@Test
	void reservaRegistraAPrimeiraAlteracaoSemStatusAnterior() {
		Agendamento agendamento = reservado();

		assertThat(agendamento.alteracoesPendentes()).singleElement().satisfies(alteracao -> {
			assertThat(alteracao.statusAnterior()).isNull();
			assertThat(alteracao.statusNovo()).isEqualTo(StatusAgendamento.AGENDADO);
			assertThat(alteracao.inicioAnterior()).isNull();
			assertThat(alteracao.inicioNovo()).isNull();
			assertThat(alteracao.motivo()).isNull();
			assertThat(alteracao.alteradoPor()).isEqualTo(CLIENTE_ID);
			assertThat(alteracao.ocorridoEm()).isEqualTo(AGORA);
		});
	}

	@Test
	void reservaSemObservacoesQuandoVazias() {
		assertThat(reservar(INICIO, 60, BigDecimal.TEN, "   ").observacoes()).isNull();
	}

	@Test
	void recusaReservaNoPassadoOuNoInstanteAtual() {
		assertThatThrownBy(() -> reservar(AGORA, 60, BigDecimal.TEN, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("inicio deve estar no futuro");
		assertThatThrownBy(() -> reservar(AGORA.minusSeconds(1), 60, BigDecimal.TEN, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("inicio deve estar no futuro");
	}

	@Test
	void recusaDuracaoNaoPositiva() {
		assertThatThrownBy(() -> reservar(INICIO, 0, BigDecimal.TEN, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("duracaoMinutos deve ser maior que zero");
	}

	@Test
	void recusaPrecoAusenteNegativoOuComMaisDeDuasCasas() {
		assertThatThrownBy(() -> reservar(INICIO, 60, null, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("preco é obrigatório");
		assertThatThrownBy(() -> reservar(INICIO, 60, new BigDecimal("-0.01"), null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("preco não pode ser negativo");
		assertThatThrownBy(() -> reservar(INICIO, 60, new BigDecimal("10.001"), null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("preco deve ter no máximo duas casas decimais");
	}

	@Test
	void recusaObservacoesLongas() {
		assertThatThrownBy(() -> reservar(INICIO, 60, BigDecimal.TEN, "x".repeat(501)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("observacoes deve ter no máximo 500 caracteres");
	}

	@Test
	void recusaReservaSemIdentificadores() {
		assertThatThrownBy(() -> Agendamento.reservar(null, ESTABELECIMENTO_ID, PROFISSIONAL_ID, SERVICO_ID,
				INICIO, 60, BigDecimal.TEN, null, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("clienteId é obrigatório");
		assertThatThrownBy(() -> Agendamento.reservar(CLIENTE_ID, ESTABELECIMENTO_ID, null, SERVICO_ID,
				INICIO, 60, BigDecimal.TEN, null, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("profissionalId é obrigatório");
		assertThatThrownBy(() -> Agendamento.reservar(CLIENTE_ID, ESTABELECIMENTO_ID, PROFISSIONAL_ID, SERVICO_ID,
				null, 60, BigDecimal.TEN, null, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("inicio é obrigatório");
	}

	// Transições válidas

	@Test
	void confirmaAgendamentoAgendado() {
		Agendamento agendamento = comStatus(StatusAgendamento.AGENDADO);

		agendamento.confirmar(GESTOR_ID, AGORA);

		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.CONFIRMADO);
		assertThat(agendamento.alteracoesPendentes())
				.extracting(AlteracaoAgendamento::statusAnterior, AlteracaoAgendamento::statusNovo,
						AlteracaoAgendamento::alteradoPor, AlteracaoAgendamento::ocorridoEm)
				.containsExactly(tuple(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, GESTOR_ID, AGORA));
	}

	@ParameterizedTest
	@EnumSource(value = StatusAgendamento.class, names = { "AGENDADO", "CONFIRMADO" })
	void cancelaAgendamentoAtivoComMotivoELiberaOHorario(StatusAgendamento status) {
		Agendamento agendamento = comStatus(status);

		agendamento.cancelar("  Imprevisto  ", CLIENTE_ID, AGORA);

		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.CANCELADO);
		assertThat(agendamento.ocupaHorario()).isFalse();
		assertThat(agendamento.alteracoesPendentes()).singleElement().satisfies(alteracao -> {
			assertThat(alteracao.statusAnterior()).isEqualTo(status);
			assertThat(alteracao.statusNovo()).isEqualTo(StatusAgendamento.CANCELADO);
			assertThat(alteracao.motivo()).isEqualTo("Imprevisto");
		});
	}

	@Test
	void cancelaSemMotivo() {
		Agendamento agendamento = comStatus(StatusAgendamento.AGENDADO);

		agendamento.cancelar(null, CLIENTE_ID, AGORA);

		assertThat(agendamento.alteracoesPendentes()).singleElement()
				.extracting(AlteracaoAgendamento::motivo).isNull();
	}

	@Test
	void recusaMotivoLongo() {
		Agendamento agendamento = comStatus(StatusAgendamento.AGENDADO);

		assertThatThrownBy(() -> agendamento.cancelar("x".repeat(251), CLIENTE_ID, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("motivo deve ter no máximo 250 caracteres");
		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.AGENDADO);
	}

	@ParameterizedTest
	@EnumSource(value = StatusAgendamento.class, names = { "AGENDADO", "CONFIRMADO" })
	void reagendaMantendoADuracaoEVoltandoParaAgendado(StatusAgendamento status) {
		Agendamento agendamento = comStatus(status);
		Instant novoInicio = INICIO.plus(Duration.ofDays(2));

		agendamento.reagendar(novoInicio, CLIENTE_ID, AGORA);

		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.AGENDADO);
		assertThat(agendamento.inicio()).isEqualTo(novoInicio);
		assertThat(agendamento.fim()).isEqualTo(novoInicio.plus(Duration.ofMinutes(60)));
		assertThat(agendamento.duracaoMinutos()).isEqualTo(60);
		assertThat(agendamento.alteracoesPendentes()).singleElement().satisfies(alteracao -> {
			assertThat(alteracao.statusAnterior()).isEqualTo(status);
			assertThat(alteracao.statusNovo()).isEqualTo(StatusAgendamento.AGENDADO);
			assertThat(alteracao.inicioAnterior()).isEqualTo(INICIO);
			assertThat(alteracao.inicioNovo()).isEqualTo(novoInicio);
		});
	}

	@Test
	void recusaReagendarParaOPassadoOuParaOMesmoInicio() {
		Agendamento agendamento = comStatus(StatusAgendamento.AGENDADO);

		assertThatThrownBy(() -> agendamento.reagendar(AGORA.minusSeconds(60), CLIENTE_ID, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("inicio deve estar no futuro");
		assertThatThrownBy(() -> agendamento.reagendar(INICIO, CLIENTE_ID, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("o novo início deve ser diferente do atual");
		assertThat(agendamento.inicio()).isEqualTo(INICIO);
		assertThat(agendamento.alteracoesPendentes()).isEmpty();
	}

	@Test
	void concluiAgendamentoConfirmadoDepoisDoInicio() {
		Agendamento agendamento = comStatus(StatusAgendamento.CONFIRMADO);

		agendamento.concluir(GESTOR_ID, DEPOIS_DO_INICIO);

		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.CONCLUIDO);
		assertThat(agendamento.ocupaHorario()).isFalse();
		assertThat(agendamento.alteracoesPendentes()).singleElement()
				.extracting(AlteracaoAgendamento::statusAnterior, AlteracaoAgendamento::statusNovo)
				.containsExactly(StatusAgendamento.CONFIRMADO, StatusAgendamento.CONCLUIDO);
	}

	@Test
	void registraNaoComparecimentoNoInstanteDoInicio() {
		Agendamento agendamento = comStatus(StatusAgendamento.CONFIRMADO);

		agendamento.registrarNaoComparecimento(GESTOR_ID, INICIO);

		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.NAO_COMPARECEU);
		assertThat(agendamento.alteracoesPendentes()).singleElement()
				.extracting(AlteracaoAgendamento::statusNovo).isEqualTo(StatusAgendamento.NAO_COMPARECEU);
	}

	@Test
	void recusaConcluirOuRegistrarFaltaAntesDoInicio() {
		Agendamento agendamento = comStatus(StatusAgendamento.CONFIRMADO);

		assertThatThrownBy(() -> agendamento.concluir(GESTOR_ID, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("o atendimento só pode ser concluído depois do início");
		assertThatThrownBy(() -> agendamento.registrarNaoComparecimento(GESTOR_ID, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("o não comparecimento só pode ser registrado depois do início");
		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.CONFIRMADO);
	}

	@Test
	void acumulaAsAlteracoesNaOrdem() {
		Agendamento agendamento = reservado();

		agendamento.confirmar(GESTOR_ID, AGORA);
		agendamento.concluir(GESTOR_ID, DEPOIS_DO_INICIO);

		assertThat(agendamento.alteracoesPendentes())
				.extracting(AlteracaoAgendamento::statusNovo)
				.containsExactly(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO,
						StatusAgendamento.CONCLUIDO);
	}

	// Transições inválidas

	@Test
	void recusaConfirmarAgendamentoJaConfirmado() {
		Agendamento agendamento = comStatus(StatusAgendamento.CONFIRMADO);

		assertThatThrownBy(() -> agendamento.confirmar(GESTOR_ID, AGORA))
				.isInstanceOf(TransicaoInvalidaException.class)
				.hasMessage("não é possível confirmar um agendamento CONFIRMADO");
	}

	@Test
	void recusaConcluirOuRegistrarFaltaSemConfirmacao() {
		Agendamento agendamento = comStatus(StatusAgendamento.AGENDADO);

		assertThatThrownBy(() -> agendamento.concluir(GESTOR_ID, DEPOIS_DO_INICIO))
				.isInstanceOf(TransicaoInvalidaException.class)
				.hasMessage("não é possível concluir um agendamento AGENDADO");
		assertThatThrownBy(() -> agendamento.registrarNaoComparecimento(GESTOR_ID, DEPOIS_DO_INICIO))
				.isInstanceOf(TransicaoInvalidaException.class)
				.hasMessage("não é possível registrar o não comparecimento de um agendamento AGENDADO");
		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.AGENDADO);
	}

	@ParameterizedTest
	@EnumSource(value = StatusAgendamento.class, names = { "CONCLUIDO", "CANCELADO", "NAO_COMPARECEU" })
	void estadoFinalRecusaQualquerOperacao(StatusAgendamento status) {
		Agendamento agendamento = comStatus(status);
		Instant novoInicio = INICIO.plus(Duration.ofDays(1));

		assertTransicaoInvalida(() -> agendamento.confirmar(GESTOR_ID, AGORA));
		assertTransicaoInvalida(() -> agendamento.cancelar(null, CLIENTE_ID, AGORA));
		assertTransicaoInvalida(() -> agendamento.reagendar(novoInicio, CLIENTE_ID, AGORA));
		assertTransicaoInvalida(() -> agendamento.concluir(GESTOR_ID, DEPOIS_DO_INICIO));
		assertTransicaoInvalida(() -> agendamento.registrarNaoComparecimento(GESTOR_ID, DEPOIS_DO_INICIO));
		assertThat(agendamento.status()).isEqualTo(status);
		assertThat(agendamento.alteracoesPendentes()).isEmpty();
	}

	@Test
	void transicaoInvalidaEhRegraDeNegocioEInformaOStatus() {
		Agendamento agendamento = comStatus(StatusAgendamento.CANCELADO);

		assertThatThrownBy(() -> agendamento.cancelar(null, CLIENTE_ID, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.isInstanceOfSatisfying(TransicaoInvalidaException.class,
						erro -> assertThat(erro.statusAtual()).isEqualTo(StatusAgendamento.CANCELADO))
				.hasMessage("não é possível cancelar um agendamento CANCELADO");
	}

	private static void assertTransicaoInvalida(ThrowingCallable operacao) {
		assertThatThrownBy(operacao).isInstanceOf(TransicaoInvalidaException.class);
	}

	// Reconstituição

	@Test
	void reconstituiSemAlteracoesPendentes() {
		Agendamento agendamento = comStatus(StatusAgendamento.CONFIRMADO);

		assertThat(agendamento.status()).isEqualTo(StatusAgendamento.CONFIRMADO);
		assertThat(agendamento.fim()).isEqualTo(FIM);
		assertThat(agendamento.duracaoMinutos()).isEqualTo(60);
		assertThat(agendamento.versao()).isEqualTo(3L);
		assertThat(agendamento.alteracoesPendentes()).isEmpty();
	}

	@Test
	void reconstituiAgendamentoPassadoSemValidarOFuturo() {
		Instant ontem = AGORA.minus(Duration.ofDays(1));

		Agendamento agendamento = Agendamento.reconstituir(UUID.randomUUID(), CLIENTE_ID, ESTABELECIMENTO_ID,
				PROFISSIONAL_ID, SERVICO_ID, ontem, ontem.plusSeconds(1800), StatusAgendamento.CONCLUIDO,
				BigDecimal.TEN, null, 0L);

		assertThat(agendamento.inicio()).isEqualTo(ontem);
		assertThat(agendamento.duracaoMinutos()).isEqualTo(30);
	}

	@Test
	void recusaReconstituirComFimAntesDoInicioOuSemStatus() {
		assertThatThrownBy(() -> Agendamento.reconstituir(UUID.randomUUID(), CLIENTE_ID, ESTABELECIMENTO_ID,
				PROFISSIONAL_ID, SERVICO_ID, INICIO, INICIO, StatusAgendamento.AGENDADO, BigDecimal.TEN, null, 0L))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("fim deve ser posterior ao inicio");
		assertThatThrownBy(() -> Agendamento.reconstituir(UUID.randomUUID(), CLIENTE_ID, ESTABELECIMENTO_ID,
				PROFISSIONAL_ID, SERVICO_ID, INICIO, FIM, null, BigDecimal.TEN, null, 0L))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("status é obrigatório");
	}

	@Test
	void alteracoesPendentesNaoPodemSerModificadasDeFora() {
		Agendamento agendamento = reservado();

		assertThatThrownBy(() -> agendamento.alteracoesPendentes().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

}
