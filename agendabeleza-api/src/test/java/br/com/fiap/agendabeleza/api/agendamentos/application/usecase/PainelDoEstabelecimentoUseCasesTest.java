package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.OUTRO_USUARIO_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.agendamentos.application.AgendamentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.ResumoDaAgenda;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.EstabelecimentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Painel do estabelecimento: agenda filtrada e resumo do período.
 * <p>
 * Estabelecimento em America/Sao_Paulo (UTC-3). Agora é segunda, 04/03/2030, às 9h no horário de Brasília. O dia de
 * hoje no fuso do estabelecimento vai de 2030-03-04T03:00Z (inclusivo) a 2030-03-05T03:00Z (exclusivo).
 */
class PainelDoEstabelecimentoUseCasesTest {

	private static final Instant AGORA = Instant.parse("2030-03-04T12:00:00Z");
	private static final LocalDate HOJE = LocalDate.of(2030, 3, 4);
	private static final LocalDate AMANHA = HOJE.plusDays(1);
	private static final LocalDate ONTEM = HOJE.minusDays(1);

	/** Segunda, 0h no horário de Brasília: primeiro instante do dia. */
	private static final Instant MEIA_NOITE_DE_HOJE = Instant.parse("2030-03-04T03:00:00Z");
	/** Segunda, 10h no horário de Brasília. */
	private static final Instant DEZ_HORAS_DE_HOJE = Instant.parse("2030-03-04T13:00:00Z");
	/** Segunda, 23h30 no horário de Brasília (já é terça em UTC). */
	private static final Instant ULTIMA_MEIA_HORA_DE_HOJE = Instant.parse("2030-03-05T02:30:00Z");
	/** Terça, 0h no horário de Brasília: fora do dia de hoje. */
	private static final Instant MEIA_NOITE_DE_AMANHA = Instant.parse("2030-03-05T03:00:00Z");
	/** Domingo, 23h no horário de Brasília (já é segunda em UTC): fora do dia de hoje. */
	private static final Instant ONZE_DA_NOITE_DE_ONTEM = Instant.parse("2030-03-04T02:00:00Z");

	private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID ANA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
	private static final UUID BIA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f2");
	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

