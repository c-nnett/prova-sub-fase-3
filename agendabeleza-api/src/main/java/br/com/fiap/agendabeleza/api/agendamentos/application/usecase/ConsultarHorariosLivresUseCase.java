package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.AgendaProfissional;
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
 * Consulta pública dos horários livres de um profissional para um serviço em uma data, no fuso do
 * estabelecimento. Cruza jornada e funcionamento com {@link AgendaProfissional}, descontando como ocupações os
 * bloqueios e os agendamentos ativos do profissional.
 */
public class ConsultarHorariosLivresUseCase {

	private final ProfissionalRepository profissionais;
	private final EstabelecimentoRepository estabelecimentos;
	private final ServicoRepository servicos;
	private final BloqueioAgendaRepository bloqueios;
	private final AgendamentoRepository agendamentos;
	private final Clock relogio;

	public ConsultarHorariosLivresUseCase(ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, ServicoRepository servicos,
			BloqueioAgendaRepository bloqueios, AgendamentoRepository agendamentos, Clock relogio) {
		this.profissionais = profissionais;
		this.estabelecimentos = estabelecimentos;
		this.servicos = servicos;
		this.bloqueios = bloqueios;
		this.agendamentos = agendamentos;
		this.relogio = relogio;
	}

	public HorariosLivres executar(Consulta consulta) {
		LocalDate data = obrigatorio(consulta.data(), "data");
		Profissional profissional = profissionais.buscarPorId(consulta.profissionalId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Profissional " + consulta.profissionalId() + " não encontrado"));
		if (!profissional.ativo()) {
			throw new RegraNegocioException("O profissional está inativo");
		}
		Estabelecimento estabelecimento = estabelecimentos.buscarPorId(profissional.estabelecimentoId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + profissional.estabelecimentoId() + " não encontrado"));
		if (!estabelecimento.ativo()) {
			throw new RegraNegocioException("O estabelecimento está inativo");
		}
		Servico servico = servicoExecutado(profissional, consulta.servicoId());

		ZoneId fuso = estabelecimento.timezone();
		Intervalo dia = new Intervalo(data.atStartOfDay(fuso).toInstant(),
				data.plusDays(1).atStartOfDay(fuso).toInstant());
		List<Intervalo> ocupacoes = Stream.concat(
				bloqueios.listarNoPeriodo(profissional.id(), dia).stream().map(BloqueioAgenda::periodo),
				agendamentos.ocupacoesDoProfissional(profissional.id(), dia).stream())
				.toList();
		AgendaProfissional agenda = AgendaProfissional.doDia(data, fuso,
				estabelecimento.horarioEm(data.getDayOfWeek()), profissional.jornada(), ocupacoes);
		List<Intervalo> horarios = agenda.horariosLivres(Duration.ofMinutes(servico.duracaoMinutos()),
				relogio.instant());
		return new HorariosLivres(data, fuso, servico.duracaoMinutos(), horarios);
	}

	private Servico servicoExecutado(Profissional profissional, UUID servicoId) {
		Servico servico = servicos.buscarPorId(servicoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Serviço " + servicoId + " não encontrado"));
		if (!servico.pertenceA(profissional.estabelecimentoId())) {
			throw new RegraNegocioException("O serviço não pertence ao estabelecimento do profissional");
		}
		if (!servico.ativo()) {
			throw new RegraNegocioException("O serviço está inativo");
		}
		if (!profissional.executa(servicoId)) {
			throw new RegraNegocioException("O profissional não executa o serviço");
		}
		return servico;
	}

	public record Consulta(UUID profissionalId, UUID servicoId, LocalDate data) {
	}

}
