package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;

/**
 * Cria um bloqueio na agenda do profissional. Gestor ou o próprio profissional. A recusa de bloqueio sobre
 * agendamento ativo entra junto com os agendamentos.
 */
public class CriarBloqueioUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;
	private final BloqueioAgendaRepository bloqueios;
	private final Clock relogio;

	public CriarBloqueioUseCase(EstabelecimentoRepository estabelecimentos, ProfissionalRepository profissionais,
			BloqueioAgendaRepository bloqueios, Clock relogio) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
		this.bloqueios = bloqueios;
		this.relogio = relogio;
	}

	public BloqueioAgenda executar(Comando comando) {
		ProfissionalAlteravel alteravel = ProfissionalAlteravel.obter(profissionais, estabelecimentos,
				comando.profissionalId(), comando.usuarioId());
		BloqueioAgenda bloqueio = BloqueioAgenda.criar(alteravel.profissional().id(), comando.inicio(),
				comando.fim(), comando.motivo(), relogio.instant());
		return bloqueios.salvar(bloqueio);
	}

	public record Comando(UUID profissionalId, UUID usuarioId, Instant inicio, Instant fim, String motivo) {
	}

}
