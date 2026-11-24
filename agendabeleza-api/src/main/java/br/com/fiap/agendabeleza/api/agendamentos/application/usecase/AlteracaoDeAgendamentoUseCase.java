package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * Fluxo comum das ações de gestão sobre um agendamento existente (Template Method): carregar o agendamento e as
 * partes dele (404), exigir a permissão da ação (403), aplicar a ação do domínio (422 se a transição não for
 * permitida) e gravar, com a linha de histórico (409 se outra operação gravou o agendamento antes).
 * <p>
 * Cada subclasse informa a permissão e a ação. A gravação devolve o agendamento como ficou gravado, e é ele que
 * segue para o chamador, conforme o contrato de {@link AgendamentoRepository#salvar}.
 *
 * @param <C> comando da ação
 */
public abstract class AlteracaoDeAgendamentoUseCase<C extends ComandoDeAlteracao> {

	private final AgendamentoRepository agendamentos;
	private final ProfissionalRepository profissionais;
	private final EstabelecimentoRepository estabelecimentos;
	private final Clock relogio;
	private final PermissaoNoAgendamento permissao;
	private final String acao;

	/**
	 * @param acao verbo da operação, usado na mensagem de acesso negado
	 */
	AlteracaoDeAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, Clock relogio, PermissaoNoAgendamento permissao, String acao) {
		this.agendamentos = agendamentos;
		this.profissionais = profissionais;
		this.estabelecimentos = estabelecimentos;
		this.relogio = relogio;
		this.permissao = permissao;
		this.acao = acao;
	}

	public final Agendamento executar(C comando) {
		PartesDoAgendamento partes = PartesDoAgendamento.carregar(agendamentos, profissionais, estabelecimentos,
				comando.agendamentoId());
		permissao.exigir(partes, comando.usuarioSolicitante(), acao);
		Agendamento agendamento = partes.agendamento();
		return executarNaAgenda(agendamento.profissionalId(), () -> {
			aplicar(partes, comando, relogio.instant());
			return agendamentos.salvar(agendamento);
		});
	}

	/** Aplica a ação do domínio sobre {@code partes.agendamento()}, que será gravado em seguida. */
	abstract void aplicar(PartesDoAgendamento partes, C comando, Instant agora);

	/**
	 * Gancho para as ações que verificam a agenda do profissional antes de gravar. Por padrão, executa direto: o
	 * bloqueio otimista do agendamento já impede que duas alterações do mesmo registro passem juntas.
	 */
	<T> T executarNaAgenda(UUID profissionalId, Supplier<T> alteracao) {
		return alteracao.get();
	}

}
