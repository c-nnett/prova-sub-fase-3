package br.com.fiap.agendabeleza.api.avaliacoes.application.usecase;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.OUTRO_USUARIO_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.agendamentos.application.AgendamentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.avaliacoes.application.AvaliacaoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import br.com.fiap.agendabeleza.api.compartilhado.application.ExecucaoSerializadaDireta;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.EstabelecimentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.profissionais.application.ProfissionalRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Avaliação de atendimentos e listagem pública. As notas médias do estabelecimento e do profissional são recalculadas
 * a partir de todas as avaliações gravadas, na mesma execução serializada pelo estabelecimento.
 */
class AvaliacaoUseCasesTest {

	private static final Instant INICIO = Instant.parse("2030-03-04T13:00:00Z");
	private static final Instant AGORA = INICIO.plus(Duration.ofHours(3));
	private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

	private final AgendamentoRepositoryEmMemoria agendamentos = new AgendamentoRepositoryEmMemoria();
	private final AvaliacaoRepositoryEmMemoria avaliacoes = new AvaliacaoRepositoryEmMemoria();
	private final EstabelecimentoRepositoryEmMemoria estabelecimentos = new EstabelecimentoRepositoryEmMemoria();
	private final ProfissionalRepositoryEmMemoria profissionais = new ProfissionalRepositoryEmMemoria();
	private final ExecucaoSerializadaDireta execucao = new ExecucaoSerializadaDireta();
	private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);

	private final AvaliarAtendimentoUseCase avaliar = new AvaliarAtendimentoUseCase(agendamentos, avaliacoes,
			estabelecimentos, profissionais, execucao, relogio);
	private final ListarAvaliacoesUseCase listar = new ListarAvaliacoesUseCase(avaliacoes, estabelecimentos,
			profissionais);

	private Estabelecimento estabelecimento;
	private Profissional ana;
	private Profissional bia;

	@BeforeEach
	void prepararCadastros() {
		estabelecimento = estabelecimentos.salvar(
				Estabelecimento.cadastrar(GESTOR_ID, "Salão Bela Vista", null, endereco(), null));
		ana = profissionais.salvar(Profissional.cadastrar(estabelecimento.id(), null, "Ana", "ana@salao.com", null));
		bia = profissionais.salvar(Profissional.cadastrar(estabelecimento.id(), null, "Bia", "bia@salao.com", null));
	}

	/** Cada atendimento começa uma hora depois do anterior, para não conflitar na agenda em memória. */
	private Agendamento atendimento(Profissional profissional, StatusAgendamento status) {
		Instant inicio = INICIO.minus(Duration.ofHours(agendamentos.total() + 1L));
		return agendamentos.salvar(Agendamento.reconstituir(UUID.randomUUID(), CLIENTE_ID, estabelecimento.id(),
				profissional.id(), SERVICO_ID, inicio, inicio.plus(Duration.ofMinutes(30)), status,
				new BigDecimal("95.00"), null, null));
	}

	private Agendamento concluido(Profissional profissional) {
		return atendimento(profissional, StatusAgendamento.CONCLUIDO);
	}

	private Avaliacao avaliar(Agendamento agendamento, UUID usuarioId, Integer notaEstabelecimento,
			Integer notaProfissional) {
		return avaliar.executar(new AvaliarAtendimentoUseCase.Comando(agendamento.id(), usuarioId,
				notaEstabelecimento, notaProfissional, "Comentário"));
	}

	private Avaliacao avaliarComoCliente(Agendamento agendamento, int notaEstabelecimento, int notaProfissional) {
		return avaliar(agendamento, CLIENTE_ID, notaEstabelecimento, notaProfissional);
	}

	private Estabelecimento estabelecimentoGravado() {
		return estabelecimentos.buscarPorId(estabelecimento.id()).orElseThrow();
	}

	private Profissional profissionalGravado(Profissional profissional) {
		return profissionais.buscarPorId(profissional.id()).orElseThrow();
	}

	// Avaliar

	@Test
	void clienteAvaliaOAtendimentoConcluidoEAAvaliacaoEGravada() {
		Agendamento agendamento = concluido(ana);

		Avaliacao avaliacao = avaliarComoCliente(agendamento, 5, 4);

		assertThat(avaliacao.agendamentoId()).isEqualTo(agendamento.id());
		assertThat(avaliacao.profissionalId()).isEqualTo(ana.id());
		assertThat(avaliacao.criadaEm()).isEqualTo(AGORA);
		assertThat(avaliacao.comentario()).isEqualTo("Comentário");
		assertThat(avaliacoes.total()).isEqualTo(1);
	}

	@Test
	void primeiraAvaliacaoDefineAsNotasMediasDoEstabelecimentoEDoProfissional() {
		avaliarComoCliente(concluido(ana), 5, 4);

		assertThat(estabelecimentoGravado().notaMedia()).isEqualTo(new BigDecimal("5.00"));
		assertThat(estabelecimentoGravado().totalAvaliacoes()).isEqualTo(1);
		assertThat(profissionalGravado(ana).notaMedia()).isEqualTo(new BigDecimal("4.00"));
		assertThat(profissionalGravado(ana).totalAvaliacoes()).isEqualTo(1);
	}

	@Test
	void novasAvaliacoesRecalculamAMediaComTodasAsNotas() {
		avaliarComoCliente(concluido(ana), 5, 5);
		avaliarComoCliente(concluido(ana), 4, 4);
		avaliarComoCliente(concluido(bia), 4, 2);

		assertThat(estabelecimentoGravado().notaMedia()).isEqualTo(new BigDecimal("4.33"));
		assertThat(estabelecimentoGravado().totalAvaliacoes()).isEqualTo(3);
		assertThat(profissionalGravado(ana).notaMedia()).isEqualTo(new BigDecimal("4.50"));
		assertThat(profissionalGravado(ana).totalAvaliacoes()).isEqualTo(2);
		assertThat(profissionalGravado(bia).notaMedia()).isEqualTo(new BigDecimal("2.00"));
		assertThat(profissionalGravado(bia).totalAvaliacoes()).isEqualTo(1);
	}

	@Test
	void avaliacaoRodaSerializadaPeloEstabelecimento() {
		avaliarComoCliente(concluido(ana), 5, 4);

		assertThat(execucao.chaves()).containsExactly(estabelecimento.id());
	}

	@Test
	void agendamentoInexistenteDevolve404() {
		UUID agendamentoInexistente = UUID.randomUUID();

		assertThatThrownBy(() -> avaliar.executar(new AvaliarAtendimentoUseCase.Comando(agendamentoInexistente,
				CLIENTE_ID, 5, 5, null)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessageContaining(agendamentoInexistente.toString());
	}

	@Test
	void somenteOClienteDoAgendamentoAvalia() {
		Agendamento agendamento = concluido(ana);

		for (UUID quemNaoEhOCliente : new UUID[] { GESTOR_ID, OUTRO_USUARIO_ID }) {
			assertThatThrownBy(() -> avaliar(agendamento, quemNaoEhOCliente, 5, 5))
					.isInstanceOf(AcessoNegadoException.class)
					.hasMessage(AvaliarAtendimentoUseCase.SOMENTE_O_CLIENTE);
		}
		assertThat(avaliacoes.total()).isZero();
		assertThat(execucao.chaves()).isEmpty();
	}

	@Test
	void recusaAtendimentoNaoConcluidoSemMudarAsNotas() {
		Agendamento confirmado = atendimento(ana, StatusAgendamento.CONFIRMADO);

		assertThatThrownBy(() -> avaliarComoCliente(confirmado, 5, 5))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(Avaliacao.AGENDAMENTO_NAO_CONCLUIDO);
		assertThat(avaliacoes.total()).isZero();
		assertThat(estabelecimentoGravado().totalAvaliacoes()).isZero();
	}

	@Test
	void recusaSegundaAvaliacaoDoMesmoAtendimentoSemMudarAsNotas() {
		Agendamento agendamento = concluido(ana);
		avaliarComoCliente(agendamento, 5, 5);

		assertThatThrownBy(() -> avaliarComoCliente(agendamento, 1, 1))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(AvaliarAtendimentoUseCase.JA_AVALIADO);
		assertThat(avaliacoes.total()).isEqualTo(1);
		assertThat(estabelecimentoGravado().notaMedia()).isEqualTo(new BigDecimal("5.00"));
		assertThat(profissionalGravado(ana).totalAvaliacoes()).isEqualTo(1);
	}

	@Test
	void recusaNotaForaDaEscalaSemGravar() {
		Agendamento agendamento = concluido(ana);

		assertThatThrownBy(() -> avaliarComoCliente(agendamento, 6, 5))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("notaEstabelecimento deve ser de 1 a 5");
		assertThat(avaliacoes.total()).isZero();
	}

	// Listar

	private Avaliacao gravada(Agendamento agendamento, Instant criadaEm) {
		return avaliacoes.salvar(Avaliacao.reconstituir(UUID.randomUUID(), agendamento.id(), CLIENTE_ID,
				agendamento.estabelecimentoId(), agendamento.profissionalId(), 4, 4, null, criadaEm));
	}

	@Test
	void listaAsAvaliacoesDoEstabelecimentoDaMaisRecenteParaAMaisAntiga() {
		Avaliacao antiga = gravada(concluido(ana), AGORA.minus(Duration.ofDays(2)));
		Avaliacao recente = gravada(concluido(bia), AGORA);
		Avaliacao intermediaria = gravada(concluido(ana), AGORA.minus(Duration.ofDays(1)));

		assertThat(listar.doEstabelecimento(estabelecimento.id()))
				.extracting(Avaliacao::id)
				.containsExactly(recente.id(), intermediaria.id(), antiga.id());
	}

	@Test
	void listaSomenteAsAvaliacoesDoProfissional() {
		Avaliacao daAna = gravada(concluido(ana), AGORA);
		gravada(concluido(bia), AGORA);

		assertThat(listar.doProfissional(ana.id())).extracting(Avaliacao::id).containsExactly(daAna.id());
	}

	@Test
	void estabelecimentoSemAvaliacoesDevolveListaVazia() {
		assertThat(listar.doEstabelecimento(estabelecimento.id())).isEmpty();
	}

	@Test
	void listarDeEstabelecimentoOuProfissionalInexistenteDevolve404() {
		UUID inexistente = UUID.randomUUID();

		assertThatThrownBy(() -> listar.doEstabelecimento(inexistente))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Estabelecimento " + inexistente + " não encontrado");
		assertThatThrownBy(() -> listar.doProfissional(inexistente))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Profissional " + inexistente + " não encontrado");
	}

}
