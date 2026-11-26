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
import br.com.fiap.agendabeleza.api.agendamentos.application.AvisosDoAgendamentoEmMemoria;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisoDoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.application.ExecucaoSerializadaDireta;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
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
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

/**
 * Estabelecimento em America/Sao_Paulo (UTC-3), aberto às segundas das 8h às 20h. A profissional trabalha das 9h
 * às 12h e das 13h às 18h. Hoje é sexta, 01/03/2030, e as reservas são para segunda, 04/03/2030.
 */
class AgendamentoUseCasesTest {

	private static final Instant AGORA = Instant.parse("2030-03-01T12:00:00Z");
	/** Segunda, 10h no horário de Brasília. */
	private static final Instant DEZ_HORAS = Instant.parse("2030-03-04T13:00:00Z");
	private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID OUTRO_CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
	private static final String EMAIL_CLIENTE = "cliente@email.com";

	private final AgendamentoRepositoryEmMemoria agendamentos = new AgendamentoRepositoryEmMemoria();
	private final ProfissionalRepositoryEmMemoria profissionais = new ProfissionalRepositoryEmMemoria();
	private final ServicoRepositoryEmMemoria servicos = new ServicoRepositoryEmMemoria();
	private final EstabelecimentoRepositoryEmMemoria estabelecimentos = new EstabelecimentoRepositoryEmMemoria();
	private final BloqueioAgendaRepositoryEmMemoria bloqueios = new BloqueioAgendaRepositoryEmMemoria();
	private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
	private final ExecucaoSerializadaDireta execucao = new ExecucaoSerializadaDireta();
	private final AvisosDoAgendamentoEmMemoria avisos = new AvisosDoAgendamentoEmMemoria(execucao);

