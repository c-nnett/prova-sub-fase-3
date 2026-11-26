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
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.fiap.agendabeleza.api.agendamentos.application.AgendamentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.AlteracaoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.TransicaoInvalidaException;
import br.com.fiap.agendabeleza.api.compartilhado.application.ExecucaoSerializadaDireta;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.EstabelecimentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.ServicoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.BloqueioAgendaRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.application.ProfissionalRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;
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
	/** Segunda, 14h no horário de Brasília. */
	private static final Instant CATORZE_HORAS = Instant.parse("2030-03-04T17:00:00Z");
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
	private final ReagendarAgendamentoUseCase reagendar = new ReagendarAgendamentoUseCase(agendamentos,
			profissionais, estabelecimentos, bloqueios, execucao, relogio);

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

	private Agendamento reagendar(Agendamento agendamento, UUID usuarioSolicitante, Instant novoInicio) {
		return reagendar.executar(new ReagendarAgendamentoUseCase.Comando(agendamento.id(), usuarioSolicitante,
				novoInicio));
	}

	private void assertReagendamentoRecusado(Agendamento agendamento, Instant novoInicio,
			Class<? extends RuntimeException> excecaoEsperada, String mensagemEsperada) {
		assertThatThrownBy(() -> reagendar(agendamento, CLIENTE_ID, novoInicio))
				.isInstanceOf(excecaoEsperada)
				.hasMessage(mensagemEsperada);
		assertThat(agendamentos.buscarPorId(agendamento.id()).orElseThrow().inicio())
				.isEqualTo(agendamento.inicio());
	}

	private static Clock relogioEm(Instant instante) {
		return Clock.fixed(instante, ZoneOffset.UTC);
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

	// Reagendar

	@Test
	void clienteReagendaAgendamentoConfirmadoParaHorarioLivre() {
		Agendamento confirmado = confirmar(reservar(DEZ_HORAS), GESTOR_ID);

		Agendamento reagendado = reagendar(confirmado, CLIENTE_ID, CATORZE_HORAS);

		assertThat(reagendado.inicio()).isEqualTo(CATORZE_HORAS);
		assertThat(reagendado.fim()).isEqualTo(CATORZE_HORAS.plusSeconds(3600));
		assertThat(reagendado.status()).isEqualTo(StatusAgendamento.AGENDADO);
		assertThat(ultimaAlteracao()).satisfies(alteracao -> {
			assertThat(alteracao.statusAnterior()).isEqualTo(StatusAgendamento.CONFIRMADO);
			assertThat(alteracao.statusNovo()).isEqualTo(StatusAgendamento.AGENDADO);
			assertThat(alteracao.inicioAnterior()).isEqualTo(DEZ_HORAS);
			assertThat(alteracao.inicioNovo()).isEqualTo(CATORZE_HORAS);
			assertThat(alteracao.alteradoPor()).isEqualTo(CLIENTE_ID);
		});
	}

	@Test
	void reagendamentoRodaSerializadoPeloProfissional() {
		Agendamento reservado = reservar(DEZ_HORAS);

		reagendar(reservado, GESTOR_ID, CATORZE_HORAS);

		assertThat(execucao.chaves()).containsExactly(ana.id(), ana.id());
	}

	@Test
	void aceitaNovoHorarioQueSobrepoeOProprioHorarioAntigo() {
		Agendamento reservado = reservar(DEZ_HORAS);
		Instant meiaHoraDepois = DEZ_HORAS.plusSeconds(1800);

		assertThat(reagendar(reservado, CLIENTE_ID, meiaHoraDepois).inicio()).isEqualTo(meiaHoraDepois);
	}

	@Test
	void recusaHorarioDeOutroAgendamentoComConflito() {
		Agendamento reservado = reservar(DEZ_HORAS);
		reservar(CATORZE_HORAS);

		assertReagendamentoRecusado(reservado, CATORZE_HORAS.plusSeconds(1800), HorarioIndisponivelException.class,
				"O profissional já tem um agendamento nesse horário");
	}

	@Test
	void recusaHorarioEmBloqueioDoProfissional() {
		Agendamento reservado = reservar(DEZ_HORAS);
		bloqueios.salvar(BloqueioAgenda.reconstituir(UUID.randomUUID(), ana.id(), CATORZE_HORAS,
				CATORZE_HORAS.plusSeconds(3600), "Curso"));

		assertReagendamentoRecusado(reservado, CATORZE_HORAS, RegraNegocioException.class,
				DisponibilidadeDaAgenda.PROFISSIONAL_INDISPONIVEL);
	}

	@Test
	void recusaHorarioDeAlmoco() {
		Agendamento reservado = reservar(DEZ_HORAS);

		assertReagendamentoRecusado(reservado, Instant.parse("2030-03-04T15:30:00Z"), RegraNegocioException.class,
				DisponibilidadeDaAgenda.FORA_DO_ATENDIMENTO);
	}

	@Test
	void recusaOMesmoInicio() {
		Agendamento reservado = reservar(DEZ_HORAS);

		assertReagendamentoRecusado(reservado, DEZ_HORAS, RegraNegocioException.class,
				"O novo início deve ser diferente do atual");
	}

	@Test
	void recusaInicioNoPassado() {
		Agendamento reservado = reservar(DEZ_HORAS);

		assertReagendamentoRecusado(reservado, AGORA.minusSeconds(3600), RegraNegocioException.class,
				"inicio deve estar no futuro");
	}

	@Test
	void recusaReagendarComProfissionalInativo() {
		Agendamento reservado = reservar(DEZ_HORAS);
		ana.atualizarPerfil(ana.nome(), ana.email(), null, false);
		profissionais.salvar(ana);

		assertReagendamentoRecusado(reservado, CATORZE_HORAS, RegraNegocioException.class,
				"O profissional está inativo");
	}

	@Test
	void recusaReagendarAgendamentoCancelado() {
		Agendamento cancelado = cancelar(reservar(DEZ_HORAS), CLIENTE_ID, null);

		assertReagendamentoRecusado(cancelado, CATORZE_HORAS, TransicaoInvalidaException.class,
				"Não é possível reagendar um agendamento CANCELADO");
	}

	@Test
	void profissionalNaoReagenda() {
		Agendamento reservado = reservar(DEZ_HORAS);

		assertThatThrownBy(() -> reagendar(reservado, USUARIO_PROFISSIONAL_ID, CATORZE_HORAS))
				.isInstanceOf(AcessoNegadoException.class)
				.hasMessage("Somente o cliente ou o gestor do estabelecimento podem reagendar este agendamento");
	}

	// Concluir e registrar o não comparecimento: mesmas regras, destinos diferentes

	@ParameterizedTest
	@EnumSource(Encerramento.class)
	void gestorEProfissionalEncerramDepoisDoInicio(Encerramento encerramento) {
		Map<UUID, Instant> inicioPorUsuario = Map.of(
				GESTOR_ID, DEZ_HORAS,
				USUARIO_PROFISSIONAL_ID, CATORZE_HORAS);

		for (Map.Entry<UUID, Instant> usuarioEInicio : inicioPorUsuario.entrySet()) {
			UUID usuarioComPermissao = usuarioEInicio.getKey();
			Instant durante = usuarioEInicio.getValue().plusSeconds(1800);
			Agendamento confirmado = confirmar(reservar(usuarioEInicio.getValue()), GESTOR_ID);

			Agendamento encerrado = encerrar(encerramento, confirmado, usuarioComPermissao, durante);

			assertThat(encerrado.status()).isEqualTo(encerramento.statusFinal);
			assertThat(ultimaAlteracao()).satisfies(alteracao -> {
				assertThat(alteracao.statusAnterior()).isEqualTo(StatusAgendamento.CONFIRMADO);
				assertThat(alteracao.statusNovo()).isEqualTo(encerramento.statusFinal);
				assertThat(alteracao.alteradoPor()).isEqualTo(usuarioComPermissao);
				assertThat(alteracao.ocorridoEm()).isEqualTo(durante);
			});
		}
	}

	@ParameterizedTest
	@EnumSource(Encerramento.class)
	void recusaEncerrarAntesDoInicio(Encerramento encerramento) {
		Agendamento confirmado = confirmar(reservar(DEZ_HORAS), GESTOR_ID);

		assertThatThrownBy(() -> encerrar(encerramento, confirmado, GESTOR_ID, DEZ_HORAS.minusSeconds(60)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(encerramento.mensagemAntesDoInicio);
	}

	@ParameterizedTest
	@EnumSource(Encerramento.class)
	void recusaEncerrarAgendamentoNaoConfirmado(Encerramento encerramento) {
		Agendamento reservado = reservar(DEZ_HORAS);

		assertThatThrownBy(() -> encerrar(encerramento, reservado, GESTOR_ID, DEZ_HORAS.plusSeconds(1800)))
				.isInstanceOf(TransicaoInvalidaException.class)
				.hasMessage("Não é possível " + encerramento.verboDaTransicao + " um agendamento AGENDADO");
	}

	@ParameterizedTest
	@EnumSource(Encerramento.class)
	void clienteNaoEncerra(Encerramento encerramento) {
		Agendamento confirmado = confirmar(reservar(DEZ_HORAS), GESTOR_ID);

		assertThatThrownBy(() -> encerrar(encerramento, confirmado, CLIENTE_ID, DEZ_HORAS.plusSeconds(1800)))
				.isInstanceOf(AcessoNegadoException.class)
				.hasMessage("Somente o gestor do estabelecimento ou o profissional podem "
						+ encerramento.acaoNaMensagemDePermissao);
	}

	private Agendamento encerrar(Encerramento encerramento, Agendamento agendamento, UUID usuarioSolicitante,
			Instant agora) {
		return encerramento.fabrica.criar(agendamentos, profissionais, estabelecimentos, relogioEm(agora))
				.executar(new ComandoDeAlteracao.Simples(agendamento.id(), usuarioSolicitante));
	}

	/** Cria o caso de uso de encerramento com as dependências do teste. */
	@FunctionalInterface
	private interface FabricaDeEncerramento {
		AlteracaoDeAgendamentoUseCase<ComandoDeAlteracao.Simples> criar(AgendamentoRepository agendamentos,
				ProfissionalRepository profissionais, EstabelecimentoRepository estabelecimentos, Clock relogio);
	}

	/** As duas formas de encerrar um atendimento confirmado, a partir do início. */
	enum Encerramento {

		CONCLUSAO(ConcluirAgendamentoUseCase::new, StatusAgendamento.CONCLUIDO,
				"O atendimento só pode ser concluído depois do início",
				"concluir", "concluir este agendamento"),
		NAO_COMPARECIMENTO(RegistrarNaoComparecimentoUseCase::new, StatusAgendamento.NAO_COMPARECEU,
				"O não comparecimento só pode ser registrado depois do início",
				"registrar o não comparecimento de", "registrar o não comparecimento deste agendamento");

		private final FabricaDeEncerramento fabrica;
		private final StatusAgendamento statusFinal;
		private final String mensagemAntesDoInicio;
		private final String verboDaTransicao;
		private final String acaoNaMensagemDePermissao;

		Encerramento(FabricaDeEncerramento fabrica, StatusAgendamento statusFinal, String mensagemAntesDoInicio,
				String verboDaTransicao, String acaoNaMensagemDePermissao) {
			this.fabrica = fabrica;
			this.statusFinal = statusFinal;
			this.mensagemAntesDoInicio = mensagemAntesDoInicio;
			this.verboDaTransicao = verboDaTransicao;
			this.acaoNaMensagemDePermissao = acaoNaMensagemDePermissao;
		}

	}

}
