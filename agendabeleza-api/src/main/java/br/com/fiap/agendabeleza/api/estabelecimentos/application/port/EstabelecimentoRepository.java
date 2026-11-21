package br.com.fiap.agendabeleza.api.estabelecimentos.application.port;

import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Porta de persistência do agregado {@link Estabelecimento}, incluindo fotos e horários.
 */
public interface EstabelecimentoRepository {

	Estabelecimento salvar(Estabelecimento estabelecimento);

	Optional<Estabelecimento> buscarPorId(UUID id);

}
