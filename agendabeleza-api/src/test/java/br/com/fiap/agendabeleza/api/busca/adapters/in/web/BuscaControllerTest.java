package br.com.fiap.agendabeleza.api.busca.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.agendabeleza.api.busca.application.usecase.BuscarEstabelecimentosUseCase;
import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;

@WebMvcTest(BuscaController.class)
class BuscaControllerTest {

	private static final String ROTA = "/api/v1/busca/estabelecimentos";
	private static final UUID ESTABELECIMENTO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private BuscarEstabelecimentosUseCase buscar;

	private static Pagina<EstabelecimentoEncontrado> paginaComUmEstabelecimento() {
		return new Pagina<>(List.of(new EstabelecimentoEncontrado(ESTABELECIMENTO_ID, "Salão Bela Vista",
				"Consolação", "São Paulo", "SP", new BigDecimal("4.50"), 2)), 1, 10, 11);
	}

	private BuscarEstabelecimentosUseCase.Consulta consultaRecebida() {
		ArgumentCaptor<BuscarEstabelecimentosUseCase.Consulta> consulta =
				ArgumentCaptor.forClass(BuscarEstabelecimentosUseCase.Consulta.class);
		verify(buscar).executar(consulta.capture());
		return consulta.getValue();
	}

	@Test
	void devolveAPaginaDeEstabelecimentosEncontrados() throws Exception {
		given(buscar.executar(any())).willReturn(paginaComUmEstabelecimento());

		mvc.perform(get(ROTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens.length()").value(1))
				.andExpect(jsonPath("$.itens[0].id").value(ESTABELECIMENTO_ID.toString()))
				.andExpect(jsonPath("$.itens[0].nome").value("Salão Bela Vista"))
				.andExpect(jsonPath("$.itens[0].bairro").value("Consolação"))
				.andExpect(jsonPath("$.itens[0].cidade").value("São Paulo"))
				.andExpect(jsonPath("$.itens[0].uf").value("SP"))
				.andExpect(jsonPath("$.itens[0].notaMedia").value(4.5))
				.andExpect(jsonPath("$.itens[0].totalAvaliacoes").value(2))
				.andExpect(jsonPath("$.pagina").value(1))
				.andExpect(jsonPath("$.tamanho").value(10))
				.andExpect(jsonPath("$.totalDeItens").value(11))
				.andExpect(jsonPath("$.totalDePaginas").value(2));
	}

	@Test
	void repassaTodosOsFiltrosAoCasoDeUso() throws Exception {
		given(buscar.executar(any())).willReturn(paginaComUmEstabelecimento());

		mvc.perform(get(ROTA)
				.param("nome", "Bela")
				.param("cidade", "São Paulo")
				.param("bairro", "Consolação")
				.param("servico", "corte")
				.param("categoria", "CABELO")
				.param("notaMinima", "4")
				.param("precoMinimo", "50")
				.param("precoMaximo", "90.50")
				.param("pagina", "2")
				.param("tamanho", "5")
				.param("ordem", "NOME"))
				.andExpect(status().isOk());

		assertThat(consultaRecebida()).isEqualTo(new BuscarEstabelecimentosUseCase.Consulta("Bela", "São Paulo",
				"Consolação", "corte", CategoriaServico.CABELO, new BigDecimal("4"), new BigDecimal("50"),
				new BigDecimal("90.50"), null, null, null, 2, 5, OrdemDaBusca.NOME));
	}

	@Test
	void semParametrosDeixaOsPadroesParaOCasoDeUso() throws Exception {
		given(buscar.executar(any())).willReturn(paginaComUmEstabelecimento());

		mvc.perform(get(ROTA)).andExpect(status().isOk());

		assertThat(consultaRecebida()).isEqualTo(new BuscarEstabelecimentosUseCase.Consulta(null, null, null, null,
				null, null, null, null, null, null, null, null, null, null));
	}

	@Test
	void semResultadosDevolvePaginaVazia() throws Exception {
		given(buscar.executar(any())).willReturn(new Pagina<>(List.of(), 0, 20, 0));

		mvc.perform(get(ROTA).param("cidade", "Lugar Nenhum"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens").isEmpty())
				.andExpect(jsonPath("$.totalDeItens").value(0))
				.andExpect(jsonPath("$.totalDePaginas").value(0));
	}

	@ParameterizedTest(name = "{0}={1}")
	@CsvSource({
			"notaMinima, abc",
			"precoMinimo, barato",
			"categoria, PERFUMARIA",
			"ordem, PRECO",
			"pagina, primeira",
			"pagina, -1",
			"tamanho, 0",
			"tamanho, 51",
			"notaMinima, 5.5",
			"notaMinima, -1",
			"precoMinimo, -10",
			"precoMaximo, -0.01" })
	void parametroMalformadoOuForaDoLimiteResponde400(String parametro, String valor) throws Exception {
		mvc.perform(get(ROTA).param(parametro, valor))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(buscar);
	}

	@Test
	void faixaDePrecoInvertidaResponde422() throws Exception {
		given(buscar.executar(any())).willThrow(new RegraNegocioException(FiltroDaBusca.FAIXA_DE_PRECO_INVERTIDA));

		mvc.perform(get(ROTA).param("precoMinimo", "90").param("precoMaximo", "50"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(FiltroDaBusca.FAIXA_DE_PRECO_INVERTIDA));
	}

}
