package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.USUARIO_PROFISSIONAL_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.agendamentos.application.AgendamentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.AlteracaoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.TransicaoInvalidaException;
import br.com.fiap.agendabeleza.api.compartilhado.application.ExecucaoSerializadaDireta;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.EstabelecimentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.ServicoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.BloqueioAgendaRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.application.ProfissionalRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

/**
 * Ações de gestão sobre um agendamento já reservado: confirmar, cancelar, reagendar, concluir e registrar o não
 * comparecimento.
 * <p>
 * Mesmo cenário de {@link AgendamentoUseCasesTest}: estabelecimento em America/Sao_Paulo (UTC-3), aberto às
 * segundas das 8h às 20h, e a profissional trabalha das 9h às 12h e das 13h às 18h. Hoje é sexta, 01/03/2030, e as
 * reservas são para segunda, 04/03/2030.
 */
class GestaoAgendamentoUseCasesTest {

	private static final Instant AGORA = Instant.parse("2030-03-01T12:00:00Z");
	/** Segunda, 10h no horário de Brasília. */
	private static final Instant DEZ_HORAS = Instant.parse("2030-03-04T13:00:00Z");
	private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID OUTRO_CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

	private final AgendamentoRepositoryEmMemoria agendamentos = new AgendamentoRepositoryEmMemoria();
	private final ProfissionalRepositoryEmMemoria profissionais = new ProfissionalRepositoryEmMemoria();
	private final ServicoRepositoryEmMemoria servicos = new ServicoRepositoryEmMemoria();
	private final EstabelecimentoRepositoryEmMemoria estabelecimentos = new EstabelecimentoRepositoryEmMemoria();
	private final BloqueioAgendaRepositoryEmMemoria bloqueios = new BloqueioAgendaRepositoryEmMemoria();
	private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
	private final ExecucaoSerializadaDireta execucao = new ExecucaoSerializadaDireta();

	private final CriarAgendamentoUseCase criar = new CriarAgendamentoUseCase(agendamentos, profissionais, servicos,
			estabelecimentos, bloqueios, execucao, relogio);
	private final ConfirmarAgendamentoUseCase confirmar = new ConfirmarAgendamentoUseCase(agendamentos,
			profissionais, estabelecimentos, relogio);
	private final CancelarAgendamentoUseCase cancelar = new CancelarAgendamentoUseCase(agendamentos, profissionais,
			estabelecimentos, relogio);

	private Estabelecimento estabelecimento;
	private Servico corte;
	private Profissional ana;

