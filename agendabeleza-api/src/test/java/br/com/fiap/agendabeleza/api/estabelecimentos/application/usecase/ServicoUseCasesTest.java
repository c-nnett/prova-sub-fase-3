package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.OUTRO_USUARIO_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.estabelecimento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.EstabelecimentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.ServicoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

class ServicoUseCasesTest {

	private final EstabelecimentoRepositoryEmMemoria estabelecimentos = new EstabelecimentoRepositoryEmMemoria();
	private final ServicoRepositoryEmMemoria servicos = new ServicoRepositoryEmMemoria();

	private final CadastrarServicoUseCase cadastrar = new CadastrarServicoUseCase(estabelecimentos, servicos);
	private final AtualizarServicoUseCase atualizar = new AtualizarServicoUseCase(estabelecimentos, servicos);

	private final Estabelecimento estabelecimento = estabelecimentos.salvar(estabelecimento());

	private Servico cadastrarCorte() {
		return cadastrar.executar(new CadastrarServicoUseCase.Comando(
				estabelecimento.id(), GESTOR_ID, "Corte", null, CategoriaServico.CABELO, 60, new BigDecimal("80")));
	}

	@Nested
	class Cadastrar {

		@Test
		void cadastraServicoDoEstabelecimento() {
			Servico servico = cadastrarCorte();

			assertThat(servico.estabelecimentoId()).isEqualTo(estabelecimento.id());
			assertThat(servicos.listarPorEstabelecimento(estabelecimento.id())).containsExactly(servico);
		}

		@Test
		void recusaOutroUsuario() {
			assertThatThrownBy(() -> cadastrar.executar(new CadastrarServicoUseCase.Comando(
					estabelecimento.id(), OUTRO_USUARIO_ID, "Corte", null, CategoriaServico.CABELO, 60, BigDecimal.TEN)))
					.isInstanceOf(AcessoNegadoException.class);
			assertThat(servicos.listarPorEstabelecimento(estabelecimento.id())).isEmpty();
		}

		@Test
		void recusaEstabelecimentoInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> cadastrar.executar(new CadastrarServicoUseCase.Comando(
					inexistente, GESTOR_ID, "Corte", null, CategoriaServico.CABELO, 60, BigDecimal.TEN)))
					.isInstanceOf(RecursoNaoEncontradoException.class);
		}

	}

	@Nested
	class Listar {

		private final ListarServicosUseCase listar = new ListarServicosUseCase(estabelecimentos, servicos);

		@Test
		void listaSomenteOsServicosAtivosDoEstabelecimento() {
			Servico corte = cadastrarCorte();
			Servico inativo = cadastrarCorte();
			atualizar.executar(new AtualizarServicoUseCase.Comando(estabelecimento.id(), inativo.id(), GESTOR_ID,
					"Antigo", null, CategoriaServico.CABELO, 30, BigDecimal.TEN, false));

			assertThat(listar.executar(estabelecimento.id())).containsExactly(corte);
		}

		@Test
		void recusaEstabelecimentoInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> listar.executar(inexistente))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Estabelecimento " + inexistente + " não encontrado");
		}

	}

	@Nested
	class Atualizar {

		@Test
		void atualizaServico() {
			Servico servico = cadastrarCorte();

			Servico atualizado = atualizar.executar(new AtualizarServicoUseCase.Comando(
					estabelecimento.id(), servico.id(), GESTOR_ID,
					"Corte e escova", "Com hidratação", CategoriaServico.CABELO, 90, new BigDecimal("120"), false));

			assertThat(atualizado.nome()).isEqualTo("Corte e escova");
			assertThat(atualizado.ativo()).isFalse();
			assertThat(servicos.buscarPorId(servico.id())).contains(atualizado);
		}

		@Test
		void recusaOutroUsuario() {
			Servico servico = cadastrarCorte();

			assertThatThrownBy(() -> atualizar.executar(new AtualizarServicoUseCase.Comando(
					estabelecimento.id(), servico.id(), OUTRO_USUARIO_ID,
					"Outro", null, CategoriaServico.CABELO, 30, BigDecimal.TEN, true)))
					.isInstanceOf(AcessoNegadoException.class);
		}

		@Test
		void recusaServicoInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> atualizar.executar(new AtualizarServicoUseCase.Comando(
					estabelecimento.id(), inexistente, GESTOR_ID,
					"Corte", null, CategoriaServico.CABELO, 30, BigDecimal.TEN, true)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Serviço " + inexistente + " não encontrado");
		}

		@Test
		void recusaServicoDeOutroEstabelecimento() {
			Servico servico = cadastrarCorte();
			Estabelecimento outro = estabelecimentos.salvar(estabelecimento());

			assertThatThrownBy(() -> atualizar.executar(new AtualizarServicoUseCase.Comando(
					outro.id(), servico.id(), GESTOR_ID,
					"Corte", null, CategoriaServico.CABELO, 30, BigDecimal.TEN, true)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Serviço " + servico.id() + " não encontrado");
		}

	}

}
