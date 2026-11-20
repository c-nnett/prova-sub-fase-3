package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.OUTRO_USUARIO_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.estabelecimento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.EstabelecimentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Endereco;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.FotoEstabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;

class EstabelecimentoUseCasesTest {

	private static final UUID INEXISTENTE = UUID.fromString("00000000-0000-0000-0000-0000000000ee");

	private final EstabelecimentoRepositoryEmMemoria repositorio = new EstabelecimentoRepositoryEmMemoria();

	private final HorarioFuncionamento segunda = new HorarioFuncionamento(
			DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

	private Estabelecimento existente() {
		return repositorio.salvar(estabelecimento());
	}

	@Nested
	class Cadastrar {

		private final CadastrarEstabelecimentoUseCase useCase = new CadastrarEstabelecimentoUseCase(repositorio);

		@Test
		void cadastraComHorariosEFotosESalva() {
			Estabelecimento criado = useCase.executar(new CadastrarEstabelecimentoUseCase.Comando(
					GESTOR_ID, "Salão Bela Vista", null, endereco(), null,
					List.of(segunda), List.of("https://cdn.exemplo.com/1.jpg")));

			assertThat(repositorio.buscarPorId(criado.id())).contains(criado);
			assertThat(criado.gestorId()).isEqualTo(GESTOR_ID);
			assertThat(criado.horarios()).containsExactly(segunda);
			assertThat(criado.fotos()).extracting(FotoEstabelecimento::url)
					.containsExactly("https://cdn.exemplo.com/1.jpg");
		}

		@Test
		void horariosEFotosSaoOpcionais() {
			Estabelecimento criado = useCase.executar(new CadastrarEstabelecimentoUseCase.Comando(
					GESTOR_ID, "Salão", null, endereco(), null, null, null));

			assertThat(criado.horarios()).isEmpty();
			assertThat(criado.fotos()).isEmpty();
		}

	}

	@Nested
	class Atualizar {

		private final AtualizarEstabelecimentoUseCase useCase = new AtualizarEstabelecimentoUseCase(repositorio);

		@Test
		void atualizaQuandoEhOGestor() {
			Estabelecimento estabelecimento = existente();
			Endereco novoEndereco = new Endereco("Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP", "01310100");

			Estabelecimento atualizado = useCase.executar(new AtualizarEstabelecimentoUseCase.Comando(
					estabelecimento.id(), GESTOR_ID, "Studio Paulista", "Estética", novoEndereco, "America/Sao_Paulo"));

			assertThat(atualizado.nome()).isEqualTo("Studio Paulista");
			assertThat(atualizado.endereco()).isEqualTo(novoEndereco);
			assertThat(repositorio.salvamentos()).isEqualTo(2);
		}

		@Test
		void recusaOutroUsuarioSemSalvar() {
			Estabelecimento estabelecimento = existente();

			assertThatThrownBy(() -> useCase.executar(new AtualizarEstabelecimentoUseCase.Comando(
					estabelecimento.id(), OUTRO_USUARIO_ID, "Outro nome", null, endereco(), null)))
					.isInstanceOf(AcessoNegadoException.class);
			assertThat(repositorio.salvamentos()).isEqualTo(1);
		}

		@Test
		void recusaEstabelecimentoInexistente() {
			assertThatThrownBy(() -> useCase.executar(new AtualizarEstabelecimentoUseCase.Comando(
					INEXISTENTE, GESTOR_ID, "Nome", null, endereco(), null)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Estabelecimento " + INEXISTENTE + " não encontrado");
		}

	}

	@Nested
	class DefinirHorarios {

		private final DefinirHorariosFuncionamentoUseCase useCase = new DefinirHorariosFuncionamentoUseCase(repositorio);

		@Test
		void defineHorariosQuandoEhOGestor() {
			Estabelecimento estabelecimento = existente();

			Estabelecimento atualizado = useCase.executar(new DefinirHorariosFuncionamentoUseCase.Comando(
					estabelecimento.id(), GESTOR_ID, List.of(segunda)));

			assertThat(atualizado.horarios()).containsExactly(segunda);
			assertThat(repositorio.salvamentos()).isEqualTo(2);
		}

		@Test
		void recusaOutroUsuario() {
			Estabelecimento estabelecimento = existente();

			assertThatThrownBy(() -> useCase.executar(new DefinirHorariosFuncionamentoUseCase.Comando(
					estabelecimento.id(), OUTRO_USUARIO_ID, List.of(segunda))))
					.isInstanceOf(AcessoNegadoException.class);
		}

	}

	@Nested
	class Fotos {

		private final AdicionarFotoEstabelecimentoUseCase adicionar = new AdicionarFotoEstabelecimentoUseCase(repositorio);
		private final RemoverFotoEstabelecimentoUseCase remover = new RemoverFotoEstabelecimentoUseCase(repositorio);

		@Test
		void adicionaERemoveFoto() {
			Estabelecimento estabelecimento = existente();

			FotoEstabelecimento foto = adicionar.executar(new AdicionarFotoEstabelecimentoUseCase.Comando(
					estabelecimento.id(), GESTOR_ID, "https://cdn.exemplo.com/1.jpg"));
			assertThat(repositorio.buscarPorId(estabelecimento.id()).orElseThrow().fotos()).containsExactly(foto);

			remover.executar(new RemoverFotoEstabelecimentoUseCase.Comando(estabelecimento.id(), GESTOR_ID, foto.id()));
			assertThat(repositorio.buscarPorId(estabelecimento.id()).orElseThrow().fotos()).isEmpty();
			assertThat(repositorio.salvamentos()).isEqualTo(3);
		}

		@Test
		void recusaOutroUsuario() {
			Estabelecimento estabelecimento = existente();

			assertThatThrownBy(() -> adicionar.executar(new AdicionarFotoEstabelecimentoUseCase.Comando(
					estabelecimento.id(), OUTRO_USUARIO_ID, "https://cdn.exemplo.com/1.jpg")))
					.isInstanceOf(AcessoNegadoException.class);
			assertThatThrownBy(() -> remover.executar(new RemoverFotoEstabelecimentoUseCase.Comando(
					estabelecimento.id(), OUTRO_USUARIO_ID, UUID.randomUUID())))
					.isInstanceOf(AcessoNegadoException.class);
		}

	}

}
