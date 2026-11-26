package br.com.fiap.agendabeleza.api.busca.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.busca.application.CatalogoDeEstabelecimentosEmMemoria;
import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.JanelaDeDisponibilidade;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Paginacao;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;

class BuscarEstabelecimentosUseCaseTest {

	/** 04/03/2030, 01h30 em UTC: ainda 03/03 em São Paulo, para provar que "hoje" usa o fuso padrão. */
	private static final Instant AGORA = Instant.parse("2030-03-04T01:30:00Z");
	private static final LocalDate HOJE_EM_SAO_PAULO = LocalDate.of(2030, 3, 3);
	private static final LocalDate SEXTA = LocalDate.of(2030, 3, 8);

	private CatalogoDeEstabelecimentosEmMemoria catalogo;
	private BuscarEstabelecimentosUseCase buscar;

	@BeforeEach
	void prepararCatalogo() {
		catalogo = new CatalogoDeEstabelecimentosEmMemoria();
		buscar = new BuscarEstabelecimentosUseCase(catalogo, Clock.fixed(AGORA, ZoneOffset.UTC));
	}

	private static EstabelecimentoEncontrado encontrado(String nome) {
		return new EstabelecimentoEncontrado(UUID.randomUUID(), nome, "Consolação", "São Paulo", "SP",
				new BigDecimal("4.50"), 2);
	}

	private static BuscarEstabelecimentosUseCase.Consulta semFiltros(Integer pagina, Integer tamanho) {
		return new BuscarEstabelecimentosUseCase.Consulta(null, null, null, null, null, null, null, null, null, null,
				null, pagina, tamanho, null);
	}

	private static BuscarEstabelecimentosUseCase.Consulta comPrecos(String precoMinimo, String precoMaximo) {
		return new BuscarEstabelecimentosUseCase.Consulta(null, null, null, null, null, null,
				new BigDecimal(precoMinimo), new BigDecimal(precoMaximo), null, null, null, null, null, null);
	}

	private static BuscarEstabelecimentosUseCase.Consulta disponivelEm(LocalDate data, LocalTime horaInicio,
			LocalTime horaFim) {
		return new BuscarEstabelecimentosUseCase.Consulta(null, null, null, null, null, null, null, null, data,
				horaInicio, horaFim, null, null, null);
	}

	@Test
	void repassaOsFiltrosNormalizadosAoCatalogo() {
		buscar.executar(new BuscarEstabelecimentosUseCase.Consulta(" Bela ", "São Paulo", "Consolação", "corte",
				CategoriaServico.CABELO, new BigDecimal("4"), new BigDecimal("50"), new BigDecimal("90"), SEXTA,
				LocalTime.of(14, 0), LocalTime.of(18, 0), 1, 10, OrdemDaBusca.NOME));

		assertThat(catalogo.ultimoFiltro()).isEqualTo(new FiltroDaBusca("Bela", "São Paulo", "Consolação", "corte",
				CategoriaServico.CABELO, new BigDecimal("4"), new BigDecimal("50"), new BigDecimal("90"),
				new JanelaDeDisponibilidade(SEXTA, LocalTime.of(14, 0), LocalTime.of(18, 0))));
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
	void semDataNaoFiltraPorDisponibilidade() {
		buscar.executar(semFiltros(null, null));

		assertThat(catalogo.ultimoFiltro().disponibilidade()).isNull();
	}

	@Test
	void hojeSegueOFusoPadraoDosEstabelecimentos() {
		buscar.executar(disponivelEm(HOJE_EM_SAO_PAULO, null, null));

		assertThat(catalogo.ultimoFiltro().disponibilidade().data()).isEqualTo(HOJE_EM_SAO_PAULO);
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
		BuscarEstabelecimentosUseCase.Consulta consulta = comPrecos("90", "50");

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

	@Test
	void dataNoPassadoNaoChegaAoCatalogo() {
		BuscarEstabelecimentosUseCase.Consulta consulta = disponivelEm(HOJE_EM_SAO_PAULO.minusDays(1), null, null);

		assertThatThrownBy(() -> buscar.executar(consulta))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(JanelaDeDisponibilidade.DATA_NO_PASSADO);
		assertThat(catalogo.foiConsultado()).isFalse();
	}

	@Test
	void horaSemDataNaoChegaAoCatalogo() {
		BuscarEstabelecimentosUseCase.Consulta consulta = disponivelEm(null, LocalTime.of(14, 0), null);

		assertThatThrownBy(() -> buscar.executar(consulta))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(JanelaDeDisponibilidade.HORA_SEM_DATA);
		assertThat(catalogo.foiConsultado()).isFalse();
	}

}