	private final AgendamentoRepositoryEmMemoria agendamentos = new AgendamentoRepositoryEmMemoria();
	private final EstabelecimentoRepositoryEmMemoria estabelecimentos = new EstabelecimentoRepositoryEmMemoria();
	private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);

	private final ConsultarAgendaDoEstabelecimentoUseCase consultarAgenda =
			new ConsultarAgendaDoEstabelecimentoUseCase(estabelecimentos, agendamentos, relogio);
	private final ResumirAgendaDoEstabelecimentoUseCase resumirAgenda =
			new ResumirAgendaDoEstabelecimentoUseCase(consultarAgenda);

	private Estabelecimento estabelecimento;
	private Agendamento meiaNoiteDeHojeDaAna;
	private Agendamento dezHorasCanceladoDaAna;
	private Agendamento ultimaMeiaHoraDeHojeDaBia;
	private Agendamento meiaNoiteDeAmanhaDaAna;
	private Agendamento onzeDaNoiteDeOntemDaAna;

	@BeforeEach
	void prepararAgenda() {
		estabelecimento = estabelecimentos.salvar(
				Estabelecimento.cadastrar(GESTOR_ID, "Salão Bela Vista", null, endereco(), null));
		Estabelecimento outroEstabelecimento = estabelecimentos.salvar(
				Estabelecimento.cadastrar(OUTRO_USUARIO_ID, "Outro Salão", null, endereco(), null));

		meiaNoiteDeHojeDaAna = gravar(estabelecimento, ANA_ID, MEIA_NOITE_DE_HOJE, StatusAgendamento.AGENDADO,
				"50.00");
		dezHorasCanceladoDaAna = gravar(estabelecimento, ANA_ID, DEZ_HORAS_DE_HOJE, StatusAgendamento.CANCELADO,
				"70.00");
		ultimaMeiaHoraDeHojeDaBia = gravar(estabelecimento, BIA_ID, ULTIMA_MEIA_HORA_DE_HOJE,
				StatusAgendamento.CONFIRMADO, "90.00");
		meiaNoiteDeAmanhaDaAna = gravar(estabelecimento, ANA_ID, MEIA_NOITE_DE_AMANHA, StatusAgendamento.AGENDADO,
				"50.00");
		onzeDaNoiteDeOntemDaAna = gravar(estabelecimento, ANA_ID, ONZE_DA_NOITE_DE_ONTEM,
				StatusAgendamento.CONCLUIDO, "100.00");
		gravar(outroEstabelecimento, UUID.randomUUID(), DEZ_HORAS_DE_HOJE, StatusAgendamento.AGENDADO, "40.00");
	}

	private Agendamento gravar(Estabelecimento dono, UUID profissionalId, Instant inicio, StatusAgendamento status,
			String preco) {
		return agendamentos.salvar(Agendamento.reconstituir(UUID.randomUUID(), CLIENTE_ID, dono.id(),
				profissionalId, SERVICO_ID, inicio, inicio.plus(Duration.ofMinutes(30)), status,
				new BigDecimal(preco), null, null));
	}

	private DadosDoPeriodo<List<Agendamento>> agenda(LocalDate de, LocalDate ate, Set<StatusAgendamento> status,
			UUID profissionalId) {
		return consultarAgenda.executar(new ConsultarAgendaDoEstabelecimentoUseCase.Consulta(estabelecimento.id(),
				GESTOR_ID, de, ate, status, profissionalId));
	}

	private List<UUID> idsDaAgenda(LocalDate de, LocalDate ate, Set<StatusAgendamento> status,
			UUID profissionalId) {
		return agenda(de, ate, status, profissionalId).dados().stream().map(Agendamento::id).toList();
	}

	private DadosDoPeriodo<ResumoDaAgenda> resumo(LocalDate de, LocalDate ate, UUID profissionalId) {
		return resumirAgenda.executar(new ResumirAgendaDoEstabelecimentoUseCase.Consulta(estabelecimento.id(),
				GESTOR_ID, de, ate, profissionalId));
	}

	// Período

	@Test
	void semDatasMostraOsAgendamentosDeHojeNoFusoDoEstabelecimentoEmOrdemDeInicio() {
		DadosDoPeriodo<List<Agendamento>> agendaDeHoje = agenda(null, null, Set.of(), null);

		assertThat(agendaDeHoje.dados()).extracting(Agendamento::id).containsExactly(
				meiaNoiteDeHojeDaAna.id(), dezHorasCanceladoDaAna.id(), ultimaMeiaHoraDeHojeDaBia.id());
		assertThat(agendaDeHoje.periodo().de()).isEqualTo(HOJE);
		assertThat(agendaDeHoje.periodo().ate()).isEqualTo(HOJE);
		assertThat(agendaDeHoje.periodo().fuso()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
	}

	@Test
	void somenteADataInicialMostraApenasEsseDia() {
		assertThat(idsDaAgenda(AMANHA, null, Set.of(), null)).containsExactly(meiaNoiteDeAmanhaDaAna.id());
	}

	@Test
	void somenteADataFinalMostraApenasEsseDia() {
		assertThat(idsDaAgenda(null, ONTEM, Set.of(), null)).containsExactly(onzeDaNoiteDeOntemDaAna.id());
	}

	@Test
	void periodoDeVariosDiasIncluiOPrimeiroEOUltimoDia() {
		assertThat(idsDaAgenda(ONTEM, AMANHA, Set.of(), null)).containsExactly(onzeDaNoiteDeOntemDaAna.id(),
				meiaNoiteDeHojeDaAna.id(), dezHorasCanceladoDaAna.id(), ultimaMeiaHoraDeHojeDaBia.id(),
				meiaNoiteDeAmanhaDaAna.id());
	}

	@Test
	void recusaDataFinalAnteriorAInicial() {
		assertThatThrownBy(() -> agenda(HOJE, ONTEM, Set.of(), null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(PeriodoDoPainel.PERIODO_INVERTIDO);
	}

	@Test
	void aceitaPeriodoDeExatamenteTrintaEUmDias() {
		assertThat(agenda(HOJE, HOJE.plusDays(30), Set.of(), null).dados()).isNotEmpty();
	}

	@Test
	void recusaPeriodoMaiorQueTrintaEUmDias() {
		assertThatThrownBy(() -> agenda(HOJE, HOJE.plusDays(31), Set.of(), null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(PeriodoDoPainel.PERIODO_LONGO_DEMAIS);
	}

	// Filtros

	@Test
	void filtraPorUmStatus() {
		assertThat(idsDaAgenda(HOJE, HOJE, Set.of(StatusAgendamento.CANCELADO), null))
				.containsExactly(dezHorasCanceladoDaAna.id());
	}

	@Test
	void filtraPorVariosStatus() {
		assertThat(idsDaAgenda(HOJE, HOJE, Set.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO), null))
				.containsExactly(meiaNoiteDeHojeDaAna.id(), ultimaMeiaHoraDeHojeDaBia.id());
	}

	@Test
	void statusNuloEquivaleATodosOsStatus() {
		assertThat(idsDaAgenda(HOJE, HOJE, null, null)).hasSize(3);
	}

	@Test
	void filtraPorProfissional() {
		assertThat(idsDaAgenda(HOJE, HOJE, Set.of(), BIA_ID)).containsExactly(ultimaMeiaHoraDeHojeDaBia.id());
	}

	@Test
	void combinaStatusEProfissional() {
		assertThat(idsDaAgenda(HOJE, HOJE, Set.of(StatusAgendamento.AGENDADO), ANA_ID))
				.containsExactly(meiaNoiteDeHojeDaAna.id());
	}

	@Test
	void profissionalDeOutroEstabelecimentoNaoTrazNada() {
		assertThat(idsDaAgenda(HOJE, HOJE, Set.of(), UUID.randomUUID())).isEmpty();
	}

	// Permissão

	@Test
	void somenteOGestorConsultaAAgenda() {
		assertThatThrownBy(() -> consultarAgenda.executar(new ConsultarAgendaDoEstabelecimentoUseCase.Consulta(
				estabelecimento.id(), OUTRO_USUARIO_ID, null, null, Set.of(), null)))
				.isInstanceOf(AcessoNegadoException.class)
				.hasMessage("Somente o gestor do estabelecimento pode consultar o painel");
	}

	@Test
	void somenteOGestorConsultaOResumo() {
		assertThatThrownBy(() -> resumirAgenda.executar(new ResumirAgendaDoEstabelecimentoUseCase.Consulta(
				estabelecimento.id(), OUTRO_USUARIO_ID, null, null, null)))
				.isInstanceOf(AcessoNegadoException.class);
	}

	@Test
	void estabelecimentoInexistenteDevolve404() {
		UUID estabelecimentoInexistente = UUID.randomUUID();

		assertThatThrownBy(() -> consultarAgenda.executar(new ConsultarAgendaDoEstabelecimentoUseCase.Consulta(
				estabelecimentoInexistente, GESTOR_ID, null, null, Set.of(), null)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessageContaining(estabelecimentoInexistente.toString());
	}

	// Resumo

	@Test
	void resumoDeHojeContaTodosOsStatusESomaAsReceitas() {
		DadosDoPeriodo<ResumoDaAgenda> resumoDeHoje = resumo(null, null, null);

		assertThat(resumoDeHoje.periodo().de()).isEqualTo(HOJE);
		assertThat(resumoDeHoje.periodo().ate()).isEqualTo(HOJE);
		assertThat(resumoDeHoje.dados().total()).isEqualTo(3);
		assertThat(resumoDeHoje.dados().porStatus())
				.containsEntry(StatusAgendamento.AGENDADO, 1L)
				.containsEntry(StatusAgendamento.CONFIRMADO, 1L)
				.containsEntry(StatusAgendamento.CANCELADO, 1L)
				.containsEntry(StatusAgendamento.CONCLUIDO, 0L);
		assertThat(resumoDeHoje.dados().receitaPrevista()).isEqualByComparingTo("140.00");
		assertThat(resumoDeHoje.dados().receitaRealizada()).isEqualByComparingTo("0");
	}

	@Test
	void resumoFiltraPorPeriodoEProfissional() {
		ResumoDaAgenda resumoDaAna = resumo(ONTEM, AMANHA, ANA_ID).dados();

		assertThat(resumoDaAna.total()).isEqualTo(4);
		assertThat(resumoDaAna.receitaRealizada()).isEqualByComparingTo("100.00");
		assertThat(resumoDaAna.receitaPrevista()).isEqualByComparingTo("100.00");
	}

	@Test
	void resumoAcompanhaAAgendaAtualizada() {
		gravar(estabelecimento, ANA_ID, DEZ_HORAS_DE_HOJE.plus(Duration.ofHours(2)), StatusAgendamento.CONCLUIDO,
				"60.00");

		ResumoDaAgenda resumoDeHoje = resumo(HOJE, HOJE, null).dados();

		assertThat(resumoDeHoje.total()).isEqualTo(4);
		assertThat(resumoDeHoje.porStatus()).containsEntry(StatusAgendamento.CONCLUIDO, 1L);
		assertThat(resumoDeHoje.receitaRealizada()).isEqualByComparingTo("60.00");
	}

}
