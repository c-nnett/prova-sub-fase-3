package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.OUTRO_USUARIO_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.endereco;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.estabelecimento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class EstabelecimentoTest {

	@Nested
	class Cadastro {

		@Test
		void cadastraAtivoComFusoPadraoESemFotosNemHorarios() {
			Estabelecimento estabelecimento = estabelecimento();

			assertThat(estabelecimento.id()).isNotNull();
			assertThat(estabelecimento.gestorId()).isEqualTo(GESTOR_ID);
			assertThat(estabelecimento.nome()).isEqualTo("Salão Bela Vista");
			assertThat(estabelecimento.descricao()).isEqualTo("Cabelo e unhas");
			assertThat(estabelecimento.endereco()).isEqualTo(endereco());
			assertThat(estabelecimento.timezone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
			assertThat(estabelecimento.ativo()).isTrue();
			assertThat(estabelecimento.fotos()).isEmpty();
			assertThat(estabelecimento.horarios()).isEmpty();
		}

		@Test
		void aceitaFusoInformado() {
			Estabelecimento estabelecimento = Estabelecimento.cadastrar(
					GESTOR_ID, "Salão", null, endereco(), "America/Manaus");

			assertThat(estabelecimento.timezone()).isEqualTo(ZoneId.of("America/Manaus"));
			assertThat(estabelecimento.descricao()).isNull();
		}

		@Test
		void recusaFusoInvalido() {
			assertThatThrownBy(() -> Estabelecimento.cadastrar(GESTOR_ID, "Salão", null, endereco(), "Lua/Crateras"))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("timezone inválido: Lua/Crateras");
		}

		@Test
		void recusaSemGestorNomeOuEndereco() {
			assertThatThrownBy(() -> Estabelecimento.cadastrar(null, "Salão", null, endereco(), null))
					.hasMessage("gestorId é obrigatório");
			assertThatThrownBy(() -> Estabelecimento.cadastrar(GESTOR_ID, " ", null, endereco(), null))
					.hasMessage("nome é obrigatório");
			assertThatThrownBy(() -> Estabelecimento.cadastrar(GESTOR_ID, "Salão", null, null, null))
					.hasMessage("endereco é obrigatório");
		}

		@Test
		void recusaNomeAcimaDe150Caracteres() {
			assertThatThrownBy(() -> Estabelecimento.cadastrar(GESTOR_ID, "a".repeat(151), null, endereco(), null))
					.hasMessage("nome deve ter no máximo 150 caracteres");
		}

	}

	@Nested
	class Atualizacao {

		@Test
		void atualizaDados() {
			Estabelecimento estabelecimento = estabelecimento();
			Endereco novoEndereco = new Endereco("Av. Paulista", "1000", "Loja 3", "Bela Vista", "São Paulo", "SP", "01310100");

			estabelecimento.atualizarDados("Studio Paulista", "Estética", novoEndereco, "America/Sao_Paulo");

			assertThat(estabelecimento.nome()).isEqualTo("Studio Paulista");
			assertThat(estabelecimento.descricao()).isEqualTo("Estética");
			assertThat(estabelecimento.endereco()).isEqualTo(novoEndereco);
		}

		@Test
		void aplicaAsMesmasValidacoesDoCadastro() {
			Estabelecimento estabelecimento = estabelecimento();

			assertThatThrownBy(() -> estabelecimento.atualizarDados("", null, endereco(), null))
					.hasMessage("nome é obrigatório");
			assertThat(estabelecimento.nome()).isEqualTo("Salão Bela Vista");
		}

	}

	@Nested
	class Gestor {

		@Test
		void aceitaOProprioGestor() {
			assertThatCode(() -> estabelecimento().garantirGestor(GESTOR_ID)).doesNotThrowAnyException();
		}

		@Test
		void recusaOutroUsuario() {
			assertThatThrownBy(() -> estabelecimento().garantirGestor(OUTRO_USUARIO_ID))
					.isInstanceOf(AcessoNegadoException.class)
					.hasMessage("Somente o gestor do estabelecimento pode alterá-lo");
		}

	}

	@Nested
	class Horarios {

		private final HorarioFuncionamento segunda = new HorarioFuncionamento(
				DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));
		private final HorarioFuncionamento sabado = new HorarioFuncionamento(
				DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(14, 0));

		@Test
		void defineHorariosOrdenadosPorDia() {
			Estabelecimento estabelecimento = estabelecimento();

			estabelecimento.definirHorarios(List.of(sabado, segunda));

			assertThat(estabelecimento.horarios()).containsExactly(segunda, sabado);
			assertThat(estabelecimento.horarioEm(DayOfWeek.MONDAY)).contains(segunda);
			assertThat(estabelecimento.horarioEm(DayOfWeek.SUNDAY)).isEmpty();
		}

		@Test
		void substituiOsHorariosAnteriores() {
			Estabelecimento estabelecimento = estabelecimento();
			estabelecimento.definirHorarios(List.of(segunda, sabado));

			estabelecimento.definirHorarios(List.of(sabado));

			assertThat(estabelecimento.horarios()).containsExactly(sabado);
		}

		@Test
		void recusaDiaRepetido() {
			HorarioFuncionamento segundaTarde = new HorarioFuncionamento(
					DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(20, 0));

			assertThatThrownBy(() -> estabelecimento().definirHorarios(List.of(segunda, segundaTarde)))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Horário de funcionamento repetido para MONDAY");
		}

		@Test
		void listaNulaDeixaOEstabelecimentoSemHorarios() {
			Estabelecimento estabelecimento = estabelecimento();
			estabelecimento.definirHorarios(List.of(segunda));

			estabelecimento.definirHorarios(null);

			assertThat(estabelecimento.horarios()).isEmpty();
		}

	}

	@Nested
	class Fotos {

		@Test
		void adicionaFotosNaOrdem() {
			Estabelecimento estabelecimento = estabelecimento();

			FotoEstabelecimento primeira = estabelecimento.adicionarFoto("https://cdn.exemplo.com/1.jpg");
			FotoEstabelecimento segunda = estabelecimento.adicionarFoto("http://cdn.exemplo.com/2.jpg");

			assertThat(primeira.id()).isNotNull();
			assertThat(primeira.ordem()).isEqualTo(1);
			assertThat(segunda.ordem()).isEqualTo(2);
			assertThat(estabelecimento.fotos()).containsExactly(primeira, segunda);
		}

		@Test
		void recusaUrlQueNaoEhHttp() {
			assertThatThrownBy(() -> estabelecimento().adicionarFoto("ftp://cdn.exemplo.com/1.jpg"))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("url da foto deve começar com http:// ou https://");
		}

		@Test
		void recusaUrlAcimaDe500Caracteres() {
			String url = "https://cdn.exemplo.com/" + "a".repeat(480);

			assertThatThrownBy(() -> estabelecimento().adicionarFoto(url))
					.hasMessage("url da foto deve ter no máximo 500 caracteres");
		}

		@Test
		void recusaMaisQueOLimiteDeFotos() {
			Estabelecimento estabelecimento = estabelecimento();
			for (int i = 1; i <= Estabelecimento.MAXIMO_FOTOS; i++) {
				estabelecimento.adicionarFoto("https://cdn.exemplo.com/" + i + ".jpg");
			}

			assertThatThrownBy(() -> estabelecimento.adicionarFoto("https://cdn.exemplo.com/extra.jpg"))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O estabelecimento pode ter no máximo 10 fotos");
		}

		@Test
		void removeFotoEReordenaAsDemais() {
			Estabelecimento estabelecimento = estabelecimento();
			FotoEstabelecimento primeira = estabelecimento.adicionarFoto("https://cdn.exemplo.com/1.jpg");
			estabelecimento.adicionarFoto("https://cdn.exemplo.com/2.jpg");
			estabelecimento.adicionarFoto("https://cdn.exemplo.com/3.jpg");

			estabelecimento.removerFoto(primeira.id());

			assertThat(estabelecimento.fotos())
					.extracting(FotoEstabelecimento::url, FotoEstabelecimento::ordem)
					.containsExactly(
							org.assertj.core.groups.Tuple.tuple("https://cdn.exemplo.com/2.jpg", 1),
							org.assertj.core.groups.Tuple.tuple("https://cdn.exemplo.com/3.jpg", 2));
		}

		@Test
		void recusaRemoverFotoInexistente() {
			UUID fotoId = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

			assertThatThrownBy(() -> estabelecimento().removerFoto(fotoId))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Foto " + fotoId + " não encontrada");
		}

		@Test
		void listaDeFotosNaoPodeSerAlteradaPorFora() {
			Estabelecimento estabelecimento = estabelecimento();

			assertThatThrownBy(() -> estabelecimento.fotos().clear())
					.isInstanceOf(UnsupportedOperationException.class);
		}

	}

	@Nested
	class Reconstituicao {

		private final UUID id = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
		private final FotoEstabelecimento foto = new FotoEstabelecimento(
				UUID.fromString("00000000-0000-0000-0000-0000000000f1"), "https://cdn.exemplo.com/1.jpg", 1);
		private final HorarioFuncionamento segunda = new HorarioFuncionamento(
				DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

		@Test
		void reconstituiComOsDadosGravados() {
			Estabelecimento estabelecimento = Estabelecimento.reconstituir(id, GESTOR_ID, "Salão", "Desc",
					endereco(), "America/Manaus", new BigDecimal("4.50"), 12, false, List.of(foto), List.of(segunda));

			assertThat(estabelecimento.id()).isEqualTo(id);
			assertThat(estabelecimento.gestorId()).isEqualTo(GESTOR_ID);
			assertThat(estabelecimento.nome()).isEqualTo("Salão");
			assertThat(estabelecimento.timezone()).isEqualTo(ZoneId.of("America/Manaus"));
			assertThat(estabelecimento.notaMedia()).isEqualTo(new BigDecimal("4.50"));
			assertThat(estabelecimento.totalAvaliacoes()).isEqualTo(12);
			assertThat(estabelecimento.ativo()).isFalse();
			assertThat(estabelecimento.fotos()).containsExactly(foto);
			assertThat(estabelecimento.horarios()).containsExactly(segunda);
		}

		@Test
		void ordenaFotosPelaOrdemGravada() {
			FotoEstabelecimento segundaFoto = new FotoEstabelecimento(UUID.randomUUID(), "https://cdn.exemplo.com/2.jpg", 2);

			Estabelecimento estabelecimento = Estabelecimento.reconstituir(id, GESTOR_ID, "Salão", null,
					endereco(), null, null, 0, true, List.of(segundaFoto, foto), null);

			assertThat(estabelecimento.fotos()).containsExactly(foto, segundaFoto);
			assertThat(estabelecimento.horarios()).isEmpty();
		}

		@Test
		void recusaSemId() {
			assertThatThrownBy(() -> Estabelecimento.reconstituir(null, GESTOR_ID, "Salão", null,
					endereco(), null, null, 0, true, List.of(), List.of()))
					.hasMessage("id é obrigatório");
		}

	}

	@Nested
	class Avaliacoes {

		@Test
		void novoEstabelecimentoNaoTemAvaliacoes() {
			Estabelecimento estabelecimento = estabelecimento();

			assertThat(estabelecimento.notaMedia()).isNull();
			assertThat(estabelecimento.totalAvaliacoes()).isZero();
		}

		@Test
		void atualizaANotaMediaEOTotal() {
			Estabelecimento estabelecimento = estabelecimento();

			estabelecimento.atualizarAvaliacoes(NotaMedia.de(9, 2));

			assertThat(estabelecimento.notaMedia()).isEqualTo(new BigDecimal("4.50"));
			assertThat(estabelecimento.totalAvaliacoes()).isEqualTo(2);
		}

	}

}
