package br.com.fiap.agendabeleza.api.agendamentos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;

class AgendaProfissionalTest {

	/** Segunda-feira. */
	private static final LocalDate DIA = LocalDate.of(2026, 11, 30);
	private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
	private static final Instant ONTEM = DIA.minusDays(1).atStartOfDay(FUSO).toInstant();
	private static final Duration UMA_HORA = Duration.ofMinutes(60);

	private static final Optional<HorarioFuncionamento> FUNCIONAMENTO = Optional.of(
			new HorarioFuncionamento(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(20, 0)));

	private static Instant as(String hora) {
		return DIA.atTime(LocalTime.parse(hora)).atZone(FUSO).toInstant();
	}

	private static Intervalo intervalo(String inicio, String fim) {
		return new Intervalo(as(inicio), as(fim));
	}

	private static PeriodoTrabalho periodo(DayOfWeek dia, String inicio, String fim) {
		return new PeriodoTrabalho(dia, LocalTime.parse(inicio), LocalTime.parse(fim));
	}

	private static PeriodoTrabalho segunda(String inicio, String fim) {
		return periodo(DayOfWeek.MONDAY, inicio, fim);
	}

	private static AgendaProfissional agenda(Collection<PeriodoTrabalho> jornada, Collection<Intervalo> ocupacoes) {
		return AgendaProfissional.doDia(DIA, FUSO, FUNCIONAMENTO, jornada, ocupacoes);
	}

	private static List<String> inicios(List<Intervalo> horarios) {
		return horarios.stream()
				.map(horario -> horario.inicio().atZone(FUSO).toLocalTime().toString())
				.toList();
	}

	@Nested
	class HorariosLivres {

		@Test
		void geraCandidatosDeQuinzeEmQuinzeMinutosQueCabemNaJornada() {
			List<Intervalo> livres = agenda(List.of(segunda("09:00", "12:00")), List.of())
					.horariosLivres(UMA_HORA, ONTEM);

			assertThat(inicios(livres)).containsExactly(
					"09:00", "09:15", "09:30", "09:45", "10:00", "10:15", "10:30", "10:45", "11:00");
			assertThat(livres.getFirst()).isEqualTo(intervalo("09:00", "10:00"));
		}

		@Test
		void naoOfereceHorarioQueAtravessaOAlmoco() {
			List<String> livres = inicios(agenda(List.of(segunda("09:00", "12:00"), segunda("13:00", "18:00")),
					List.of()).horariosLivres(UMA_HORA, ONTEM));

			assertThat(livres).contains("11:00", "13:00")
					.doesNotContain("11:15", "11:30", "11:45", "12:00", "12:15", "12:30", "12:45");
		}

		@Test
		void servicoLongoNaoAtravessaOAlmoco() {
			List<String> livres = inicios(agenda(List.of(segunda("09:00", "12:00"), segunda("13:00", "15:00")),
					List.of()).horariosLivres(Duration.ofMinutes(90), ONTEM));

			assertThat(livres).containsExactly(
					"09:00", "09:15", "09:30", "09:45", "10:00", "10:15", "10:30", "13:00", "13:15", "13:30");
		}

		@Test
		void ultimoHorarioTerminaExatamenteNoFimDaJornada() {
			List<Intervalo> livres = agenda(List.of(segunda("13:00", "18:00")), List.of())
					.horariosLivres(UMA_HORA, ONTEM);

			assertThat(livres.getLast()).isEqualTo(intervalo("17:00", "18:00"));
			assertThat(inicios(livres)).doesNotContain("17:15", "17:30", "17:45");
		}

		@Test
		void servicoMaiorQueAJornadaNaoTemHorario() {
			assertThat(agenda(List.of(segunda("09:00", "10:00")), List.of())
					.horariosLivres(Duration.ofMinutes(75), ONTEM)).isEmpty();
		}

		@Test
		void diaFechadoNaoTemHorarioMesmoComJornada() {
			AgendaProfissional agenda = AgendaProfissional.doDia(DIA, FUSO, Optional.empty(),
					List.of(segunda("09:00", "12:00")), List.of());

			assertThat(agenda.horariosLivres(UMA_HORA, ONTEM)).isEmpty();
		}

