package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import br.com.fiap.agendabeleza.api.busca.application.port.CatalogoDeEstabelecimentos;
import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Paginacao;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Endereco;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

/**
 * Busca de estabelecimentos no PostgreSQL do Testcontainers. O banco é compartilhado pelas classes de teste, então
 * cada teste cadastra numa cidade própria e sempre filtra por ela.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BuscaPersistenciaTest {

	private static final Paginacao PRIMEIRA_PAGINA = Paginacao.de(0, 10);

	@Autowired
	private CatalogoDeEstabelecimentos catalogo;

	@Autowired
	private EstabelecimentoRepository estabelecimentos;

	@Autowired
	private ServicoRepository servicos;

	@Autowired
	private ProfissionalRepository profissionais;

	private String cidade;

	@BeforeEach
	void escolherCidadeDoTeste() {
		cidade = "Cidade " + UUID.randomUUID();
	}

	private Estabelecimento estabelecimento(String nome, String bairro) {
		return estabelecimentos.salvar(Estabelecimento.cadastrar(UUID.randomUUID(), nome, null, endereco(bairro),
				null));
	}

	private Estabelecimento estabelecimentoInativo(String nome) {
		Estabelecimento cadastrado = estabelecimento(nome, "Centro");
		return estabelecimentos.salvar(Estabelecimento.reconstituir(cadastrado.id(), cadastrado.gestorId(),
				cadastrado.nome(), null, endereco("Centro"), null, null, 0, false, List.of(), List.of()));
	}

	private Endereco endereco(String bairro) {
		return new Endereco("Rua Augusta", "100", null, bairro, cidade, "SP", "01305000");
	}

	private Estabelecimento avaliado(String nome, long somaDasNotas, int total) {
		Estabelecimento cadastrado = estabelecimento(nome, "Centro");
		estabelecimentos.atualizarAvaliacoes(cadastrado.id(), NotaMedia.de(somaDasNotas, total));
		return cadastrado;
	}

	private Servico servico(Estabelecimento estabelecimento, String nome, CategoriaServico categoria) {
		return servicos.salvar(Servico.cadastrar(estabelecimento.id(), nome, null, categoria, 30,
				new BigDecimal("70.00")));
	}

	private Servico servicoInativo(Estabelecimento estabelecimento, String nome, CategoriaServico categoria) {
		Servico cadastrado = servico(estabelecimento, nome, categoria);
		return servicos.salvar(Servico.reconstituir(cadastrado.id(), estabelecimento.id(), nome, null, categoria, 30,
				new BigDecimal("70.00"), false));
	}

	private Profissional profissional(Estabelecimento estabelecimento, boolean ativo, Servico servico,
			String tarifa) {
		Profissional profissional = Profissional.cadastrar(estabelecimento.id(), null, "Ana", "ana@salao.com", null);
		profissional.atualizarPerfil("Ana", "ana@salao.com", null, ativo);
		profissional.definirServicos(List.of(new ServicoProfissional(servico.id(), new BigDecimal(tarifa))));
		return profissionais.salvar(profissional);
	}

	private FiltroDaBusca naCidade() {
		return filtro(null, null, null, null, null, null);
	}

	private FiltroDaBusca filtro(String nome, String bairro, String servico, CategoriaServico categoria,
			String precoMinimo, String precoMaximo) {
		return new FiltroDaBusca(nome, cidade, bairro, servico, categoria, null, decimal(precoMinimo),
				decimal(precoMaximo), null);
	}

	private FiltroDaBusca comNotaMinima(String notaMinima) {
		return new FiltroDaBusca(null, cidade, null, null, null, new BigDecimal(notaMinima), null, null, null);
	}

	private static BigDecimal decimal(String valor) {
		return valor == null ? null : new BigDecimal(valor);
	}

	private List<String> nomesEncontrados(FiltroDaBusca filtro) {
		return nomes(catalogo.buscar(filtro, PRIMEIRA_PAGINA, OrdemDaBusca.NOME));
	}

	private static List<String> nomes(Pagina<EstabelecimentoEncontrado> pagina) {
		return pagina.itens().stream().map(EstabelecimentoEncontrado::nome).toList();
	}

	@Test
	void devolveOResumoDosEstabelecimentosAtivosDaCidade() {
		Estabelecimento bela = avaliado("Salão Bela Vista", 9, 2);
		estabelecimentoInativo("Salão Fechado");

		Pagina<EstabelecimentoEncontrado> pagina = catalogo.buscar(naCidade(), PRIMEIRA_PAGINA, OrdemDaBusca.NOME);

		assertThat(pagina.itens()).containsExactly(new EstabelecimentoEncontrado(bela.id(), "Salão Bela Vista",
				"Centro", cidade, "SP", new BigDecimal("4.50"), 2));
		assertThat(pagina.totalDeItens()).isEqualTo(1);
	}

	@Test
	void cidadeEBairroNaoDiferenciamMaiusculas() {
		estabelecimento("Salão Augusta", "Consolação");
		estabelecimento("Salão Paulista", "Bela Vista");

		FiltroDaBusca filtro = new FiltroDaBusca(null, cidade.toUpperCase(), "CONSOLAÇÃO", null, null, null, null,
				null, null);

		assertThat(nomesEncontrados(filtro)).containsExactly("Salão Augusta");
	}

	@Test
	void nomeBuscaPorTrechoSemDiferenciarMaiusculas() {
		estabelecimento("Salão Bela Vista", "Centro");
		estabelecimento("Barbearia do Zé", "Centro");

		assertThat(nomesEncontrados(filtro("bela", null, null, null, null, null)))
				.containsExactly("Salão Bela Vista");
	}

	@Test
	void caracteresCuringaDoLikeSaoTratadosComoTexto() {
		estabelecimento("Studio 100% Beleza", "Centro");
		estabelecimento("Studio 1000 Beleza", "Centro");
		estabelecimento("Bar_bearia", "Centro");
		estabelecimento("Barbearia", "Centro");

		assertThat(nomesEncontrados(filtro("100%", null, null, null, null, null)))
				.containsExactly("Studio 100% Beleza");
		assertThat(nomesEncontrados(filtro("r_b", null, null, null, null, null))).containsExactly("Bar_bearia");
	}

	@Test
	void notaMinimaDeixaDeForaOsSemAvaliacao() {
		avaliado("Salão Cinco", 5, 1);
		avaliado("Salão Três", 3, 1);
		estabelecimento("Salão Novo", "Centro");

		assertThat(nomesEncontrados(comNotaMinima("4"))).containsExactly("Salão Cinco");
		assertThat(nomesEncontrados(comNotaMinima("3"))).containsExactly("Salão Cinco", "Salão Três");
		assertThat(nomesEncontrados(comNotaMinima("0"))).containsExactly("Salão Cinco", "Salão Três");
	}

	@Test
	void servicoECategoriaExigemUmServicoAtivo() {
		Estabelecimento cabelo = estabelecimento("Salão Cabelo", "Centro");
		servico(cabelo, "Corte feminino", CategoriaServico.CABELO);
		Estabelecimento unhas = estabelecimento("Espaço Unhas", "Centro");
		servico(unhas, "Manicure", CategoriaServico.UNHAS);
		servicoInativo(unhas, "Corte infantil", CategoriaServico.CABELO);

		assertThat(nomesEncontrados(filtro(null, null, "CORTE", null, null, null))).containsExactly("Salão Cabelo");
		assertThat(nomesEncontrados(filtro(null, null, null, CategoriaServico.UNHAS, null, null)))
				.containsExactly("Espaço Unhas");
		assertThat(nomesEncontrados(filtro(null, null, null, CategoriaServico.CABELO, null, null)))
				.containsExactly("Salão Cabelo");
	}

	@Test
	void faixaDePrecoUsaATarifaDeUmProfissionalAtivo() {
		Estabelecimento barato = estabelecimento("Salão Barato", "Centro");
		profissional(barato, true, servico(barato, "Corte", CategoriaServico.CABELO), "40.00");
		Estabelecimento caro = estabelecimento("Salão Caro", "Centro");
		profissional(caro, true, servico(caro, "Corte", CategoriaServico.CABELO), "150.00");
		Estabelecimento inativo = estabelecimento("Salão Sem Equipe", "Centro");
		profissional(inativo, false, servico(inativo, "Corte", CategoriaServico.CABELO), "40.00");
		estabelecimento("Salão Sem Serviço", "Centro");

		assertThat(nomesEncontrados(filtro(null, null, null, null, null, "50"))).containsExactly("Salão Barato");
		assertThat(nomesEncontrados(filtro(null, null, null, null, "100", null))).containsExactly("Salão Caro");
		assertThat(nomesEncontrados(filtro(null, null, null, null, "40", "150")))
				.containsExactly("Salão Barato", "Salão Caro");
	}

	@Test
	void servicoEPrecoValemParaOMesmoServico() {
		Estabelecimento salao = estabelecimento("Salão Completo", "Centro");
		profissional(salao, true, servico(salao, "Corte", CategoriaServico.CABELO), "120.00");
		profissional(salao, true, servico(salao, "Manicure", CategoriaServico.UNHAS), "40.00");

		assertThat(nomesEncontrados(filtro(null, null, "corte", null, null, "50"))).isEmpty();
		assertThat(nomesEncontrados(filtro(null, null, null, CategoriaServico.UNHAS, null, "50")))
				.containsExactly("Salão Completo");
	}

	@Test
	void estabelecimentoComVariosServicosNaFaixaApareceUmaVez() {
		Estabelecimento salao = estabelecimento("Salão Cheio", "Centro");
		Servico corte = servico(salao, "Corte", CategoriaServico.CABELO);
		profissional(salao, true, corte, "50.00");
		profissional(salao, true, corte, "60.00");
		profissional(salao, true, servico(salao, "Escova", CategoriaServico.CABELO), "55.00");

		Pagina<EstabelecimentoEncontrado> pagina = catalogo.buscar(filtro(null, null, null, CategoriaServico.CABELO,
				null, "100"), PRIMEIRA_PAGINA, OrdemDaBusca.NOME);

		assertThat(nomes(pagina)).containsExactly("Salão Cheio");
		assertThat(pagina.totalDeItens()).isEqualTo(1);
	}

	@Test
	void filtrosCombinadosExigemTodosOsCriterios() {
		Estabelecimento certo = avaliado("Salão Bela Vista", 5, 1);
		profissional(certo, true, servico(certo, "Corte", CategoriaServico.CABELO), "80.00");
		Estabelecimento malAvaliado = avaliado("Salão Bela Mal Avaliado", 2, 1);
		profissional(malAvaliado, true, servico(malAvaliado, "Corte", CategoriaServico.CABELO), "80.00");
		Estabelecimento outroNome = avaliado("Barbearia", 5, 1);
		profissional(outroNome, true, servico(outroNome, "Corte", CategoriaServico.CABELO), "80.00");

		FiltroDaBusca filtro = new FiltroDaBusca("bela", cidade, "Centro", "corte", CategoriaServico.CABELO,
				new BigDecimal("4"), new BigDecimal("50"), new BigDecimal("90"), null);

		assertThat(nomesEncontrados(filtro)).containsExactly("Salão Bela Vista");
	}

	@Test
	void melhorAvaliadosPrimeiroESemAvaliacaoNoFim() {
		estabelecimento("Salão Novo", "Centro");
		avaliado("Salão Bom", 3, 1);
		avaliado("Salão Ótimo B", 9, 2);
		avaliado("Salão Ótimo A", 9, 2);

		Pagina<EstabelecimentoEncontrado> pagina = catalogo.buscar(naCidade(), PRIMEIRA_PAGINA,
				OrdemDaBusca.MELHOR_AVALIADOS);

		assertThat(nomes(pagina)).containsExactly("Salão Ótimo A", "Salão Ótimo B", "Salão Bom", "Salão Novo");
	}

	@Test
	void paginaComOsTotaisDaConsultaInteira() {
		estabelecimento("Salão A", "Centro");
		estabelecimento("Salão B", "Centro");
		estabelecimento("Salão C", "Centro");

		Pagina<EstabelecimentoEncontrado> segunda = catalogo.buscar(naCidade(), Paginacao.de(1, 2),
				OrdemDaBusca.NOME);

		assertThat(nomes(segunda)).containsExactly("Salão C");
		assertThat(segunda.numero()).isEqualTo(1);
		assertThat(segunda.tamanho()).isEqualTo(2);
		assertThat(segunda.totalDeItens()).isEqualTo(3);
		assertThat(segunda.totalDePaginas()).isEqualTo(2);
	}

	@Test
	void paginaAlemDoFimVemVaziaComOsTotais() {
		estabelecimento("Salão A", "Centro");

		Pagina<EstabelecimentoEncontrado> pagina = catalogo.buscar(naCidade(), Paginacao.de(5, 10),
				OrdemDaBusca.NOME);

		assertThat(pagina.itens()).isEmpty();
		assertThat(pagina.totalDeItens()).isEqualTo(1);
	}

}
