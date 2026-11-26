package br.com.fiap.agendabeleza.api.agendamentos;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CriarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.DisponibilidadeDaAgenda;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ReagendarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.ConflitoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.CriarBloqueioUseCase;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

/**
 * Operações simultâneas na agenda de um profissional, no PostgreSQL real. A reserva consulta os bloqueios e o
 * bloqueio consulta as reservas: sem a execução serializada por profissional, as duas podem passar juntas.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcorrenciaAgendaTest {

	/** Segunda-feira, 4 de março de 2030, à meia-noite em Brasília. */
	private static final Instant SEGUNDA = Instant.parse("2030-03-04T03:00:00Z");
	private static final int RODADAS = 8;
	/** Cada rodada usa duas horas da jornada (9h às 18h): o horário atual e o novo. */
	private static final int RODADAS_DE_REAGENDAMENTO = 4;

	@Autowired
	private CriarAgendamentoUseCase criarAgendamento;

	@Autowired
	private ReagendarAgendamentoUseCase reagendarAgendamento;

	@Autowired
	private CriarBloqueioUseCase criarBloqueio;

	@Autowired
	private EstabelecimentoRepository estabelecimentos;

	@Autowired
	private ServicoRepository servicos;

	@Autowired
	private ProfissionalRepository profissionais;

	@Autowired
	private JdbcTemplate jdbc;

	private final ExecutorService threads = Executors.newFixedThreadPool(5);
	private UUID gestorId;
	private Servico corte;
	private Profissional ana;

	@BeforeEach
	void prepararAgenda() {
		gestorId = UUID.randomUUID();
		Estabelecimento estabelecimento = Estabelecimento.cadastrar(gestorId, "Salão Concorrência", null,
				endereco(), null);
		estabelecimento.definirHorarios(List.of(
				new HorarioFuncionamento(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(20, 0))));
		estabelecimentos.salvar(estabelecimento);
		corte = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Corte", null, CategoriaServico.CABELO, 60,
				new BigDecimal("80.00")));
		Profissional profissional = Profissional.cadastrar(estabelecimento.id(), null, "Ana", "ana@salao.com",
				null);
		profissional.definirServicos(List.of(new ServicoProfissional(corte.id(), new BigDecimal("95.00"))));
		profissional.definirJornada(List.of(
				new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))));
		ana = profissionais.salvar(profissional);
	}

	@AfterEach
	void encerrarThreads() {
		threads.shutdownNow();
	}

	/** Uma reserva de um cliente novo a cada chamada. */
	private Agendamento reservar(Instant inicio) {
		return criarAgendamento.executar(new CriarAgendamentoUseCase.Comando(UUID.randomUUID(), "cliente@email.com",
				ana.id(), corte.id(), inicio, null));
	}

	@Test
	void reservaEBloqueioSimultaneosNoMesmoHorarioNaoPassamJuntos() throws Exception {
		for (int rodada = 0; rodada < RODADAS; rodada++) {
			Instant inicio = SEGUNDA.plus(Duration.ofHours(9L + rodada));
			CyclicBarrier largada = new CyclicBarrier(2);

			List<Resultado> resultados = executarJuntos(List.of(
					() -> {
						largada.await();
						return criarBloqueio.executar(new CriarBloqueioUseCase.Comando(ana.id(), gestorId, inicio,
								inicio.plus(Duration.ofHours(1)), "Concorrência"));
					},
					() -> {
						largada.await();
						return reservar(inicio);
					}));

			assertSomenteOBloqueioOuAAgendaPassa(resultados, inicio);
		}
	}

	@Test
	void reagendamentoEBloqueioSimultaneosNoMesmoHorarioNaoPassamJuntos() throws Exception {
		for (int rodada = 0; rodada < RODADAS_DE_REAGENDAMENTO; rodada++) {
			Instant inicioAtual = SEGUNDA.plus(Duration.ofHours(10L + 2L * rodada));
			Instant novoInicio = inicioAtual.plus(Duration.ofHours(1));
			Agendamento agendamento = reservar(inicioAtual);
			CyclicBarrier largada = new CyclicBarrier(2);

			List<Resultado> resultados = executarJuntos(List.of(
					() -> {
						largada.await();
						return criarBloqueio.executar(new CriarBloqueioUseCase.Comando(ana.id(), gestorId,
								novoInicio, novoInicio.plus(Duration.ofHours(1)), "Concorrência"));
					},
					() -> {
						largada.await();
						return reagendarAgendamento.executar(new ReagendarAgendamentoUseCase.Comando(
								agendamento.id(), gestorId, novoInicio));
					}));

			assertSomenteOBloqueioOuAAgendaPassa(resultados, novoInicio);
		}
	}

	/**
	 * Exatamente uma das duas operações passa. A outra é recusada pela verificação, e nunca as duas juntas: o
	 * bloqueio com 409 por encontrar o agendamento, ou a reserva/reagendamento com 422 por encontrar o bloqueio.
	 */
	private static void assertSomenteOBloqueioOuAAgendaPassa(List<Resultado> resultados, Instant horarioDisputado) {
		assertThat(resultados).as("rodada das %s", horarioDisputado)
				.filteredOn(Resultado::sucesso).hasSize(1);
		assertThat(resultados).filteredOn(resultado -> !resultado.sucesso())
				.singleElement()
				.satisfies(falha -> assertThat(falha.erro()).satisfiesAnyOf(
						erro -> assertThat(erro).isInstanceOf(ConflitoException.class)
								.hasMessage("Há agendamento ativo no período do bloqueio"),
						erro -> assertThat(erro).isInstanceOf(RegraNegocioException.class)
								.hasMessage(DisponibilidadeDaAgenda.PROFISSIONAL_INDISPONIVEL)));
	}

	@Test
	void reservasSimultaneasNoMesmoHorarioAceitamSomenteUma() throws Exception {
		Instant inicio = SEGUNDA.plus(Duration.ofHours(10));
		CyclicBarrier largada = new CyclicBarrier(5);
		List<Callable<Object>> reservas = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			reservas.add(() -> {
				largada.await();
				return reservar(inicio);
			});
		}

		List<Resultado> resultados = executarJuntos(reservas);

		assertThat(resultados).filteredOn(Resultado::sucesso).hasSize(1);
		assertThat(resultados).filteredOn(resultado -> !resultado.sucesso())
				.hasSize(4)
				.allSatisfy(falha -> assertThat(falha.erro()).isInstanceOf(HorarioIndisponivelException.class));
		assertThat(notificacoesDaProfissional())
				.as("só a reserva aceita deixa notificações; as recusadas sofrem rollback junto com a outbox")
				.isEqualTo(2);
	}

	/** Contagem direta no banco: a outbox não expõe esse total pela porta. */
	private int notificacoesDaProfissional() {
		return jdbc.queryForObject("""
				select count(*) from notificacao n
				join agendamento a on a.id = n.agendamento_id
				where a.profissional_id = ?
				""", Integer.class, ana.id());
	}

	private List<Resultado> executarJuntos(List<Callable<Object>> operacoes) throws InterruptedException {
		List<Future<Object>> futuros = new ArrayList<>();
		for (Callable<Object> operacao : operacoes) {
			futuros.add(threads.submit(operacao));
		}
		List<Resultado> resultados = new ArrayList<>();
		for (Future<Object> futuro : futuros) {
			try {
				futuro.get();
				resultados.add(new Resultado(true, null));
			} catch (ExecutionException excecao) {
				resultados.add(new Resultado(false, excecao.getCause()));
			}
		}
		return resultados;
	}

	private record Resultado(boolean sucesso, Throwable erro) {
	}

}
