package br.com.fiap.agendabeleza.api.avaliacoes.adapters.out.persistence;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.avaliacoes.application.port.AvaliacaoRepository;
import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Persistência das avaliações no PostgreSQL do Testcontainers, e a gravação isolada das notas agregadas de
 * estabelecimento e profissional.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AvaliacaoPersistenciaTest {

	private static final Instant INICIO = Instant.parse("2030-03-04T13:00:00Z");

	@Autowired
	private AvaliacaoRepository avaliacoes;

	@Autowired
	private AgendamentoRepository agendamentos;

	@Autowired
	private EstabelecimentoRepository estabelecimentos;

	@Autowired
	private ServicoRepository servicos;

	@Autowired
	private ProfissionalRepository profissionais;

	private Estabelecimento estabelecimento;
	private Servico corte;
	private Profissional ana;
	private Profissional bia;
	private int atendimentosCriados;

	@BeforeEach
	void prepararCadastros() {
		estabelecimento = estabelecimentos.salvar(Estabelecimento.cadastrar(
				UUID.randomUUID(), "Salão Bela Vista", null, endereco(), null));
		corte = servicos.salvar(Servico.cadastrar(estabelecimento.id(), "Corte", null, CategoriaServico.CABELO, 30,
				new BigDecimal("80.00")));
		ana = profissionais.salvar(Profissional.cadastrar(estabelecimento.id(), null, "Ana", "ana@salao.com", null));
		bia = profissionais.salvar(Profissional.cadastrar(estabelecimento.id(), null, "Bia", "bia@salao.com", null));
	}

	private Agendamento concluido(Profissional profissional) {
		Instant inicio = INICIO.plus(Duration.ofHours(atendimentosCriados++));
		return agendamentos.salvar(Agendamento.reconstituir(UUID.randomUUID(), UUID.randomUUID(),
				estabelecimento.id(), profissional.id(), corte.id(), inicio, inicio.plus(Duration.ofMinutes(30)),
				StatusAgendamento.CONCLUIDO, new BigDecimal("80.00"), null, null));
	}

	private Avaliacao avaliar(Agendamento agendamento, int notaEstabelecimento, int notaProfissional,
			Instant criadaEm) {
		return avaliacoes.salvar(Avaliacao.registrar(agendamento, notaEstabelecimento, notaProfissional,
				"Muito bom", criadaEm));
	}

	@Test
	void salvaELeAAvaliacaoComTodosOsCampos() {
		Agendamento agendamento = concluido(ana);
		Avaliacao avaliacao = avaliar(agendamento, 5, 4, INICIO.plus(Duration.ofDays(1)));

		assertThat(avaliacoes.existeDoAgendamento(agendamento.id())).isTrue();
		assertThat(avaliacoes.listarDoProfissional(ana.id())).singleElement().satisfies(lida -> {
			assertThat(lida.id()).isEqualTo(avaliacao.id());
			assertThat(lida.agendamentoId()).isEqualTo(agendamento.id());
			assertThat(lida.clienteId()).isEqualTo(agendamento.clienteId());
			assertThat(lida.estabelecimentoId()).isEqualTo(estabelecimento.id());
			assertThat(lida.notaEstabelecimento()).isEqualTo(5);
			assertThat(lida.notaProfissional()).isEqualTo(4);
			assertThat(lida.comentario()).isEqualTo("Muito bom");
			assertThat(lida.criadaEm()).isEqualTo(avaliacao.criadaEm());
		});
	}

	@Test
	void agendamentoSemAvaliacaoNaoExiste() {
		assertThat(avaliacoes.existeDoAgendamento(concluido(ana).id())).isFalse();
	}

	@Test
	void restricaoUnicaRecusaSegundaAvaliacaoDoMesmoAgendamento() {
		Agendamento agendamento = concluido(ana);
		avaliar(agendamento, 5, 5, INICIO);

		assertThatThrownBy(() -> avaliar(agendamento, 1, 1, INICIO))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void listasVemDaMaisRecenteParaAMaisAntigaESeparadasPorProfissional() {
		Avaliacao antiga = avaliar(concluido(ana), 4, 4, INICIO.plus(Duration.ofDays(1)));
		Avaliacao recente = avaliar(concluido(bia), 3, 3, INICIO.plus(Duration.ofDays(3)));
		Avaliacao intermediaria = avaliar(concluido(ana), 5, 5, INICIO.plus(Duration.ofDays(2)));

		assertThat(avaliacoes.listarDoEstabelecimento(estabelecimento.id()))
				.extracting(Avaliacao::id)
				.containsExactly(recente.id(), intermediaria.id(), antiga.id());
		assertThat(avaliacoes.listarDoProfissional(ana.id()))
				.extracting(Avaliacao::id)
				.containsExactly(intermediaria.id(), antiga.id());
	}

	@Test
	void notasSaoCalculadasComTodasAsAvaliacoesGravadas() {
		avaliar(concluido(ana), 5, 5, INICIO);
		avaliar(concluido(ana), 4, 4, INICIO);
		avaliar(concluido(bia), 4, 2, INICIO);

		assertThat(avaliacoes.notasDoEstabelecimento(estabelecimento.id()))
				.isEqualTo(new NotaMedia(new BigDecimal("4.33"), 3));
		assertThat(avaliacoes.notasDoProfissional(ana.id())).isEqualTo(new NotaMedia(new BigDecimal("4.50"), 2));
		assertThat(avaliacoes.notasDoProfissional(bia.id())).isEqualTo(new NotaMedia(new BigDecimal("2.00"), 1));
	}

	@Test
	void semAvaliacoesAsNotasSaoVazias() {
		assertThat(avaliacoes.notasDoEstabelecimento(estabelecimento.id())).isEqualTo(NotaMedia.SEM_AVALIACOES);
	}

	@Test
	void notasDoEstabelecimentoGravadasNaoSaoSobrescritasPorUmaEdicaoComDadosAntigos() {
		Estabelecimento lidoAntesDaAvaliacao = estabelecimentos.buscarPorId(estabelecimento.id()).orElseThrow();
		estabelecimentos.atualizarAvaliacoes(estabelecimento.id(), NotaMedia.de(9, 2));

		lidoAntesDaAvaliacao.atualizarDados("Salão Renovado", null, endereco(), null);
		estabelecimentos.salvar(lidoAntesDaAvaliacao);

		Estabelecimento lido = estabelecimentos.buscarPorId(estabelecimento.id()).orElseThrow();
		assertThat(lido.nome()).isEqualTo("Salão Renovado");
		assertThat(lido.notaMedia()).isEqualTo(new BigDecimal("4.50"));
		assertThat(lido.totalAvaliacoes()).isEqualTo(2);
	}

	@Test
	void notasDoProfissionalGravadasNaoSaoSobrescritasPorUmaEdicaoComDadosAntigos() {
		Profissional lidaAntesDaAvaliacao = profissionais.buscarPorId(ana.id()).orElseThrow();
		profissionais.atualizarAvaliacoes(ana.id(), NotaMedia.de(13, 3));

		lidaAntesDaAvaliacao.atualizarPerfil("Ana Souza", "ana@salao.com", "Colorista", true);
		profissionais.salvar(lidaAntesDaAvaliacao);

		Profissional lida = profissionais.buscarPorId(ana.id()).orElseThrow();
		assertThat(lida.nome()).isEqualTo("Ana Souza");
		assertThat(lida.notaMedia()).isEqualTo(new BigDecimal("4.33"));
		assertThat(lida.totalAvaliacoes()).isEqualTo(3);
	}

	@Test
	void novoEstabelecimentoENovoProfissionalComecamSemAvaliacoes() {
		Estabelecimento lido = estabelecimentos.buscarPorId(estabelecimento.id()).orElseThrow();
		Profissional lida = profissionais.buscarPorId(ana.id()).orElseThrow();

		assertThat(lido.notaMedia()).isNull();
		assertThat(lido.totalAvaliacoes()).isZero();
		assertThat(lida.notaMedia()).isNull();
		assertThat(lida.totalAvaliacoes()).isZero();
	}

}
