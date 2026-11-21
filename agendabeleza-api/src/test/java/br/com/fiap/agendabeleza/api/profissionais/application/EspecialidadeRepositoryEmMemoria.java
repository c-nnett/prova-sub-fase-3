package br.com.fiap.agendabeleza.api.profissionais.application;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.profissionais.application.port.EspecialidadeRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;

public class EspecialidadeRepositoryEmMemoria implements EspecialidadeRepository {

	private final Map<UUID, Especialidade> dados = new LinkedHashMap<>();
	private int buscasPorIds;

	/** Simula o catálogo carregado pela migração. */
	public Especialidade adicionar(String nome) {
		Especialidade especialidade = Especialidade.cadastrar(nome);
		dados.put(especialidade.id(), especialidade);
		return especialidade;
	}

	@Override
	public List<Especialidade> buscarPorIds(Collection<UUID> ids) {
		buscasPorIds++;
		return ids.stream().distinct().filter(dados::containsKey).map(dados::get).toList();
	}

	@Override
	public List<Especialidade> listarTodas() {
		return List.copyOf(dados.values());
	}

	public int buscasPorIds() {
		return buscasPorIds;
	}

}
