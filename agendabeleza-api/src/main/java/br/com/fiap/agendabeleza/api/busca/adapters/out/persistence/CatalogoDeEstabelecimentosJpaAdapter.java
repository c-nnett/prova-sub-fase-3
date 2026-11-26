package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.busca.application.port.CatalogoDeEstabelecimentos;
import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Paginacao;

/**
 * Implementa {@link CatalogoDeEstabelecimentos} com {@code Specification} e paginação do Spring Data: a página e o
 * total vêm de duas consultas com os mesmos filtros.
 */
@Component
class CatalogoDeEstabelecimentosJpaAdapter implements CatalogoDeEstabelecimentos {

	private static final Sort.Order POR_NOME = Sort.Order.asc("nome");
	private static final Sort.Order DESEMPATE = Sort.Order.asc("id");

	private final EstabelecimentoDaBuscaJpaRepository repositorio;

	CatalogoDeEstabelecimentosJpaAdapter(EstabelecimentoDaBuscaJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public Pagina<EstabelecimentoEncontrado> buscar(FiltroDaBusca filtro, Paginacao paginacao, OrdemDaBusca ordem) {
		PageRequest pedido = PageRequest.of(paginacao.numero(), paginacao.tamanho(), ordenacao(ordem));
		Page<EstabelecimentoDaBuscaJpaEntity> pagina = repositorio.findAll(BuscaEspecificacoes.de(filtro), pedido);
		return Pagina.de(pagina.map(EstabelecimentoDaBuscaJpaEntity::paraDominio).getContent(), paginacao,
				pagina.getTotalElements());
	}

	/** Toda ordem termina no identificador, para que a mesma consulta devolva sempre as mesmas páginas. */
	private static Sort ordenacao(OrdemDaBusca ordem) {
		return switch (ordem) {
			case MELHOR_AVALIADOS -> Sort.by(Sort.Order.desc("notaMedia").nullsLast(), POR_NOME, DESEMPATE);
			case NOME -> Sort.by(POR_NOME, DESEMPATE);
		};
	}

}
