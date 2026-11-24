package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.application.port.ExecucaoSerializada;
import br.com.fiap.agendabeleza.api.compartilhado.domain.ConflitoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.AgendamentosAtivos;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;

/**
 * Cria um bloqueio na agenda do profissional. Gestor ou o próprio profissional. Recusa o bloqueio com
 * {@link ConflitoException} se o profissional tiver agendamento ativo no trecho do período que ainda não passou.
 * <p>
 * A verificação e a gravação rodam em {@link ExecucaoSerializada}, uma reserva ou bloqueio de cada vez por
 * profissional, para que uma reserva simultânea no mesmo período não passe junto.
 */
public class CriarBloqueioUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;
	private final BloqueioAgendaRepository bloqueios;
	private final AgendamentosAtivos agendamentosAtivos;
	private final ExecucaoSerializada execucao;
	private final Clock relogio;

	public CriarBloqueioUseCase(EstabelecimentoRepository estabelecimentos, ProfissionalRepository profissionais,
			BloqueioAgendaRepository bloqueios, AgendamentosAtivos agendamentosAtivos, ExecucaoSerializada execucao,
			Clock relogio) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
		this.bloqueios = bloqueios;
		this.agendamentosAtivos = agendamentosAtivos;
		this.execucao = execucao;
		this.relogio = relogio;
	}

	public BloqueioAgenda executar(Comando comando) {
		return execucao.executar(comando.profissionalId(), () -> bloquear(comando));
	}

	private BloqueioAgenda bloquear(Comando comando) {
		ProfissionalAlteravel alteravel = ProfissionalAlteravel.obter(profissionais, estabelecimentos,
				comando.profissionalId(), comando.usuarioId());
		Instant agora = relogio.instant();
		BloqueioAgenda bloqueio = BloqueioAgenda.criar(alteravel.profissional().id(), comando.inicio(),
				comando.fim(), comando.motivo(), agora);
		if (agendamentosAtivos.existemNoPeriodo(bloqueio.profissionalId(), trechoFuturo(bloqueio.periodo(), agora))) {
			throw new ConflitoException("Há agendamento ativo no período do bloqueio");
		}
		return bloqueios.salvar(bloqueio);
	}

	/**
	 * Parte do período a partir de agora. Agendamentos que já terminaram e nunca foram concluídos continuam ativos,
	 * mas não impedem um bloqueio registrado com início retroativo. {@link BloqueioAgenda#criar} já garante que o
	 * fim está no futuro.
	 */
	private static Intervalo trechoFuturo(Intervalo periodo, Instant agora) {
		return periodo.inicio().isBefore(agora) ? new Intervalo(agora, periodo.fim()) : periodo;
	}

	public record Comando(UUID profissionalId, UUID usuarioId, Instant inicio, Instant fim, String motivo) {
	}

}