		@Test
		void funcionamentoNuloEquivaleADiaFechado() {
			AgendaProfissional agenda = AgendaProfissional.doDia(DIA, FUSO, null,
					List.of(segunda("09:00", "12:00")), List.of());

			assertThat(agenda.horariosLivres(UMA_HORA, ONTEM)).isEmpty();
		}

		@Test
		void funcionamentoReduzidoDepoisDaJornadaRecortaOsHorarios() {
			Optional<HorarioFuncionamento> reduzido = Optional.of(
					new HorarioFuncionamento(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0)));
			AgendaProfissional agenda = AgendaProfissional.doDia(DIA, FUSO, reduzido,
					List.of(segunda("09:00", "18:00")), List.of());

			assertThat(inicios(agenda.horariosLivres(UMA_HORA, ONTEM)))
					.containsExactly("10:00", "10:15", "10:30", "10:45", "11:00");
		}

		@Test
		void jornadaForaDoFuncionamentoNaoTemHorario() {
			Optional<HorarioFuncionamento> tarde = Optional.of(
					new HorarioFuncionamento(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(20, 0)));
			AgendaProfissional agenda = AgendaProfissional.doDia(DIA, FUSO, tarde,
					List.of(segunda("09:00", "12:00")), List.of());

			assertThat(agenda.horariosLivres(UMA_HORA, ONTEM)).isEmpty();
		}

		@Test
		void ignoraPeriodosDeOutrosDias() {
			AgendaProfissional agenda = agenda(List.of(
					periodo(DayOfWeek.TUESDAY, "09:00", "12:00"),
					periodo(DayOfWeek.SUNDAY, "09:00", "12:00")), List.of());

			assertThat(agenda.horariosLivres(UMA_HORA, ONTEM)).isEmpty();
		}

		@Test
		void jornadaNulaNaoTemHorario() {
			assertThat(agenda(null, List.of()).horariosLivres(UMA_HORA, ONTEM)).isEmpty();
		}

		@Test
		void bloqueioParcialRemoveSomenteOsHorariosQueSeCruzam() {
			List<String> livres = inicios(agenda(List.of(segunda("09:00", "13:00")),
					List.of(intervalo("10:00", "11:00"))).horariosLivres(UMA_HORA, ONTEM));

			assertThat(livres).containsExactly("09:00", "11:00", "11:15", "11:30", "11:45", "12:00");
		}

		@Test
		void bloqueioDeVariosDiasCobreODiaInteiro() {
			Intervalo ferias = new Intervalo(DIA.minusDays(3).atStartOfDay(FUSO).toInstant(),
					DIA.plusDays(4).atStartOfDay(FUSO).toInstant());

			assertThat(agenda(List.of(segunda("09:00", "18:00")), List.of(ferias))
					.horariosLivres(UMA_HORA, ONTEM)).isEmpty();
		}

		@Test
		void agendamentoAtivoOcupaOHorarioEOLiberaAoTerminar() {
			List<String> livres = inicios(agenda(List.of(segunda("13:00", "17:00")),
					List.of(intervalo("14:00", "15:00"))).horariosLivres(UMA_HORA, ONTEM));

			assertThat(livres).containsExactly("13:00", "15:00", "15:15", "15:30", "15:45", "16:00");
		}

		@Test
		void ignoraOcupacoesNulas() {
			List<Intervalo> ocupacoes = new ArrayList<>();
			ocupacoes.add(null);

			assertThat(agenda(List.of(segunda("09:00", "10:00")), ocupacoes).horariosLivres(UMA_HORA, ONTEM))
					.containsExactly(intervalo("09:00", "10:00"));
		}

		@Test
		void descartaHorariosQueJaComecaram() {
			List<String> livres = inicios(agenda(List.of(segunda("09:00", "12:00")), List.of())
					.horariosLivres(UMA_HORA, as("10:07")));

			assertThat(livres).containsExactly("10:15", "10:30", "10:45", "11:00");
		}

		@Test
		void horarioQueComecaAgoraAindaEstaLivre() {
			List<String> livres = inicios(agenda(List.of(segunda("09:00", "11:00")), List.of())
					.horariosLivres(UMA_HORA, as("10:00")));

			assertThat(livres).containsExactly("10:00");
		}

