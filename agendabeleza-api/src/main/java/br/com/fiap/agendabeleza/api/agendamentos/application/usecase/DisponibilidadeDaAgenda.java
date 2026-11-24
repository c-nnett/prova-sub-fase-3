package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.AgendaProfissional;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Verifica se um horário cabe na agenda do profissional, para a reserva e para o reagendamento. A ordem das
 * verificações define o erro: conflito com outro agendamento vira 409 ({@link HorarioIndisponivelException});
 * bloqueio do profissional, ou horário fora da jornada ou do funcionamento, vira 422.
 * <p>
 * Deve ser chamada dentro da execução serializada por profissional, junto com a gravação.
 */
public final class DisponibilidadeDaAgenda {

	public static final String FORA_DO_ATENDIMENTO = "O horário está fora do atendimento do profissional";
	public static final String PROFISSIONAL_INDISPONIVEL = "O profissional está indisponível nesse horário";

	private final AgendamentoRepository agendamentos;
	private final BloqueioAgendaRepository bloqueios;

	DisponibilidadeDaAgenda(AgendamentoRepository agendamentos, BloqueioAgendaRepository bloqueios) {
		this.agendamentos = agendamentos;
		this.bloqueios = bloqueios;
	}

	static void exigirProfissionalAtivo(Profissional profissional) {
		if (!profissional.ativo()) {
			throw new RegraNegocioException("O profissional está inativo");
		}
	}

	static void exigirEstabelecimentoAtivo(Estabelecimento estabelecimento) {
		if (!estabelecimento.ativo()) {
			throw new RegraNegocioException("O estabelecimento está inativo");
		}
	}

	/** Para uma nova reserva: todos os agendamentos ativos do profissional ocupam a agenda. */
	void garantirHorarioLivre(Intervalo horario, Profissional profissional, Estabelecimento estabelecimento,
			Instant agora) {
		garantirHorarioLivre(horario, profissional, estabelecimento, agora, ocupacao -> true);
	}

	/**
	 * Para mover um agendamento: o intervalo que ele ocupa hoje não conta como ocupação, senão um reagendamento
	 * que se sobrepõe ao próprio horário antigo seria recusado. Basta comparar o intervalo, porque a restrição
	 * {@code agendamento_sem_conflito} garante que nenhum outro agendamento ativo do profissional o sobrepõe.
	 */
	void garantirHorarioLivreIgnorando(Intervalo intervaloAtual, Intervalo novoHorario, Profissional profissional,
			Estabelecimento estabelecimento, Instant agora) {
		garantirHorarioLivre(novoHorario, profissional, estabelecimento, agora,
				ocupacao -> !ocupacao.equals(intervaloAtual));
	}

	private void garantirHorarioLivre(Intervalo horario, Profissional profissional, Estabelecimento estabelecimento,
			Instant agora, Predicate<Intervalo> ocupacaoConsiderada) {
		ZoneId fuso = estabelecimento.timezone();
		LocalDate data = LocalDate.ofInstant(horario.inicio(), fuso);
		Intervalo dia = new Intervalo(data.atStartOfDay(fuso).toInstant(),
				data.plusDays(1).atStartOfDay(fuso).toInstant());
		List<Intervalo> agendados = agendamentos.ocupacoesDoProfissional(profissional.id(), dia).stream()
				.filter(ocupacaoConsiderada)
				.toList();
		List<Intervalo> bloqueados = bloqueios.listarNoPeriodo(profissional.id(), dia).stream()
				.map(BloqueioAgenda::periodo)
				.toList();

		if (agendados.stream().anyMatch(ocupacao -> ocupacao.sobrepoe(horario))) {
			throw new HorarioIndisponivelException();
		}
		if (bloqueados.stream().anyMatch(bloqueio -> bloqueio.sobrepoe(horario))) {
			throw new RegraNegocioException(PROFISSIONAL_INDISPONIVEL);
		}
		List<Intervalo> ocupacoes = Stream.concat(agendados.stream(), bloqueados.stream()).toList();
		AgendaProfissional agenda = AgendaProfissional.doDia(data, fuso,
				estabelecimento.horarioEm(data.getDayOfWeek()), profissional.jornada(), ocupacoes);
		if (!agenda.estaLivre(horario, agora)) {
			throw new RegraNegocioException(FORA_DO_ATENDIMENTO);
		}
	}

}
