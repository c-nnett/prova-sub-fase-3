package br.com.fiap.agendabeleza.api.estabelecimentos.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

public class EstabelecimentoRepositoryEmMemoria implements EstabelecimentoRepository {

	private final Map<UUID, Estabelecimento> dados = new HashMap<>();
	private int salvamentos;

	@Override
	public Estabelecimento salvar(Estabelecimento estabelecimento) {
		dados.put(estabelecimento.id(), estabelecimento);
		salvamentos++;
		return estabelecimento;
	}

	@Override
	public Optional<Estabelecimento> buscarPorId(UUID id) {
		return Optional.ofNullable(dados.get(id));
	}

	/** Como o adaptador JPA, altera só as notas, sem contar como salvamento. */
	@Override
	public void atualizarAvaliacoes(UUID estabelecimentoId, NotaMedia notas) {
		Optional.ofNullable(dados.get(estabelecimentoId))
				.ifPresent(estabelecimento -> estabelecimento.atualizarAvaliacoes(notas));
	}

	public int salvamentos() {
		return salvamentos;
	}

}
