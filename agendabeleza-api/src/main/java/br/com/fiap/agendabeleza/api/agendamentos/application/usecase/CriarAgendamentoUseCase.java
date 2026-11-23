package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.AgendaProfissional;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Reserva um horário com um profissional. O estabelecimento vem do profissional, e o preço é a tarifa dele para o
 * serviço, congelada no agendamento.
 * <p>
 * O conflito com outro agendamento é verificado antes de gravar, para responder com clareza. Duas reservas
 * simultâneas no mesmo horário passam por essa verificação, e a restrição {@code agendamento_sem_conflito} recusa a
 * segunda: o adaptador também a traduz para {@link HorarioIndisponivelException}.
 */
public class CriarAgendamentoUseCase {

	public static final String FORA_DO_ATENDIMENTO = "o horário está fora do atendimento do profissional";

	private final AgendamentoRepository agendamentos;
	private final ProfissionalRepository profissionais;
	private final ServicoRepository servicos;
	private final EstabelecimentoRepository estabelecimentos;
	private final Clock relogio;

	public CriarAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			ServicoRepository servicos, EstabelecimentoRepository estabelecimentos, Clock relogio) {
		this.agendamentos = agendamentos;
		this.profissionais = profissionais;
		this.servicos = servicos;
		this.estabelecimentos = estabelecimentos;
		this.relogio = relogio;
	}

	public Agendamento executar(Comando comando) {
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
			throw new RegraNegocioException("o profissional está inativo");
		}
		return profissional;
	}

	private Servico servicoDoProfissional(UUID servicoId, Profissional profissional) {
		Servico servico = servicos.buscarPorId(servicoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Serviço " + servicoId + " não encontrado"));
		if (!servico.ativo()) {
			throw new RegraNegocioException("o serviço está inativo");
		}
		if (!servico.pertenceA(profissional.estabelecimentoId())) {
			throw new RegraNegocioException("o serviço não pertence ao estabelecimento do profissional");
		}
		if (!profissional.executa(servico.id())) {
			throw new RegraNegocioException("o profissional não executa este serviço");
		}
		return servico;
	}

	private Estabelecimento estabelecimentoAtivo(UUID estabelecimentoId) {
		Estabelecimento estabelecimento = estabelecimentos.buscarPorId(estabelecimentoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + estabelecimentoId + " não encontrado"));
		if (!estabelecimento.ativo()) {
			throw new RegraNegocioException("o estabelecimento está inativo");
		}
		return estabelecimento;
	}

	/** Conflito com outro agendamento vira 409. Fora da jornada ou do funcionamento vira 422. */
	private void garantirHorarioLivre(Agendamento agendamento, Profissional profissional,
			Estabelecimento estabelecimento, Instant agora) {
		ZoneId fuso = estabelecimento.timezone();
		LocalDate data = LocalDate.ofInstant(agendamento.inicio(), fuso);
		Intervalo dia = new Intervalo(data.atStartOfDay(fuso).toInstant(),
				data.plusDays(1).atStartOfDay(fuso).toInstant());
		List<Intervalo> ocupacoes = agendamentos.ocupacoesDoProfissional(profissional.id(), dia);

		Intervalo horario = agendamento.intervalo();
		if (ocupacoes.stream().anyMatch(ocupacao -> ocupacao.sobrepoe(horario))) {
			throw new HorarioIndisponivelException();
		}
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
