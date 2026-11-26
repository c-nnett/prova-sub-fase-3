package br.com.fiap.agendabeleza.api.busca.application.port;

import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Paginacao;

/**
 * Consulta somente de leitura sobre os cadastros de estabelecimentos, serviços e profissionais. Os filtros são
 * aplicados no banco, para que a paginação e o total contem só o que atende a todos eles.
 */
public interface CatalogoDeEstabelecimentos {

	Pagina<EstabelecimentoEncontrado> buscar(FiltroDaBusca filtro, Paginacao paginacao, OrdemDaBusca ordem);

}
