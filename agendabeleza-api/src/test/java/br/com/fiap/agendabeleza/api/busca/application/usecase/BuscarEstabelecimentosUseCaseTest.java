package br.com.fiap.agendabeleza.api.busca.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.busca.application.CatalogoDeEstabelecimentosEmMemoria;
import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Paginacao;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;

class BuscarEstabelecimentosUseCaseTest {

	private CatalogoDeEstabelecimentosEmMemoria catalogo;
	private BuscarEstabelecimentosUseCase buscar;

	@BeforeEach
	void prepararCatalogo() {
		catalogo = new CatalogoDeEstabelecimentosEmMemoria();
		buscar = new BuscarEstabelecimentosUseCase(catalogo);
	}

	private static EstabelecimentoEncontrado encontrado(String nome) {
		return new EstabelecimentoEncontrado(UUID.randomUUID(), nome, "Consolação", "São Paulo", "SP",
				new BigDecimal("4.50"), 2);
	}

	private static BuscarEstabelecimentosUseCase.Consulta semFiltros(Integer pagina, Integer tamanho) {
		return new BuscarEstabelecimentosUseCase.Consulta(null, null, null, null, null, null, null, null, pagina,
				tamanho, null);
	}

	@Test
	void repassaOsFiltrosNormalizadosAoCatalogo() {
		buscar.executar(new BuscarEstabelecimentosUseCase.Consulta(" Bela ", "São Paulo", "Consolação", "corte",
				CategoriaServico.CABELO, new BigDecimal("4"), new BigDecimal("50"), new BigDecimal("90"), 1, 10,
				OrdemDaBusca.NOME));

		assertThat(catalogo.ultimoFiltro()).isEqualTo(new FiltroDaBusca("Bela", "São Paulo", "Consolação", "corte",
				CategoriaServico.CABELO, new BigDecimal("4"), new BigDecimal("50"), new BigDecimal("90")));
		assertThat(catalogo.ultimaPaginacao()).isEqualTo(Paginacao.de(1, 10));
		assertThat(catalogo.ultimaOrdem()).isEqualTo(OrdemDaBusca.NOME);
	}

	@Test
	void semPaginacaoNemOrdemUsaOsPadroes() {
		buscar.executar(semFiltros(null, null));

		assertThat(catalogo.ultimaPaginacao()).isEqualTo(Paginacao.de(0, Paginacao.TAMANHO_PADRAO));
		assertThat(catalogo.ultimaOrdem()).isEqualTo(OrdemDaBusca.PADRAO);
		assertThat(OrdemDaBusca.PADRAO).isEqualTo(OrdemDaBusca.MELHOR_AVALIADOS);
	}

	@Test
	void devolveAPaginaDoCatalogo() {
		catalogo.cadastrar(encontrado("Salão Ana"));
		catalogo.cadastrar(encontrado("Salão Bia"));
		catalogo.cadastrar(encontrado("Salão Cris"));

		Pagina<EstabelecimentoEncontrado> pagina = buscar.executar(semFiltros(1, 2));

		assertThat(pagina.itens()).extracting(EstabelecimentoEncontrado::nome).containsExactly("Salão Cris");
		assertThat(pagina.totalDeItens()).isEqualTo(3);
		assertThat(pagina.totalDePaginas()).isEqualTo(2);
	}

	@Test
	void faixaDePrecoInvertidaNaoChegaAoCatalogo() {
		BuscarEstabelecimentosUseCase.Consulta consulta = new BuscarEstabelecimentosUseCase.Consulta(null, null, null,
				null, null, null, new BigDecimal("90"), new BigDecimal("50"), null, null, null);

		assertThatThrownBy(() -> buscar.executar(consulta))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(FiltroDaBusca.FAIXA_DE_PRECO_INVERTIDA);
		assertThat(catalogo.foiConsultado()).isFalse();
	}

	@Test
	void paginacaoInvalidaNaoChegaAoCatalogo() {
		BuscarEstabelecimentosUseCase.Consulta consulta = semFiltros(0, Paginacao.TAMANHO_MAXIMO + 1);

		assertThatThrownBy(() -> buscar.executar(consulta))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(Paginacao.TAMANHO_FORA_DO_LIMITE);
		assertThat(catalogo.foiConsultado()).isFalse();
	}

}
