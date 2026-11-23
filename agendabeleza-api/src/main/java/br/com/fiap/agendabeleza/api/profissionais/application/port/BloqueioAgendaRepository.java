package br.com.fiap.agendabeleza.api.profissionais.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;

/**
 * Porta de persistência dos bloqueios de agenda dos profissionais.
 */
public interface BloqueioAgendaRepository {

	BloqueioAgenda salvar(BloqueioAgenda bloqueio);

	Optional<BloqueioAgenda> buscarPorId(UUID id);

	void remover(UUID id);

	/** Bloqueios do profissional que se sobrepõem ao período, ordenados pelo início. */
	List<BloqueioAgenda> listarNoPeriodo(UUID profissionalId, Intervalo periodo);

	/** Bloqueios do profissional que terminam depois de {@code agora}, ordenados pelo início. */
	List<BloqueioAgenda> listarVigentes(UUID profissionalId, Instant agora);

}