	private final CriarAgendamentoUseCase criar = new CriarAgendamentoUseCase(agendamentos, profissionais, servicos,
			estabelecimentos, bloqueios, execucao, avisos, relogio);
	private final AgendamentosAtivosDoProfissional agendamentosAtivos =
			new AgendamentosAtivosDoProfissional(agendamentos);
	private final ListarMeusAgendamentosUseCase listarMeus = new ListarMeusAgendamentosUseCase(agendamentos);
	private final ConsultarAgendamentoUseCase consultar = new ConsultarAgendamentoUseCase(agendamentos,
			profissionais, estabelecimentos);

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
		return reservar(CLIENTE_ID, inicio);
	}

	private Agendamento reservar(UUID clienteId, Instant inicio) {
		return criar.executar(new CriarAgendamentoUseCase.Comando(clienteId, EMAIL_CLIENTE, ana.id(), corte.id(),
				inicio, "Primeira vez"));
	}

	/** Reserva recusada não grava o agendamento nem pede aviso. */
	private void assertRecusa(Instant inicio, String mensagem) {
		assertThatThrownBy(() -> reservar(inicio))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(mensagem);
		assertThat(agendamentos.total()).isZero();
		assertThat(avisos.registrados()).isEmpty();
	}

	// Criar

	@Test
	void criaComATarifaDoProfissionalEOFimPelaDuracao() {
		Agendamento criado = reservar(DEZ_HORAS);

		assertThat(criado.clienteId()).isEqualTo(CLIENTE_ID);
		assertThat(criado.estabelecimentoId()).isEqualTo(estabelecimento.id());
		assertThat(criado.profissionalId()).isEqualTo(ana.id());
		assertThat(criado.servicoId()).isEqualTo(corte.id());
		assertThat(criado.inicio()).isEqualTo(DEZ_HORAS);
		assertThat(criado.fim()).isEqualTo(Instant.parse("2030-03-04T14:00:00Z"));
		assertThat(criado.preco()).isEqualTo(new BigDecimal("95.00"));
		assertThat(criado.observacoes()).isEqualTo("Primeira vez");
		assertThat(criado.status()).isEqualTo(StatusAgendamento.AGENDADO);
		assertThat(criado.versao()).isZero();
		assertThat(agendamentos.historico()).singleElement().satisfies(alteracao -> {
			assertThat(alteracao.statusNovo()).isEqualTo(StatusAgendamento.AGENDADO);
			assertThat(alteracao.alteradoPor()).isEqualTo(CLIENTE_ID);
			assertThat(alteracao.ocorridoEm()).isEqualTo(AGORA);
		});
	}

	@Test
	void reservaRodaSerializadaPeloProfissional() {
		reservar(DEZ_HORAS);

		assertThat(execucao.chaves()).containsExactly(ana.id());
	}

	@Test
	void reservaAvisaClienteEProfissionalDentroDaExecucaoSerializada() {
		Agendamento criado = reservar(DEZ_HORAS);

		assertThat(avisos.registrados()).singleElement().satisfies(registrado -> {
			assertThat(registrado.ocorrencia()).isEqualTo(AvisosDoAgendamentoEmMemoria.Ocorrencia.RESERVADO);
			assertThat(registrado.aviso()).isEqualTo(AvisoDoAgendamento.de(criado, ana, estabelecimento));
			assertThat(registrado.emailCliente()).isEqualTo(EMAIL_CLIENTE);
			assertThat(registrado.dentroDaExecucao()).isTrue();
		});
	}

	@Test
	void reservaEmHorarioOcupadoNaoAvisa() {
		reservar(DEZ_HORAS);

		assertThatThrownBy(() -> reservar(OUTRO_CLIENTE_ID, DEZ_HORAS))
				.isInstanceOf(HorarioIndisponivelException.class);
		assertThat(avisos.registrados()).hasSize(1);
	}

	@Test
	void recusaProfissionalInexistente() {
		UUID inexistente = UUID.randomUUID();

		assertThatThrownBy(() -> criar.executar(new CriarAgendamentoUseCase.Comando(CLIENTE_ID, EMAIL_CLIENTE,
				inexistente, corte.id(), DEZ_HORAS, null)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Profissional " + inexistente + " não encontrado");
	}

	@Test
	void recusaServicoInexistente() {
		UUID inexistente = UUID.randomUUID();

		assertThatThrownBy(() -> criar.executar(new CriarAgendamentoUseCase.Comando(CLIENTE_ID, EMAIL_CLIENTE,
				ana.id(), inexistente, DEZ_HORAS, null)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Serviço " + inexistente + " não encontrado");
	}

	@Test
	void recusaProfissionalInativo() {
		ana.atualizarPerfil(ana.nome(), ana.email(), null, false);

		assertRecusa(DEZ_HORAS, "O profissional está inativo");
	}

	@Test
	void recusaServicoInativo() {
		corte.atualizar(corte.nome(), null, corte.categoria(), 60, corte.precoBase(), false);

		assertRecusa(DEZ_HORAS, "O serviço está inativo");
	}

	@Test
	void recusaServicoDeOutroEstabelecimento() {
		Servico deOutro = servicos.salvar(Servico.cadastrar(UUID.randomUUID(), "Corte", null,
				CategoriaServico.CABELO, 60, BigDecimal.TEN));

		assertThatThrownBy(() -> criar.executar(new CriarAgendamentoUseCase.Comando(CLIENTE_ID, EMAIL_CLIENTE,
				ana.id(), deOutro.id(), DEZ_HORAS, null)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O serviço não pertence ao estabelecimento do profissional");
	}

	@Test
	void recusaServicoQueOProfissionalNaoExecuta() {
		Servico escova = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Escova", null,
				CategoriaServico.CABELO, 30, BigDecimal.TEN));

		assertThatThrownBy(() -> criar.executar(new CriarAgendamentoUseCase.Comando(CLIENTE_ID, EMAIL_CLIENTE,
				ana.id(), escova.id(), DEZ_HORAS, null)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O profissional não executa o serviço");
	}

	@Test
	void recusaEstabelecimentoInativo() {
		estabelecimentos.salvar(Estabelecimento.reconstituir(estabelecimento.id(), GESTOR_ID, "Salão Bela Vista",
				null, endereco(), null, null, 0, false, List.of(), estabelecimento.horarios()));

		assertRecusa(DEZ_HORAS, "O estabelecimento está inativo");
	}

	@Test
	void recusaInicioNoPassado() {
		assertRecusa(AGORA.minusSeconds(3600), "inicio deve estar no futuro");
	}

	@Test
	void recusaHorarioDeAlmoco() {
		assertRecusa(Instant.parse("2030-03-04T15:30:00Z"), DisponibilidadeDaAgenda.FORA_DO_ATENDIMENTO);
	}

	@Test
	void recusaServicoQueTerminaDepoisDaJornada() {
		assertRecusa(Instant.parse("2030-03-04T20:30:00Z"), DisponibilidadeDaAgenda.FORA_DO_ATENDIMENTO);
	}

	@Test
	void recusaDiaComOEstabelecimentoFechado() {
		assertRecusa(Instant.parse("2030-03-03T13:00:00Z"), DisponibilidadeDaAgenda.FORA_DO_ATENDIMENTO);
	}

	@Test
	void usaOFusoDoEstabelecimento() {
		// 9h em UTC são 6h em Brasília, antes da jornada. 12h em UTC são 9h em Brasília, o início da jornada.
		assertRecusa(Instant.parse("2030-03-04T09:00:00Z"), DisponibilidadeDaAgenda.FORA_DO_ATENDIMENTO);

		assertThat(reservar(Instant.parse("2030-03-04T12:00:00Z")).inicio())
				.isEqualTo(Instant.parse("2030-03-04T12:00:00Z"));
	}

	@Test
	void recusaHorarioOcupadoComConflito() {
		reservar(DEZ_HORAS);

		assertThatThrownBy(() -> reservar(OUTRO_CLIENTE_ID, DEZ_HORAS.plusSeconds(1800)))
				.isInstanceOf(HorarioIndisponivelException.class)
				.hasMessage("O profissional já tem um agendamento nesse horário");
		assertThat(agendamentos.total()).isOne();
	}

	@Test
	void aceitaHorarioQueComecaQuandoOAnteriorTermina() {
		Agendamento primeiro = reservar(DEZ_HORAS);

		Agendamento seguinte = reservar(OUTRO_CLIENTE_ID, primeiro.fim());

		assertThat(seguinte.inicio()).isEqualTo(primeiro.fim());
		assertThat(agendamentos.total()).isEqualTo(2);
	}

	@Test
	void horarioCanceladoVoltaAFicarLivre() {
		Agendamento primeiro = reservar(DEZ_HORAS);
		Agendamento lido = agendamentos.buscarPorId(primeiro.id()).orElseThrow();
		lido.cancelar(null, CLIENTE_ID, AGORA);
		agendamentos.salvar(lido);

		Agendamento novo = reservar(OUTRO_CLIENTE_ID, DEZ_HORAS);

		assertThat(novo.status()).isEqualTo(StatusAgendamento.AGENDADO);
	}

	@Test
	void recusaReservaSobreBloqueioDoProfissional() {
		bloqueios.salvar(BloqueioAgenda.reconstituir(UUID.randomUUID(), ana.id(), DEZ_HORAS.minusSeconds(1800),
				DEZ_HORAS.plusSeconds(1800), "Consulta médica"));

		assertRecusa(DEZ_HORAS, DisponibilidadeDaAgenda.PROFISSIONAL_INDISPONIVEL);
	}

	@Test
	void aceitaReservaQueTerminaQuandoOBloqueioComeca() {
		bloqueios.salvar(BloqueioAgenda.reconstituir(UUID.randomUUID(), ana.id(), DEZ_HORAS.plusSeconds(3600),
				DEZ_HORAS.plusSeconds(7200), null));

		assertThat(reservar(DEZ_HORAS).status()).isEqualTo(StatusAgendamento.AGENDADO);
	}

	@Test
	void ignoraBloqueioDeOutroProfissional() {
		bloqueios.salvar(BloqueioAgenda.reconstituir(UUID.randomUUID(), UUID.randomUUID(), DEZ_HORAS,
				DEZ_HORAS.plusSeconds(3600), null));

		assertThat(reservar(DEZ_HORAS).status()).isEqualTo(StatusAgendamento.AGENDADO);
	}

	// Agendamentos ativos, consultados pelo módulo de profissionais ao criar um bloqueio

	@Test
	void informaAgendamentoAtivoQueCruzaOPeriodo() {
		Agendamento criado = reservar(DEZ_HORAS);
		Intervalo cruzando = new Intervalo(criado.fim().minusSeconds(60), criado.fim().plusSeconds(3600));
		Intervalo encostado = new Intervalo(criado.fim(), criado.fim().plusSeconds(3600));

		assertThat(agendamentosAtivos.existemNoPeriodo(ana.id(), cruzando)).isTrue();
		assertThat(agendamentosAtivos.existemNoPeriodo(UUID.randomUUID(), cruzando)).isFalse();
		assertThat(agendamentosAtivos.existemNoPeriodo(ana.id(), encostado)).isFalse();
	}

	@Test
	void agendamentoCanceladoNaoContaComoAtivo() {
		Agendamento lido = agendamentos.buscarPorId(reservar(DEZ_HORAS).id()).orElseThrow();
		lido.cancelar(null, CLIENTE_ID, AGORA);
		agendamentos.salvar(lido);

		assertThat(agendamentosAtivos.existemNoPeriodo(ana.id(), lido.intervalo())).isFalse();
	}

	// Listar e consultar

	@Test
	void listaSomenteOsAgendamentosDoCliente() {
		Agendamento cedo = reservar(DEZ_HORAS);
		Agendamento tarde = reservar(Instant.parse("2030-03-04T17:00:00Z"));
		reservar(OUTRO_CLIENTE_ID, Instant.parse("2030-03-04T19:00:00Z"));

		assertThat(listarMeus.executar(CLIENTE_ID))
				.extracting(Agendamento::id)
				.containsExactly(tarde.id(), cedo.id());
	}

	@Test
	void clienteGestorEProfissionalConsultamOAgendamento() {
		Agendamento criado = reservar(DEZ_HORAS);

		assertThat(consultar.executar(criado.id(), CLIENTE_ID).id()).isEqualTo(criado.id());
		assertThat(consultar.executar(criado.id(), GESTOR_ID).id()).isEqualTo(criado.id());
		assertThat(consultar.executar(criado.id(), USUARIO_PROFISSIONAL_ID).id()).isEqualTo(criado.id());
	}

	@Test
	void terceiroNaoConsultaOAgendamento() {
		Agendamento criado = reservar(DEZ_HORAS);

		assertThatThrownBy(() -> consultar.executar(criado.id(), OUTRO_CLIENTE_ID))
				.isInstanceOf(AcessoNegadoException.class)
				.hasMessage("Somente o cliente, o gestor do estabelecimento ou o profissional podem consultar "
						+ "este agendamento");
	}

	@Test
	void consultaDeAgendamentoInexistenteDevolve404() {
		UUID inexistente = UUID.randomUUID();

		assertThatThrownBy(() -> consultar.executar(inexistente, CLIENTE_ID))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Agendamento " + inexistente + " não encontrado");
	}

}
