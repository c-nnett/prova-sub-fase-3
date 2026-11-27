package br.com.fiap.agendabeleza.api.agendamentos.adapters.out.persistence;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.FiltroDaAgenda;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.ConflitoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

/**
 * Persistência real no PostgreSQL do Testcontainers, com a restrição {@code agendamento_sem_conflito}. Sem
 * {@code @Transactional}: cada chamada ao adaptador faz commit.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AgendamentoPersistenciaTest {

	private static final Instant AGORA = Instant.parse("2030-03-04T09:00:00Z");
	private static final Instant DEZ_HORAS = Instant.parse("2030-03-05T13:00:00Z");

	@Autowired
	private AgendamentoRepository agendamentos;

	@Autowired
	private AgendamentoHistoricoJpaRepository historico;

	@Autowired
	private EstabelecimentoRepository estabelecimentos;

	@Autowired
	private ServicoRepository servicos;

	@Autowired
	private ProfissionalRepository profissionais;

	private Estabelecimento estabelecimento;
	private Servico corte;
	private Profissional ana;
	private Profissional bia;

	@BeforeEach
	void prepararCadastros() {
		estabelecimento = estabelecimentos.salvar(Estabelecimento.cadastrar(
				UUID.randomUUID(), "Salão Bela Vista", null, endereco(), null));
		corte = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Corte", null, CategoriaServico.CABELO, 60,
				new BigDecimal("80.00")));
		ana = novoProfissional("Ana");
		bia = novoProfissional("Bia");
	}

	private Profissional novoProfissional(String nome) {
		Profissional profissional = Profissional.cadastrar(estabelecimento.id(), null, nome,
				nome.toLowerCase() + "@salao.com", null);
		profissional.definirServicos(List.of(new ServicoProfissional(corte.id(), new BigDecimal("95.00"))));
		return profissionais.salvar(profissional);
	}

	private Agendamento reservar(Profissional profissional, Instant inicio, int duracaoMinutos) {
		return Agendamento.reservar(UUID.randomUUID(), estabelecimento.id(), profissional.id(), corte.id(), inicio,
				duracaoMinutos, new BigDecimal("95.00"), "Primeira vez", AGORA);
	}

	@Test
	void salvaELeOAgendamentoComOHistoricoDaReserva() {
		Agendamento reservado = reservar(ana, DEZ_HORAS, 60);

		Agendamento salvo = agendamentos.salvar(reservado);

		assertThat(salvo.versao()).isNotNull();
		assertThat(salvo.alteracoesPendentes()).isEmpty();
		Agendamento lido = agendamentos.buscarPorId(reservado.id()).orElseThrow();
		assertThat(lido.clienteId()).isEqualTo(reservado.clienteId());
		assertThat(lido.estabelecimentoId()).isEqualTo(estabelecimento.id());
		assertThat(lido.profissionalId()).isEqualTo(ana.id());
		assertThat(lido.servicoId()).isEqualTo(corte.id());
		assertThat(lido.inicio()).isEqualTo(DEZ_HORAS);
		assertThat(lido.fim()).isEqualTo(DEZ_HORAS.plus(Duration.ofMinutes(60)));
		assertThat(lido.status()).isEqualTo(StatusAgendamento.AGENDADO);
		assertThat(lido.preco()).isEqualTo(new BigDecimal("95.00"));
		assertThat(lido.observacoes()).isEqualTo("Primeira vez");
		assertThat(lido.versao()).isEqualTo(salvo.versao());
		assertThat(historico.findByAgendamentoIdOrderByOcorridoEm(reservado.id()))
				.singleElement()
				.satisfies(linha -> {
					assertThat(linha.getStatusAnterior()).isNull();
					assertThat(linha.getStatusNovo()).isEqualTo(StatusAgendamento.AGENDADO);
					assertThat(linha.getAlteradoPor()).isEqualTo(reservado.clienteId());
					assertThat(linha.getOcorridoEm()).isEqualTo(AGORA);
				});
	}

	@Test
	void buscaInexistenteRetornaVazio() {
		assertThat(agendamentos.buscarPorId(UUID.randomUUID())).isEmpty();
	}

	@Test
	void recusaHorarioSobrepostoNoMesmoProfissional() {
		agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));
		Agendamento sobreposto = reservar(ana, DEZ_HORAS.plus(Duration.ofMinutes(30)), 60);

		assertThatThrownBy(() -> agendamentos.salvar(sobreposto))
				.isInstanceOf(HorarioIndisponivelException.class)
				.isInstanceOf(ConflitoException.class)
				.hasMessage("O profissional já tem um agendamento nesse horário");
		assertThat(agendamentos.buscarPorId(sobreposto.id())).isEmpty();
		assertThat(historico.findByAgendamentoIdOrderByOcorridoEm(sobreposto.id())).isEmpty();
	}

	@Test
	void aceitaHorarioQueComecaQuandoOAnteriorTermina() {
		agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));

		Agendamento seguinte = agendamentos.salvar(reservar(ana, DEZ_HORAS.plus(Duration.ofMinutes(60)), 30));

		assertThat(agendamentos.buscarPorId(seguinte.id())).isPresent();
	}

	@Test
	void aceitaOMesmoHorarioComOutroProfissional() {
		agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));

		Agendamento comBia = agendamentos.salvar(reservar(bia, DEZ_HORAS, 60));

		assertThat(agendamentos.buscarPorId(comBia.id())).isPresent();
	}

	@Test
	void cancelamentoLiberaOHorarioEGravaOHistorico() {
		Agendamento primeiro = agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));
		Agendamento lido = agendamentos.buscarPorId(primeiro.id()).orElseThrow();
		lido.cancelar("Imprevisto", lido.clienteId(), AGORA.plusSeconds(60));

		Agendamento cancelado = agendamentos.salvar(lido);
		Agendamento novo = agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));

		assertThat(cancelado.status()).isEqualTo(StatusAgendamento.CANCELADO);
		assertThat(cancelado.versao()).isGreaterThan(primeiro.versao());
		assertThat(agendamentos.buscarPorId(novo.id())).isPresent();
		assertThat(historico.findByAgendamentoIdOrderByOcorridoEm(primeiro.id()))
				.extracting(AgendamentoHistoricoJpaEntity::getStatusNovo, AgendamentoHistoricoJpaEntity::getMotivo)
				.containsExactly(
						tuple(StatusAgendamento.AGENDADO, null),
						tuple(StatusAgendamento.CANCELADO, "Imprevisto"));
	}

	@Test
	void reagendamentoGravaOsInicios() {
		Agendamento primeiro = agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));
		Agendamento lido = agendamentos.buscarPorId(primeiro.id()).orElseThrow();
		Instant novoInicio = DEZ_HORAS.plus(Duration.ofDays(1));
		lido.reagendar(novoInicio, lido.clienteId(), AGORA.plusSeconds(60));

		agendamentos.salvar(lido);

		assertThat(agendamentos.buscarPorId(primeiro.id()).orElseThrow().inicio()).isEqualTo(novoInicio);
		assertThat(historico.findByAgendamentoIdOrderByOcorridoEm(primeiro.id())).last().satisfies(linha -> {
			assertThat(linha.getInicioAnterior()).isEqualTo(DEZ_HORAS);
			assertThat(linha.getInicioNovo()).isEqualTo(novoInicio);
		});
	}

	@Test
	void listaOcupacoesAtivasDoProfissionalNoPeriodo() {
		Agendamento manha = agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));
		Agendamento cancelado = agendamentos.salvar(reservar(ana, DEZ_HORAS.plus(Duration.ofHours(2)), 60));
		Agendamento lido = agendamentos.buscarPorId(cancelado.id()).orElseThrow();
		lido.cancelar(null, lido.clienteId(), AGORA);
		agendamentos.salvar(lido);
		agendamentos.salvar(reservar(ana, DEZ_HORAS.plus(Duration.ofDays(1)), 60));
		agendamentos.salvar(reservar(bia, DEZ_HORAS, 60));

		Intervalo dia = new Intervalo(DEZ_HORAS.minus(Duration.ofHours(10)), DEZ_HORAS.plus(Duration.ofHours(10)));

		assertThat(agendamentos.ocupacoesDoProfissional(ana.id(), dia)).containsExactly(manha.intervalo());
	}

	@Test
	void ocupacaoQueSoEncostaNoPeriodoNaoEntra() {
		Agendamento agendamento = agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));

		Intervalo depois = new Intervalo(agendamento.fim(), agendamento.fim().plus(Duration.ofHours(1)));

		assertThat(agendamentos.ocupacoesDoProfissional(ana.id(), depois)).isEmpty();
	}

	@Test
	void listaAgendamentosDoClienteDoMaisRecenteParaOMaisAntigo() {
		UUID clienteId = UUID.randomUUID();
		Agendamento cedo = Agendamento.reservar(clienteId, estabelecimento.id(), ana.id(), corte.id(), DEZ_HORAS, 60,
				BigDecimal.TEN, null, AGORA);
		Agendamento tarde = Agendamento.reservar(clienteId, estabelecimento.id(), bia.id(), corte.id(),
				DEZ_HORAS.plus(Duration.ofDays(3)), 60, BigDecimal.TEN, null, AGORA);
		agendamentos.salvar(cedo);
		agendamentos.salvar(tarde);
		agendamentos.salvar(reservar(ana, DEZ_HORAS.plus(Duration.ofDays(5)), 60));

		assertThat(agendamentos.listarDoCliente(clienteId))
				.extracting(Agendamento::id)
				.containsExactly(tarde.id(), cedo.id());
	}

	@Test
	void calendarioDoProfissionalTrazOsQueTerminamDepoisDoCorteEmOrdemDeInicio() {
		Instant corteDoCalendario = DEZ_HORAS.plus(Duration.ofMinutes(15));
		Agendamento depois = gravarComStatus(estabelecimento, ana, DEZ_HORAS.plus(Duration.ofDays(1)),
				StatusAgendamento.CANCELADO);
		Agendamento cruzandoOCorte = gravarComStatus(estabelecimento, ana, DEZ_HORAS, StatusAgendamento.CONFIRMADO);
		gravarComStatus(estabelecimento, ana, DEZ_HORAS.minus(Duration.ofMinutes(30)), StatusAgendamento.CONCLUIDO);
		gravarComStatus(estabelecimento, bia, DEZ_HORAS, StatusAgendamento.AGENDADO);

		assertThat(agendamentos.listarDoProfissionalDesde(ana.id(), corteDoCalendario))
				.extracting(Agendamento::id)
				.containsExactly(cruzandoOCorte.id(), depois.id());
	}

	@Test
	void calendarioDoClienteTrazOsQueTerminamDepoisDoCorteEmOrdemDeInicio() {
		UUID clienteId = UUID.randomUUID();
		Agendamento tarde = agendamentos.salvar(Agendamento.reservar(clienteId, estabelecimento.id(), bia.id(),
				corte.id(), DEZ_HORAS.plus(Duration.ofDays(3)), 60, BigDecimal.TEN, null, AGORA));
		Agendamento cedo = agendamentos.salvar(Agendamento.reservar(clienteId, estabelecimento.id(), ana.id(),
				corte.id(), DEZ_HORAS, 60, BigDecimal.TEN, null, AGORA));
		agendamentos.salvar(reservar(ana, DEZ_HORAS.plus(Duration.ofDays(5)), 60));

		assertThat(agendamentos.listarDoClienteDesde(clienteId, DEZ_HORAS))
				.extracting(Agendamento::id)
				.containsExactly(cedo.id(), tarde.id());
		assertThat(agendamentos.listarDoClienteDesde(clienteId, DEZ_HORAS.plus(Duration.ofHours(1))))
				.extracting(Agendamento::id)
				.containsExactly(tarde.id());
	}

	private Agendamento gravarComStatus(Estabelecimento dono, Profissional profissional, Instant inicio,
			StatusAgendamento status) {
		return agendamentos.salvar(Agendamento.reconstituir(UUID.randomUUID(), UUID.randomUUID(), dono.id(),
				profissional.id(), corte.id(), inicio, inicio.plus(Duration.ofMinutes(30)), status,
				new BigDecimal("95.00"), null, null));
	}

	private List<UUID> idsDaAgenda(Intervalo periodo, Set<StatusAgendamento> status, UUID profissionalId) {
		return agendamentos.listarDoEstabelecimento(
				new FiltroDaAgenda(estabelecimento.id(), periodo, status, profissionalId))
				.stream().map(Agendamento::id).toList();
	}

	@Test
	void agendaDoEstabelecimentoTrazOsQueIniciamNoPeriodoEmOrdemDeInicio() {
		Intervalo periodo = new Intervalo(DEZ_HORAS, DEZ_HORAS.plus(Duration.ofHours(8)));
		Agendamento tarde = gravarComStatus(estabelecimento, ana, DEZ_HORAS.plus(Duration.ofHours(4)),
				StatusAgendamento.AGENDADO);
		Agendamento noInicio = gravarComStatus(estabelecimento, bia, DEZ_HORAS, StatusAgendamento.CONFIRMADO);
		gravarComStatus(estabelecimento, ana, DEZ_HORAS.minus(Duration.ofMinutes(15)), StatusAgendamento.AGENDADO);
		gravarComStatus(estabelecimento, ana, periodo.fim(), StatusAgendamento.AGENDADO);
		Estabelecimento outroEstabelecimento = estabelecimentos.salvar(Estabelecimento.cadastrar(
				UUID.randomUUID(), "Outro Salão", null, endereco(), null));
		gravarComStatus(outroEstabelecimento, bia, DEZ_HORAS.plus(Duration.ofHours(1)), StatusAgendamento.AGENDADO);

		assertThat(idsDaAgenda(periodo, Set.of(), null)).containsExactly(noInicio.id(), tarde.id());
	}

	@Test
	void agendaDoEstabelecimentoFiltraPorStatusEProfissional() {
		Intervalo periodo = new Intervalo(DEZ_HORAS, DEZ_HORAS.plus(Duration.ofHours(8)));
		Agendamento agendadoDaAna = gravarComStatus(estabelecimento, ana, DEZ_HORAS, StatusAgendamento.AGENDADO);
		Agendamento canceladoDaAna = gravarComStatus(estabelecimento, ana, DEZ_HORAS.plus(Duration.ofHours(1)),
				StatusAgendamento.CANCELADO);
		Agendamento confirmadoDaBia = gravarComStatus(estabelecimento, bia, DEZ_HORAS.plus(Duration.ofHours(2)),
				StatusAgendamento.CONFIRMADO);

		assertThat(idsDaAgenda(periodo, Set.of(StatusAgendamento.CANCELADO, StatusAgendamento.CONFIRMADO), null))
				.containsExactly(canceladoDaAna.id(), confirmadoDaBia.id());
		assertThat(idsDaAgenda(periodo, Set.of(), ana.id()))
				.containsExactly(agendadoDaAna.id(), canceladoDaAna.id());
		assertThat(idsDaAgenda(periodo, Set.of(StatusAgendamento.AGENDADO), ana.id()))
				.containsExactly(agendadoDaAna.id());
		assertThat(idsDaAgenda(periodo, Set.of(StatusAgendamento.AGENDADO), bia.id())).isEmpty();
	}

	@Test
	void atualizarOProfissionalNaoEsbarraNosAgendamentos() {
		agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));
		Profissional lida = profissionais.buscarPorId(ana.id()).orElseThrow();
		lida.atualizarPerfil("Ana Souza", "ana@salao.com", "Colorista", true);
		lida.definirServicos(List.of(new ServicoProfissional(corte.id(), new BigDecimal("99.00"))));

		profissionais.salvar(lida);

		assertThat(profissionais.buscarPorId(ana.id()).orElseThrow().bio()).isEqualTo("Colorista");
	}

	@Test
	void recusaGravarVersaoDesatualizada() {
		Agendamento salvo = agendamentos.salvar(reservar(ana, DEZ_HORAS, 60));
		Agendamento primeiraLeitura = agendamentos.buscarPorId(salvo.id()).orElseThrow();
		Agendamento segundaLeitura = agendamentos.buscarPorId(salvo.id()).orElseThrow();
		primeiraLeitura.confirmar(UUID.randomUUID(), AGORA);
		agendamentos.salvar(primeiraLeitura);
		segundaLeitura.cancelar(null, segundaLeitura.clienteId(), AGORA);

		assertThatThrownBy(() -> agendamentos.salvar(segundaLeitura))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		assertThat(agendamentos.buscarPorId(salvo.id()).orElseThrow().status())
				.isEqualTo(StatusAgendamento.CONFIRMADO);
	}

}
