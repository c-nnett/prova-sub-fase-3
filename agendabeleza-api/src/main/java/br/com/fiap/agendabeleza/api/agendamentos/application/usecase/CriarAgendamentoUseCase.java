package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.AgendaProfissional;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.compartilhado.application.port.ExecucaoSerializada;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Reserva um horário com um profissional. O estabelecimento vem do profissional, e o preço é a tarifa dele para o
 * serviço, congelada no agendamento.
 * <p>
 * As verificações e a gravação rodam em {@link ExecucaoSerializada}, uma reserva ou bloqueio de cada vez por
 * profissional. Assim, uma reserva e um bloqueio simultâneos não passam juntos pelas verificações. A restrição
 * {@code agendamento_sem_conflito} continua como garantia final entre reservas, e o adaptador a traduz para
 * {@link HorarioIndisponivelException}.
 */
public class CriarAgendamentoUseCase {

	public static final String FORA_DO_ATENDIMENTO = "O horário está fora do atendimento do profissional";
	public static final String PROFISSIONAL_INDISPONIVEL = "O profissional está indisponível nesse horário";

	private final AgendamentoRepository agendamentos;
	private final ProfissionalRepository profissionais;
	private final ServicoRepository servicos;
	private final EstabelecimentoRepository estabelecimentos;
	private final BloqueioAgendaRepository bloqueios;
	private final ExecucaoSerializada execucao;
	private final Clock relogio;

	@SuppressWarnings("java:S107") // Uma porta por fonte de dados da agenda, injetadas pelo construtor.
	public CriarAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			ServicoRepository servicos, EstabelecimentoRepository estabelecimentos, BloqueioAgendaRepository bloqueios,
			ExecucaoSerializada execucao, Clock relogio) {
		this.agendamentos = agendamentos;
		this.profissionais = profissionais;
		this.servicos = servicos;
		this.estabelecimentos = estabelecimentos;
		this.bloqueios = bloqueios;
		this.execucao = execucao;
		this.relogio = relogio;
	}

	public Agendamento executar(Comando comando) {
		return execucao.executar(comando.profissionalId(), () -> reservar(comando));
	}

	private Agendamento reservar(Comando comando) {
		Profissional profissional = profissionalAtivo(comando.profissionalId());
		Servico servico = servicoDoProfissional(comando.servicoId(), profissional);
		Estabelecimento estabelecimento = estabelecimentoAtivo(profissional.estabelecimentoId());
		BigDecimal preco = profissional.tarifaDe(servico.id()).orElseThrow();
		Instant agora = relogio.instant();

		Agendamento agendamento = Agendamento.reservar(comando.clienteId(), estabelecimento.id(), profissional.id(),
				servico.id(), comando.inicio(), servico.duracaoMinutos(), preco, comando.observacoes(), agora);
		garantirHorarioLivre(agendamento, profissional, estabelecimento, agora);
		return agendamentos.salvar(agendamento);
	}

	private Profissional profissionalAtivo(UUID profissionalId) {
		Profissional profissional = profissionais.buscarPorId(profissionalId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Profissional " + profissionalId + " não encontrado"));
		if (!profissional.ativo()) {
			throw new RegraNegocioException("O profissional está inativo");
		}
		return profissional;
	}

	private Servico servicoDoProfissional(UUID servicoId, Profissional profissional) {
		Servico servico = servicos.buscarPorId(servicoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Serviço " + servicoId + " não encontrado"));
		if (!servico.ativo()) {
			throw new RegraNegocioException("O serviço está inativo");
		}
		if (!servico.pertenceA(profissional.estabelecimentoId())) {
			throw new RegraNegocioException("O serviço não pertence ao estabelecimento do profissional");
		}
		if (!profissional.executa(servico.id())) {
			throw new RegraNegocioException("O profissional não executa o serviço");
		}
		return servico;
	}

	private Estabelecimento estabelecimentoAtivo(UUID estabelecimentoId) {
		Estabelecimento estabelecimento = estabelecimentos.buscarPorId(estabelecimentoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + estabelecimentoId + " não encontrado"));
		if (!estabelecimento.ativo()) {
			throw new RegraNegocioException("O estabelecimento está inativo");
		}
		return estabelecimento;
	}

	/**
	 * Conflito com outro agendamento vira 409. Bloqueio do profissional, ou horário fora da jornada ou do
	 * funcionamento, vira 422.
	 */
	private void garantirHorarioLivre(Agendamento agendamento, Profissional profissional,
			Estabelecimento estabelecimento, Instant agora) {
		ZoneId fuso = estabelecimento.timezone();
		LocalDate data = LocalDate.ofInstant(agendamento.inicio(), fuso);
		Intervalo dia = new Intervalo(data.atStartOfDay(fuso).toInstant(),
				data.plusDays(1).atStartOfDay(fuso).toInstant());
		List<Intervalo> agendados = agendamentos.ocupacoesDoProfissional(profissional.id(), dia);
		List<Intervalo> bloqueados = bloqueios.listarNoPeriodo(profissional.id(), dia).stream()
				.map(BloqueioAgenda::periodo)
				.toList();

		Intervalo horario = agendamento.intervalo();
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

	public record Comando(
			UUID clienteId,
			UUID profissionalId,
			UUID servicoId,
			Instant inicio,
			String observacoes) {
	}

}
