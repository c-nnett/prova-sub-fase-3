package br.com.fiap.agendabeleza.api.estabelecimentos.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

public class ServicoRepositoryEmMemoria implements ServicoRepository {

	private final Map<UUID, Servico> dados = new LinkedHashMap<>();

	@Override
	public Servico salvar(Servico servico) {
		dados.put(servico.id(), servico);
		return servico;
	}

	@Override
	public Optional<Servico> buscarPorId(UUID id) {
		return Optional.ofNullable(dados.get(id));
	}

	@Override
	public List<Servico> listarPorEstabelecimento(UUID estabelecimentoId) {
		return dados.values().stream().filter(servico -> servico.pertenceA(estabelecimentoId)).toList();
	}

}
