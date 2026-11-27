package br.com.fiap.agendabeleza.api.calendario.application.port;

import java.util.Optional;

import br.com.fiap.agendabeleza.api.calendario.domain.AssinaturaDoCalendario;

/**
 * Porta de persistência das assinaturas do feed.
 */
public interface AssinaturaCalendarioRepository {

	/** Grava a assinatura no lugar da anterior do mesmo papel e titular, se houver. */
	void salvar(AssinaturaDoCalendario assinatura);

	Optional<AssinaturaDoCalendario> buscarPorTokenHash(String tokenHash);

}
