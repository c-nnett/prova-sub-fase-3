package br.com.fiap.agendabeleza.api.busca.application;

import java.util.ArrayList;
import java.util.List;

import br.com.fiap.agendabeleza.api.busca.application.port.CatalogoDeEstabelecimentos;
import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Paginacao;

/**
 * Catálogo de teste: os filtros são aplicados pelo banco (ver {@code BuscaPersistenciaTest}), então aqui só se guarda
 * o que o caso de uso pediu e se devolve a página dos estabelecimentos cadastrados no teste.
 */
public class CatalogoDeEstabelecimentosEmMemoria implements CatalogoDeEstabelecimentos {

	private final List<EstabelecimentoEncontrado> estabelecimentos = new ArrayList<>();
	private FiltroDaBusca ultimoFiltro;
	private Paginacao ultimaPaginacao;
	private OrdemDaBusca ultimaOrdem;

	public void cadastrar(EstabelecimentoEncontrado estabelecimento) {
		estabelecimentos.add(estabelecimento);
	}

	@Override
	public Pagina<EstabelecimentoEncontrado> buscar(FiltroDaBusca filtro, Paginacao paginacao, OrdemDaBusca ordem) {
		ultimoFiltro = filtro;
		ultimaPaginacao = paginacao;
		ultimaOrdem = ordem;
		List<EstabelecimentoEncontrado> itensDaPagina = estabelecimentos.stream()
				.skip(paginacao.deslocamento())
				.limit(paginacao.tamanho())
				.toList();
		return Pagina.de(itensDaPagina, paginacao, estabelecimentos.size());
	}

	public FiltroDaBusca ultimoFiltro() {
		return ultimoFiltro;
	}

	public Paginacao ultimaPaginacao() {
		return ultimaPaginacao;
	}

	public OrdemDaBusca ultimaOrdem() {
		return ultimaOrdem;
	}

	public boolean foiConsultado() {
		return ultimoFiltro != null;
	}

}
