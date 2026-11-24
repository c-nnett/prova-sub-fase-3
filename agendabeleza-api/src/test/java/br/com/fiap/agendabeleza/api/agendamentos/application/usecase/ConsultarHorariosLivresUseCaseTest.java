package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.estabelecimento;
import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.profissional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

class ConsultarHorariosLivresUseCaseTest {

	/** Segunda-feira. */
	private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 30);
	private static final ZoneId FUSO = Estabelecimento.TIMEZONE_PADRAO;
	private static final Instant DOMINGO = Instant.parse("2026-11-29T12:00:00Z");

	private final EstabelecimentoRepositoryEmMemoria estabelecimentos = new EstabelecimentoRepositoryEmMemoria();
	private final ServicoRepositoryEmMemoria servicos = new ServicoRepositoryEmMemoria();
	private final ProfissionalRepositoryEmMemoria profissionais = new ProfissionalRepositoryEmMemoria();
	private final BloqueioAgendaRepositoryEmMemoria bloqueios = new BloqueioAgendaRepositoryEmMemoria();

	private final Estabelecimento estabelecimento = estabelecimentoAberto();
	private final Servico corte = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Corte", null,
			CategoriaServico.CABELO, 60, new BigDecimal("80.00")));
	private final Profissional ana = anaQueCorta();

	private Estabelecimento estabelecimentoAberto() {
		Estabelecimento novo = estabelecimento();
		novo.definirHorarios(List.of(new HorarioFuncionamento(DayOfWeek.MONDAY, LocalTime.of(8, 0),
				LocalTime.of(20, 0))));
		return estabelecimentos.salvar(novo);
	}

	private Profissional anaQueCorta() {
		Profissional profissional = profissional(estabelecimento.id());
		profissional.definirServicos(List.of(new ServicoProfissional(corte.id(), new BigDecimal("90.00"))));
		profissional.definirJornada(List.of(
				new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))));
		return profissionais.salvar(profissional);
	}

	private ConsultarHorariosLivresUseCase consultarEm(Instant agora) {
		return new ConsultarHorariosLivresUseCase(profissionais, estabelecimentos, servicos, bloqueios,
				Clock.fixed(agora, ZoneOffset.UTC));
	}

	private HorariosLivres consultar(LocalDate data) {
		return consultarEm(DOMINGO).executar(new ConsultarHorariosLivresUseCase.Consulta(ana.id(), corte.id(), data));
	}

	private static Instant as(String hora) {
		return SEGUNDA.atTime(LocalTime.parse(hora)).atZone(FUSO).toInstant();
	}

	private static List<String> inicios(HorariosLivres livres) {
		return livres.horarios().stream()
				.map(horario -> horario.inicio().atZone(FUSO).toLocalTime().toString())
				.toList();
	}

	private BloqueioAgenda bloquear(UUID profissionalId, Instant inicio, Instant fim) {
		return bloqueios.salvar(BloqueioAgenda.reconstituir(UUID.randomUUID(), profissionalId, inicio, fim, null));
	}

	@Nested
	class Calculo {

		@Test
		void devolveOsHorariosNoFusoDoEstabelecimentoComADuracaoDoServico() {
			HorariosLivres livres = consultar(SEGUNDA);

			assertThat(livres.data()).isEqualTo(SEGUNDA);
			assertThat(livres.fuso()).isEqualTo(FUSO);
			assertThat(livres.duracaoMinutos()).isEqualTo(60);
			assertThat(livres.horarios().getFirst()).isEqualTo(new Intervalo(as("09:00"), as("10:00")));
			assertThat(inicios(livres)).containsExactly(
					"09:00", "09:15", "09:30", "09:45", "10:00", "10:15", "10:30", "10:45", "11:00");
		}

		@Test
		void descontaOsBloqueiosDoProfissional() {
			bloquear(ana.id(), as("10:00"), as("11:00"));

			assertThat(inicios(consultar(SEGUNDA))).containsExactly("09:00", "11:00");
		}

		@Test
		void bloqueioQueComecaNoDiaAnteriorAlcancaODia() {
			bloquear(ana.id(), as("00:00").minusSeconds(86400 * 3), as("10:30"));

			assertThat(inicios(consultar(SEGUNDA))).containsExactly("10:30", "10:45", "11:00");
		}

		@Test
		void ignoraBloqueioDeOutroProfissional() {
			bloquear(UUID.randomUUID(), as("09:00"), as("12:00"));

			assertThat(consultar(SEGUNDA).horarios()).hasSize(9);
		}

		@Test
		void diaFechadoNaoTemHorarios() {
			assertThat(consultar(SEGUNDA.plusDays(1)).horarios()).isEmpty();
		}

		@Test
		void usaORelogioParaDescartarHorariosQueJaComecaram() {
			HorariosLivres livres = consultarEm(as("10:40"))
					.executar(new ConsultarHorariosLivresUseCase.Consulta(ana.id(), corte.id(), SEGUNDA));

			assertThat(inicios(livres)).containsExactly("10:45", "11:00");
		}

		@Test
		void dataNoPassadoNaoTemHorarios() {
			assertThat(consultar(SEGUNDA.minusDays(7)).horarios()).isEmpty();
		}

	}

	@Nested
	class Validacoes {

		@Test
		void recusaDataAusente() {
			assertThatThrownBy(() -> consultar(null))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("data é obrigatório");
		}

		@Test
		void recusaProfissionalInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> consultarEm(DOMINGO)
					.executar(new ConsultarHorariosLivresUseCase.Consulta(inexistente, corte.id(), SEGUNDA)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Profissional " + inexistente + " não encontrado");
		}

		@Test
		void recusaServicoInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> consultarEm(DOMINGO)
					.executar(new ConsultarHorariosLivresUseCase.Consulta(ana.id(), inexistente, SEGUNDA)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Serviço " + inexistente + " não encontrado");
		}

		@Test
		void recusaEstabelecimentoInexistente() {
			UUID semEstabelecimento = UUID.randomUUID();
			Profissional orfao = profissionais.salvar(profissional(semEstabelecimento));

			assertThatThrownBy(() -> consultarEm(DOMINGO)
					.executar(new ConsultarHorariosLivresUseCase.Consulta(orfao.id(), corte.id(), SEGUNDA)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Estabelecimento " + semEstabelecimento + " não encontrado");
		}

		@Test
		void recusaProfissionalInativo() {
			ana.atualizarPerfil(ana.nome(), ana.email(), ana.bio(), false);

			assertThatThrownBy(() -> consultar(SEGUNDA))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O profissional está inativo");
		}

		@Test
		void recusaEstabelecimentoInativo() {
			estabelecimentos.salvar(Estabelecimento.reconstituir(estabelecimento.id(), GESTOR_ID,
					estabelecimento.nome(), null, endereco(), null, false, List.of(), estabelecimento.horarios()));

			assertThatThrownBy(() -> consultar(SEGUNDA))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O estabelecimento está inativo");
		}

		@Test
		void recusaServicoDeOutroEstabelecimento() {
			Servico deOutro = servicos.salvar(Servico.cadastrar(UUID.randomUUID(), "Corte", null,
					CategoriaServico.CABELO, 60, new BigDecimal("80.00")));

			assertThatThrownBy(() -> consultarEm(DOMINGO)
					.executar(new ConsultarHorariosLivresUseCase.Consulta(ana.id(), deOutro.id(), SEGUNDA)))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O serviço não pertence ao estabelecimento do profissional");
		}

		@Test
		void recusaServicoInativo() {
			corte.atualizar(corte.nome(), null, corte.categoria(), 60, corte.precoBase(), false);

			assertThatThrownBy(() -> consultar(SEGUNDA))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O serviço está inativo");
		}

		@Test
		void recusaServicoQueOProfissionalNaoExecuta() {
			Servico manicure = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Manicure", null,
					CategoriaServico.UNHAS, 45, new BigDecimal("40.00")));

			assertThatThrownBy(() -> consultarEm(DOMINGO)
					.executar(new ConsultarHorariosLivresUseCase.Consulta(ana.id(), manicure.id(), SEGUNDA)))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O profissional não executa o serviço");
		}

	}

}
