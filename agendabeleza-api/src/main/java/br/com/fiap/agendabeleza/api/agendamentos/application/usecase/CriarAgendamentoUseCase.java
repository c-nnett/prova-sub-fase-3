package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisoDoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisosDoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.compartilhado.application.port.ExecucaoSerializada;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Reserva um horário com um profissional. O estabelecimento vem do profissional, e o preço é a tarifa dele para o
 * serviço, congelada no agendamento.
 * <p>
 * As verificações e a gravação rodam em {@link ExecucaoSerializada}, uma reserva ou bloqueio de cada vez por
 * profissional. Assim, uma reserva e um bloqueio simultâneos não passam juntos pelas verificações. A restrição
 * {@code agendamento_sem_conflito} continua como garantia final entre reservas, e o adaptador a traduz para
 * {@link HorarioIndisponivelException}.
 * <p>
 * Os avisos de confirmação para cliente e profissional são pedidos dentro da mesma execução, depois da gravação:
 * entram na outbox na mesma transação e só saem por e-mail depois do commit. Uma reserva recusada não avisa ninguém.
 */
public class CriarAgendamentoUseCase {

	private final AgendamentoRepository agendamentos;
	private final ProfissionalRepository profissionais;
	private final ServicoRepository servicos;
	private final EstabelecimentoRepository estabelecimentos;
	private final DisponibilidadeDaAgenda disponibilidade;
	private final ExecucaoSerializada execucao;
	private final AvisosDoAgendamento avisos;
	private final Clock relogio;

	@SuppressWarnings("java:S107") // Uma porta por fonte de dados da agenda, injetadas pelo construtor.
	public CriarAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			ServicoRepository servicos, EstabelecimentoRepository estabelecimentos, BloqueioAgendaRepository bloqueios,
			ExecucaoSerializada execucao, AvisosDoAgendamento avisos, Clock relogio) {
		this.agendamentos = agendamentos;
		this.profissionais = profissionais;
		this.servicos = servicos;
		this.estabelecimentos = estabelecimentos;
		this.disponibilidade = new DisponibilidadeDaAgenda(agendamentos, bloqueios);
		this.execucao = execucao;
		this.avisos = avisos;
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
		disponibilidade.garantirHorarioLivre(agendamento.intervalo(), profissional, estabelecimento, agora);
		Agendamento gravado = agendamentos.salvar(agendamento);
		avisos.reservado(AvisoDoAgendamento.de(gravado, profissional, estabelecimento), comando.emailCliente());
		return gravado;
	}

	private Profissional profissionalAtivo(UUID profissionalId) {
		Profissional profissional = profissionais.buscarPorId(profissionalId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Profissional " + profissionalId + " não encontrado"));
		DisponibilidadeDaAgenda.exigirProfissionalAtivo(profissional);
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
		DisponibilidadeDaAgenda.exigirEstabelecimentoAtivo(estabelecimento);
		return estabelecimento;
	}

	/** @param emailCliente destino dos avisos ao cliente; não há cadastro de usuários de onde tirá-lo */
	public record Comando(
			UUID clienteId,
			String emailCliente,
			UUID profissionalId,
			UUID servicoId,
			Instant inicio,
			String observacoes) {
	}

}
