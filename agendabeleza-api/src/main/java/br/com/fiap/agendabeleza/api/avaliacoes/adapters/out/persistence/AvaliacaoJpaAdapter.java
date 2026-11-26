package br.com.fiap.agendabeleza.api.avaliacoes.adapters.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.avaliacoes.application.port.AvaliacaoRepository;
import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;

/**
 * Implementa {@link AvaliacaoRepository} com JPA. As somas das notas são lidas no banco, e a média é calculada por
 * {@link NotaMedia#de}, com a mesma regra de arredondamento dos testes em memória.
 */
@Component
class AvaliacaoJpaAdapter implements AvaliacaoRepository {

	private final AvaliacaoJpaRepository repositorio;

	AvaliacaoJpaAdapter(AvaliacaoJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional
	public Avaliacao salvar(Avaliacao avaliacao) {
		repositorio.saveAndFlush(paraEntidade(avaliacao));
		return avaliacao;
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existeDoAgendamento(UUID agendamentoId) {
		return repositorio.existsByAgendamentoId(agendamentoId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Avaliacao> listarDoEstabelecimento(UUID estabelecimentoId) {
		return paraDominio(repositorio.findByEstabelecimentoIdOrderByCriadaEmDesc(estabelecimentoId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Avaliacao> listarDoProfissional(UUID profissionalId) {
		return paraDominio(repositorio.findByProfissionalIdOrderByCriadaEmDesc(profissionalId));
	}

	@Override
	@Transactional(readOnly = true)
	public NotaMedia notasDoEstabelecimento(UUID estabelecimentoId) {
		return repositorio.somarNotasDoEstabelecimento(estabelecimentoId).paraNotaMedia();
	}

	@Override
	@Transactional(readOnly = true)
	public NotaMedia notasDoProfissional(UUID profissionalId) {
		return repositorio.somarNotasDoProfissional(profissionalId).paraNotaMedia();
	}

	private static AvaliacaoJpaEntity paraEntidade(Avaliacao origem) {
		AvaliacaoJpaEntity entidade = new AvaliacaoJpaEntity();
		entidade.setId(origem.id());
		entidade.setAgendamentoId(origem.agendamentoId());
		entidade.setEstabelecimentoId(origem.estabelecimentoId());
		entidade.setProfissionalId(origem.profissionalId());
		entidade.setClienteId(origem.clienteId());
		entidade.setNotaEstabelecimento((short) origem.notaEstabelecimento());
		entidade.setNotaProfissional((short) origem.notaProfissional());
		entidade.setComentario(origem.comentario());
		entidade.setCriadaEm(origem.criadaEm());
		return entidade;
	}

	private static List<Avaliacao> paraDominio(List<AvaliacaoJpaEntity> entidades) {
		return entidades.stream()
				.map(entidade -> Avaliacao.reconstituir(entidade.getId(), entidade.getAgendamentoId(),
						entidade.getClienteId(), entidade.getEstabelecimentoId(), entidade.getProfissionalId(),
						entidade.getNotaEstabelecimento(), entidade.getNotaProfissional(), entidade.getComentario(),
						entidade.getCriadaEm()))
				.toList();
	}

}
