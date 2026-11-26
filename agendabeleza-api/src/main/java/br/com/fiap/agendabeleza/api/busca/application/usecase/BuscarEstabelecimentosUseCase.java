package br.com.fiap.agendabeleza.api.busca.application.usecase;

import java.math.BigDecimal;
import java.util.Objects;

import br.com.fiap.agendabeleza.api.busca.application.port.CatalogoDeEstabelecimentos;
import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Paginacao;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;

/**
 * Busca pública e paginada de estabelecimentos com filtros combinados (funcionalidade 5). Filtro e paginação são
 * validados antes de consultar o banco.
 */
public class BuscarEstabelecimentosUseCase {

	private final CatalogoDeEstabelecimentos catalogo;

	public BuscarEstabelecimentosUseCase(CatalogoDeEstabelecimentos catalogo) {
		this.catalogo = catalogo;
	}

	public Pagina<EstabelecimentoEncontrado> executar(Consulta consulta) {
		FiltroDaBusca filtro = new FiltroDaBusca(consulta.nome(), consulta.cidade(), consulta.bairro(),
				consulta.servico(), consulta.categoria(), consulta.notaMinima(), consulta.precoMinimo(),
				consulta.precoMaximo());
		Paginacao paginacao = Paginacao.de(consulta.pagina(), consulta.tamanho());
		OrdemDaBusca ordem = Objects.requireNonNullElse(consulta.ordem(), OrdemDaBusca.PADRAO);
		return catalogo.buscar(filtro, paginacao, ordem);
	}

	/**
	 * Parâmetros da busca, todos opcionais.
	 *
	 * @param pagina  a partir de zero; nula é a primeira
	 * @param tamanho nulo usa {@link Paginacao#TAMANHO_PADRAO}
	 * @param ordem   nula usa {@link OrdemDaBusca#PADRAO}
	 */
	@SuppressWarnings("java:S107") // Um parâmetro por filtro da rota.
	public record Consulta(String nome, String cidade, String bairro, String servico, CategoriaServico categoria,
			BigDecimal notaMinima, BigDecimal precoMinimo, BigDecimal precoMaximo, Integer pagina, Integer tamanho,
			OrdemDaBusca ordem) {
	}

}
