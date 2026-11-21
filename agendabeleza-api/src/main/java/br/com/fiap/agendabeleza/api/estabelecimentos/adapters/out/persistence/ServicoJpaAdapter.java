package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

/**
 * Implementa {@link ServicoRepository} com JPA.
 */
@Component
class ServicoJpaAdapter implements ServicoRepository {

	private final ServicoJpaRepository repositorio;

	ServicoJpaAdapter(ServicoJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional
	public Servico salvar(Servico servico) {
		repositorio.save(paraEntidade(servico));
		return servico;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Servico> buscarPorId(UUID id) {
		return repositorio.findById(id).map(ServicoJpaAdapter::paraDominio);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Servico> listarPorEstabelecimento(UUID estabelecimentoId) {
		return repositorio.findByEstabelecimentoIdOrderByNome(estabelecimentoId).stream()
				.map(ServicoJpaAdapter::paraDominio)
				.toList();
	}

	private static ServicoJpaEntity paraEntidade(Servico origem) {
		ServicoJpaEntity entidade = new ServicoJpaEntity();
		entidade.setId(origem.id());
		entidade.setEstabelecimentoId(origem.estabelecimentoId());
		entidade.setNome(origem.nome());
		entidade.setDescricao(origem.descricao());
		entidade.setCategoria(origem.categoria());
		entidade.setDuracaoMinutos(origem.duracaoMinutos());
		entidade.setPrecoBase(origem.precoBase());
		entidade.setAtivo(origem.ativo());
		return entidade;
	}

	private static Servico paraDominio(ServicoJpaEntity entidade) {
		return Servico.reconstituir(entidade.getId(), entidade.getEstabelecimentoId(), entidade.getNome(),
				entidade.getDescricao(), entidade.getCategoria(), entidade.getDuracaoMinutos(),
				entidade.getPrecoBase(), entidade.isAtivo());
	}

}