	@BeforeEach
	void prepararCadastros() {
		estabelecimento = Estabelecimento.cadastrar(GESTOR_ID, "Salão Bela Vista", null, endereco(), null);
		estabelecimento.definirHorarios(List.of(
				new HorarioFuncionamento(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(20, 0))));
		estabelecimentos.salvar(estabelecimento);
		corte = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Corte", null, CategoriaServico.CABELO, 60,
				new BigDecimal("80.00")));
		ana = Profissional.cadastrar(estabelecimento.id(), USUARIO_PROFISSIONAL_ID, "Ana", "ana@salao.com", null);
		ana.definirServicos(List.of(new ServicoProfissional(corte.id(), new BigDecimal("95.00"))));
		ana.definirJornada(List.of(
				new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
				new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(18, 0))));
		profissionais.salvar(ana);
	}

	private Agendamento reservar(Instant inicio) {
		return criar.executar(new CriarAgendamentoUseCase.Comando(CLIENTE_ID, ana.id(), corte.id(), inicio, null));
	}

	private Agendamento confirmar(Agendamento agendamento, UUID usuarioSolicitante) {
		return confirmar.executar(new ComandoDeAlteracao.Simples(agendamento.id(), usuarioSolicitante));
	}

	private Agendamento cancelar(Agendamento agendamento, UUID usuarioSolicitante, String motivo) {
		return cancelar.executar(new CancelarAgendamentoUseCase.Comando(agendamento.id(), usuarioSolicitante,
				motivo));
	}

	private AlteracaoAgendamento ultimaAlteracao() {
		List<AlteracaoAgendamento> historico = agendamentos.historico();
		return historico.get(historico.size() - 1);
	}

	// Confirmar

	@Test
	void gestorConfirmaOAgendamentoEGravaOHistorico() {
		Agendamento reservado = reservar(DEZ_HORAS);

		Agendamento confirmado = confirmar(reservado, GESTOR_ID);

		assertThat(confirmado.status()).isEqualTo(StatusAgendamento.CONFIRMADO);
		assertThat(confirmado.versao()).isEqualTo(reservado.versao() + 1);
		assertThat(agendamentos.buscarPorId(reservado.id()).orElseThrow().status())
				.isEqualTo(StatusAgendamento.CONFIRMADO);
		assertThat(ultimaAlteracao()).satisfies(alteracao -> {
			assertThat(alteracao.statusAnterior()).isEqualTo(StatusAgendamento.AGENDADO);
			assertThat(alteracao.statusNovo()).isEqualTo(StatusAgendamento.CONFIRMADO);
			assertThat(alteracao.alteradoPor()).isEqualTo(GESTOR_ID);
			assertThat(alteracao.ocorridoEm()).isEqualTo(AGORA);
		});
	}

	@Test
	void profissionalConfirmaOProprioAgendamento() {
		Agendamento confirmado = confirmar(reservar(DEZ_HORAS), USUARIO_PROFISSIONAL_ID);

		assertThat(confirmado.status()).isEqualTo(StatusAgendamento.CONFIRMADO);
		assertThat(ultimaAlteracao().alteradoPor()).isEqualTo(USUARIO_PROFISSIONAL_ID);
	}

	@Test
	void clienteETerceiroNaoConfirmam() {
		Agendamento reservado = reservar(DEZ_HORAS);

		for (UUID usuarioSemPermissao : List.of(CLIENTE_ID, OUTRO_CLIENTE_ID)) {
			assertThatThrownBy(() -> confirmar(reservado, usuarioSemPermissao))
					.isInstanceOf(AcessoNegadoException.class)
					.hasMessage("Somente o gestor do estabelecimento ou o profissional podem confirmar este "
							+ "agendamento");
		}
		assertThat(agendamentos.buscarPorId(reservado.id()).orElseThrow().status())
				.isEqualTo(StatusAgendamento.AGENDADO);
	}

	@Test
	void confirmarAgendamentoInexistenteDevolve404() {
		UUID inexistente = UUID.randomUUID();

		assertThatThrownBy(() -> confirmar.executar(new ComandoDeAlteracao.Simples(inexistente, GESTOR_ID)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Agendamento " + inexistente + " não encontrado");
	}

	@Test
	void recusaConfirmarAgendamentoJaConfirmado() {
		Agendamento confirmado = confirmar(reservar(DEZ_HORAS), GESTOR_ID);
		int alteracoesAntes = agendamentos.historico().size();

		assertThatThrownBy(() -> confirmar(confirmado, GESTOR_ID))
				.isInstanceOf(TransicaoInvalidaException.class)
				.hasMessage("Não é possível confirmar um agendamento CONFIRMADO");
		assertThat(agendamentos.historico()).hasSize(alteracoesAntes);
	}

	// Cancelar

	@Test
	void clienteCancelaComMotivoEGravaOHistorico() {
		Agendamento reservado = reservar(DEZ_HORAS);

		Agendamento cancelado = cancelar(reservado, CLIENTE_ID, "Imprevisto no trabalho");

		assertThat(cancelado.status()).isEqualTo(StatusAgendamento.CANCELADO);
		assertThat(ultimaAlteracao()).satisfies(alteracao -> {
			assertThat(alteracao.statusAnterior()).isEqualTo(StatusAgendamento.AGENDADO);
			assertThat(alteracao.statusNovo()).isEqualTo(StatusAgendamento.CANCELADO);
			assertThat(alteracao.motivo()).isEqualTo("Imprevisto no trabalho");
			assertThat(alteracao.alteradoPor()).isEqualTo(CLIENTE_ID);
		});
	}

	@Test
	void gestorCancelaAgendamentoConfirmadoSemMotivo() {
		Agendamento confirmado = confirmar(reservar(DEZ_HORAS), GESTOR_ID);

		Agendamento cancelado = cancelar(confirmado, GESTOR_ID, null);

		assertThat(cancelado.status()).isEqualTo(StatusAgendamento.CANCELADO);
		assertThat(ultimaAlteracao().motivo()).isNull();
	}

	@Test
	void cancelamentoLiberaOHorarioParaOutraReserva() {
		cancelar(reservar(DEZ_HORAS), CLIENTE_ID, null);

		assertThat(reservar(DEZ_HORAS).status()).isEqualTo(StatusAgendamento.AGENDADO);
	}

	@Test
	void profissionalETerceiroNaoCancelam() {
		Agendamento reservado = reservar(DEZ_HORAS);

		for (UUID usuarioSemPermissao : List.of(USUARIO_PROFISSIONAL_ID, OUTRO_CLIENTE_ID)) {
			assertThatThrownBy(() -> cancelar(reservado, usuarioSemPermissao, null))
					.isInstanceOf(AcessoNegadoException.class)
					.hasMessage("Somente o cliente ou o gestor do estabelecimento podem cancelar este agendamento");
		}
	}

	@Test
	void recusaMotivoLongoDemais() {
		Agendamento reservado = reservar(DEZ_HORAS);

		assertThatThrownBy(() -> cancelar(reservado, CLIENTE_ID, "x".repeat(251)))
				.isInstanceOf(RegraNegocioException.class);
		assertThat(agendamentos.buscarPorId(reservado.id()).orElseThrow().status())
				.isEqualTo(StatusAgendamento.AGENDADO);
	}

	@Test
	void recusaCancelarAgendamentoJaCancelado() {
		Agendamento cancelado = cancelar(reservar(DEZ_HORAS), CLIENTE_ID, null);

		assertThatThrownBy(() -> cancelar(cancelado, CLIENTE_ID, null))
				.isInstanceOf(TransicaoInvalidaException.class)
				.hasMessage("Não é possível cancelar um agendamento CANCELADO");
	}

}
