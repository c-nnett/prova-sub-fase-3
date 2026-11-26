package br.com.fiap.agendabeleza.api.profissionais.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

public class ProfissionalRepositoryEmMemoria implements ProfissionalRepository {

	private final Map<UUID, Profissional> dados = new LinkedHashMap<>();

	@Override
	public Profissional salvar(Profissional profissional) {
		dados.put(profissional.id(), profissional);
		return profissional;
	}

	@Override
	public Optional<Profissional> buscarPorId(UUID id) {
		return Optional.ofNullable(dados.get(id));
	}

	@Override
	public List<Profissional> listarPorEstabelecimento(UUID estabelecimentoId) {
		return dados.values().stream().filter(profissional -> profissional.pertenceA(estabelecimentoId)).toList();
	}

	@Override
	public void atualizarAvaliacoes(UUID profissionalId, NotaMedia notas) {
		Optional.ofNullable(dados.get(profissionalId))
				.ifPresent(profissional -> profissional.atualizarAvaliacoes(notas));
	}

}