		@Test
		void dataNoPassadoNaoTemHorario() {
			Instant amanha = DIA.plusDays(1).atStartOfDay(FUSO).toInstant();

			assertThat(agenda(List.of(segunda("09:00", "12:00")), List.of()).horariosLivres(UMA_HORA, amanha))
					.isEmpty();
		}

		@Test
		void recusaDuracaoAusenteOuNaoPositiva() {
			AgendaProfissional agenda = agenda(List.of(segunda("09:00", "12:00")), List.of());

			assertThatThrownBy(() -> agenda.horariosLivres(null, ONTEM))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("duracao é obrigatório");
			assertThatThrownBy(() -> agenda.horariosLivres(Duration.ZERO, ONTEM))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("duracao deve ser maior que zero");
			assertThatThrownBy(() -> agenda.horariosLivres(Duration.ofMinutes(-15), ONTEM))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("duracao deve ser maior que zero");
		}

		@Test
		void recusaAgoraAusente() {
			AgendaProfissional agenda = agenda(List.of(segunda("09:00", "12:00")), List.of());

			assertThatThrownBy(() -> agenda.horariosLivres(UMA_HORA, null))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("agora é obrigatório");
		}

	}

	@Nested
	class EstaLivre {

		private final AgendaProfissional agenda = agenda(
				List.of(segunda("09:00", "12:00"), segunda("13:00", "18:00")),
				List.of(intervalo("15:00", "16:00")));

		@Test
		void aceitaHorarioForaDaGradeQueCabeNaJornada() {
			assertThat(agenda.estaLivre(intervalo("09:10", "10:10"), ONTEM)).isTrue();
		}

		@Test
		void aceitaHorarioQueEncostaNaOcupacao() {
			assertThat(agenda.estaLivre(intervalo("14:00", "15:00"), ONTEM)).isTrue();
			assertThat(agenda.estaLivre(intervalo("16:00", "17:00"), ONTEM)).isTrue();
		}

		@Test
		void recusaHorarioQueAtravessaOAlmoco() {
			assertThat(agenda.estaLivre(intervalo("11:30", "12:30"), ONTEM)).isFalse();
		}

		@Test
		void recusaHorarioForaDaJornada() {
			assertThat(agenda.estaLivre(intervalo("17:30", "18:30"), ONTEM)).isFalse();
			assertThat(agenda.estaLivre(intervalo("08:00", "09:00"), ONTEM)).isFalse();
		}

		@Test
		void recusaHorarioOcupado() {
			assertThat(agenda.estaLivre(intervalo("15:30", "16:30"), ONTEM)).isFalse();
		}

		@Test
		void recusaHorarioNoPassado() {
			assertThat(agenda.estaLivre(intervalo("09:00", "10:00"), as("09:01"))).isFalse();
		}

		@Test
		void recusaHorarioEmOutroDia() {
			Intervalo outroDia = new Intervalo(as("09:00").plus(Duration.ofDays(7)),
					as("10:00").plus(Duration.ofDays(7)));

			assertThat(agenda.estaLivre(outroDia, ONTEM)).isFalse();
		}

		@Test
		void recusaCamposAusentes() {
			assertThatThrownBy(() -> agenda.estaLivre(null, ONTEM)).hasMessage("horario é obrigatório");
			assertThatThrownBy(() -> agenda.estaLivre(intervalo("09:00", "10:00"), null))
					.hasMessage("agora é obrigatório");
		}

	}

	@Nested
	class Criacao {

		@Test
		void recusaDataOuFusoAusentes() {
			assertThatThrownBy(() -> AgendaProfissional.doDia(null, FUSO, FUNCIONAMENTO, List.of(), List.of()))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("data é obrigatório");
			assertThatThrownBy(() -> AgendaProfissional.doDia(DIA, null, FUNCIONAMENTO, List.of(), List.of()))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("fuso é obrigatório");
		}

		@Test
		void recusaFuncionamentoDeOutroDiaDaSemana() {
			Optional<HorarioFuncionamento> terca = Optional.of(
					new HorarioFuncionamento(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(20, 0)));

			assertThatThrownBy(() -> AgendaProfissional.doDia(DIA, FUSO, terca, List.of(), List.of()))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O funcionamento informado é de TUESDAY, mas a data é MONDAY");
		}

	}

}
