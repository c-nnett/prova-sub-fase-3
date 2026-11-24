package br.com.fiap.agendabeleza.api.agendamentos.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;

/**
 * Agenda de um profissional em um dia. Calcula os horários livres como
 * {@code (jornada ∩ funcionamento) − ocupações}, em que as ocupações são os bloqueios e os agendamentos ativos.
 * <p>
 * Jornada e funcionamento estão no fuso do estabelecimento. Os horários são devolvidos como {@link Intervalo} em
 * {@link Instant}. Os candidatos partem do início de cada janela de trabalho, em passos de {@link #PASSO}, e
 * precisam comportar a duração do serviço, não se sobrepor a nenhuma ocupação e não ter começado.
 */
public final class AgendaProfissional {

	public static final Duration PASSO = Duration.ofMinutes(15);

	private final List<Intervalo> janelas;
	private final List<Intervalo> ocupacoes;

	private AgendaProfissional(List<Intervalo> janelas, List<Intervalo> ocupacoes) {
		this.janelas = janelas;
		this.ocupacoes = ocupacoes;
	}

	/**
	 * Monta a agenda do dia. A jornada pode ter períodos de todos os dias da semana: só os do dia de {@code data}
	 * são usados. Funcionamento vazio ou nulo significa estabelecimento fechado no dia.
	 *
	 * @param ocupacoes bloqueios e agendamentos ativos do profissional que alcançam o dia
	 */
	public static AgendaProfissional doDia(LocalDate data, ZoneId fuso, Optional<HorarioFuncionamento> horario,
			Collection<PeriodoTrabalho> jornada, Collection<Intervalo> ocupacoes) {
		obrigatorio(data, "data");
		obrigatorio(fuso, "fuso");
		DayOfWeek dia = data.getDayOfWeek();
		horario.ifPresent(h -> {
			if (!h.diaSemana().equals(dia)) {
				throw new RegraNegocioException(
						"O funcionamento informado é de " + h.diaSemana() + ", mas a data é " + dia);
			}
		});

		List<Intervalo> janelas = horario
				.map(h -> janelas(data, fuso, h, jornada))
				.orElseGet(List::of);
		List<Intervalo> ocupadas = Objects.isNull(ocupacoes)
				? List.of()
				: ocupacoes.stream().filter(Objects::nonNull).toList();
		return new AgendaProfissional(janelas, ocupadas);
	}

	/** Horários livres para um serviço com a duração informada, a partir de {@code agora}, em ordem. */
	public List<Intervalo> horariosLivres(Duration duracao, Instant agora) {
		obrigatorio(duracao, "duracao");
		obrigatorio(agora, "agora");
		if (duracao.isNegative() || duracao.isZero()) {
			throw new RegraNegocioException("duracao deve ser maior que zero");
		}
		List<Intervalo> livres = new ArrayList<>();
		for (Intervalo janela : janelas) {
			for (Instant inicio = janela.inicio(); !inicio.plus(duracao).isAfter(janela.fim());
					inicio = inicio.plus(PASSO)) {
				Intervalo candidato = new Intervalo(inicio, inicio.plus(duracao));
				if (estaLivre(candidato, agora)) {
					livres.add(candidato);
				}
			}
		}
		return List.copyOf(livres);
	}

	/**
	 * Indica se o horário cabe inteiro em uma janela de trabalho, não se sobrepõe a nenhuma ocupação e não começou
	 * antes de {@code agora}. Não exige que o início caia na grade de {@link #PASSO}.
	 */
	public boolean estaLivre(Intervalo horario, Instant agora) {
		obrigatorio(horario, "horario");
		obrigatorio(agora, "agora");
		return !horario.inicio().isBefore(agora)
				&& janelas.stream().anyMatch(janela -> janela.contem(horario))
				&& ocupacoes.stream().noneMatch(ocupacao -> ocupacao.sobrepoe(horario));
	}

	/** Interseção de cada período da jornada do dia com o funcionamento, sem janelas vazias e em ordem. */
	private static List<Intervalo> janelas(LocalDate data, ZoneId fuso, HorarioFuncionamento funcionamento,
			Collection<PeriodoTrabalho> jornada) {
		if (Objects.isNull(jornada)) {
			return List.of();
		}
		return jornada.stream()
				.filter(Objects::nonNull)
				.filter(periodo -> periodo.diaSemana().equals(data.getDayOfWeek()))
				.sorted(Comparator.comparing(PeriodoTrabalho::inicio))
				.flatMap(periodo -> {
					LocalTime inicio = maior(periodo.inicio(), funcionamento.abreAs());
					LocalTime fim = menor(periodo.fim(), funcionamento.fechaAs());
					return inicio.isBefore(fim)
							? Stream.of(new Intervalo(instante(data, inicio, fuso),
									instante(data, fim, fuso)))
							: Stream.empty();
				})
				.toList();
	}

	private static Instant instante(LocalDate data, LocalTime hora, ZoneId fuso) {
		return data.atTime(hora).atZone(fuso).toInstant();
	}

	private static LocalTime maior(LocalTime a, LocalTime b) {
		return a.isAfter(b) ? a : b;
	}

	private static LocalTime menor(LocalTime a, LocalTime b) {
		return a.isBefore(b) ? a : b;
	}

}
