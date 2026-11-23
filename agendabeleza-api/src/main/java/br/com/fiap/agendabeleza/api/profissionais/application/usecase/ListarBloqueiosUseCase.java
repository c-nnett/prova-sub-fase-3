package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;

/**
 * Lista os bloqueios do profissional que ainda não terminaram, ordenados pelo início. Gestor ou o próprio
 * profissional, porque o motivo pode ser pessoal.
 */
public class ListarBloqueiosUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;
	private final BloqueioAgendaRepository bloqueios;
	private final Clock relogio;

	public ListarBloqueiosUseCase(EstabelecimentoRepository estabelecimentos, ProfissionalRepository profissionais,
			BloqueioAgendaRepository bloqueios, Clock relogio) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
		this.bloqueios = bloqueios;
		this.relogio = relogio;
	}

	public List<BloqueioAgenda> executar(UUID profissionalId, UUID usuarioId) {
		ProfissionalAlteravel.obter(profissionais, estabelecimentos, profissionalId, usuarioId);
		return bloqueios.listarVigentes(profissionalId, relogio.instant());
	}

}
