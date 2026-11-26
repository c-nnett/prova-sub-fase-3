package br.com.fiap.agendabeleza.api.profissionais.domain;

import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.ESTABELECIMENTO_ID;
import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.USUARIO_PROFISSIONAL_ID;
import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.profissional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class ProfissionalTest {

	private static final UUID GESTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
	private static final UUID OUTRO_SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

	private static PeriodoTrabalho periodo(DayOfWeek dia, int inicio, int fim) {
		return new PeriodoTrabalho(dia, LocalTime.of(inicio, 0), LocalTime.of(fim, 0));
	}

	@Nested
	class Cadastro {

		@Test
		void cadastraAtivoSemAvaliacoes() {
			Profissional profissional = profissional();

			assertThat(profissional.id()).isNotNull();
			assertThat(profissional.estabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
			assertThat(profissional.usuarioId()).isEqualTo(USUARIO_PROFISSIONAL_ID);
			assertThat(profissional.nome()).isEqualTo("Ana Souza");
			assertThat(profissional.email()).isEqualTo("ana@salao.com");
			assertThat(profissional.bio()).isEqualTo("Colorista há 10 anos");
			assertThat(profissional.notaMedia()).isNull();
			assertThat(profissional.totalAvaliacoes()).isZero();
			assertThat(profissional.ativo()).isTrue();
			assertThat(profissional.especialidadeIds()).isEmpty();
			assertThat(profissional.servicos()).isEmpty();
			assertThat(profissional.jornada()).isEmpty();
			assertThat(profissional.pertenceA(ESTABELECIMENTO_ID)).isTrue();
			assertThat(profissional.pertenceA(UUID.randomUUID())).isFalse();
		}

		@Test
		void aceitaProfissionalSemLogin() {
			Profissional profissional = Profissional.cadastrar(ESTABELECIMENTO_ID, null, "Bia", "bia@salao.com", null);

			assertThat(profissional.usuarioId()).isNull();
			assertThat(profissional.bio()).isNull();
		}

		@Test
		void recusaCamposObrigatoriosAusentes() {
			assertThatThrownBy(() -> Profissional.cadastrar(null, null, "Bia", "bia@salao.com", null))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("estabelecimentoId é obrigatório");
			assertThatThrownBy(() -> Profissional.cadastrar(ESTABELECIMENTO_ID, null, " ", "bia@salao.com", null))
					.hasMessage("nome é obrigatório");
			assertThatThrownBy(() -> Profissional.cadastrar(ESTABELECIMENTO_ID, null, "Bia", null, null))
					.hasMessage("email é obrigatório");
		}

		@Test
		void recusaNomeOuEmailLongosDemais() {
			assertThatThrownBy(() -> Profissional.cadastrar(ESTABELECIMENTO_ID, null, "a".repeat(121), "bia@salao.com", null))
					.hasMessage("nome deve ter no máximo 120 caracteres");
			assertThatThrownBy(() -> Profissional.cadastrar(ESTABELECIMENTO_ID, null, "Bia", "a".repeat(171) + "@salao.com", null))
					.hasMessage("email deve ter no máximo 180 caracteres");
		}

		@Test
		void recusaEmailInvalido() {
			assertThatThrownBy(() -> Profissional.cadastrar(ESTABELECIMENTO_ID, null, "Bia", "bia.salao.com", null))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("email inválido");
		}

		@Test
		void atualizaPerfilEStatus() {
			Profissional profissional = profissional();

			profissional.atualizarPerfil("Ana S.", "ana.s@salao.com", null, false);

			assertThat(profissional.nome()).isEqualTo("Ana S.");
			assertThat(profissional.email()).isEqualTo("ana.s@salao.com");
			assertThat(profissional.bio()).isNull();
			assertThat(profissional.ativo()).isFalse();
		}

		@Test
		void atualizacaoInvalidaNaoAlteraNada() {
			Profissional profissional = profissional();

			assertThatThrownBy(() -> profissional.atualizarPerfil("Novo nome", "invalido", null, false))
					.hasMessage("email inválido");
			assertThat(profissional.nome()).isEqualTo("Ana Souza");
			assertThat(profissional.ativo()).isTrue();
		}

	}

	@Nested
	class Autorizacao {

		@Test
		void permiteGestorEProprioProfissional() {
			Profissional profissional = profissional();

			assertThatCode(() -> profissional.garantirPodeAlterar(GESTOR_ID, GESTOR_ID)).doesNotThrowAnyException();
			assertThatCode(() -> profissional.garantirPodeAlterar(USUARIO_PROFISSIONAL_ID, GESTOR_ID))
					.doesNotThrowAnyException();
		}

		@Test
		void recusaOutroUsuario() {
			assertThatThrownBy(() -> profissional().garantirPodeAlterar(UUID.randomUUID(), GESTOR_ID))
					.isInstanceOf(AcessoNegadoException.class)
					.hasMessage("Somente o gestor do estabelecimento ou o próprio profissional pode alterá-lo");
		}

		@Test
		void recusaUsuarioNuloMesmoParaProfissionalSemLogin() {
			Profissional semLogin = Profissional.cadastrar(ESTABELECIMENTO_ID, null, "Bia", "bia@salao.com", null);

			assertThatThrownBy(() -> semLogin.garantirPodeAlterar(null, GESTOR_ID))
					.isInstanceOf(AcessoNegadoException.class);
		}

	}

	@Nested
	class Especialidades {

		@Test
		void substituiEIgnoraRepetidas() {
			UUID colorimetria = UUID.randomUUID();
			UUID corte = UUID.randomUUID();
			Profissional profissional = profissional();
			profissional.definirEspecialidades(List.of(UUID.randomUUID()));

			profissional.definirEspecialidades(List.of(colorimetria, corte, colorimetria));

			assertThat(profissional.especialidadeIds()).containsExactly(colorimetria, corte);
		}

		@Test
		void listaNulaOuVaziaLimpa() {
			Profissional profissional = profissional();
			profissional.definirEspecialidades(List.of(UUID.randomUUID()));

			profissional.definirEspecialidades(null);

			assertThat(profissional.especialidadeIds()).isEmpty();
		}

	}

	@Nested
	class Servicos {

		@Test
		void substituiServicosETarifas() {
			Profissional profissional = profissional();
			profissional.definirServicos(List.of(new ServicoProfissional(OUTRO_SERVICO_ID, BigDecimal.TEN)));

			profissional.definirServicos(List.of(new ServicoProfissional(SERVICO_ID, new BigDecimal("95"))));

			assertThat(profissional.servicos()).containsExactly(new ServicoProfissional(SERVICO_ID, new BigDecimal("95.00")));
			assertThat(profissional.executa(SERVICO_ID)).isTrue();
			assertThat(profissional.executa(OUTRO_SERVICO_ID)).isFalse();
			assertThat(profissional.tarifaDe(SERVICO_ID)).contains(new BigDecimal("95.00"));
			assertThat(profissional.tarifaDe(OUTRO_SERVICO_ID)).isEmpty();
		}

		@Test
		void recusaServicoRepetidoSemAlterarOsAtuais() {
			Profissional profissional = profissional();
			profissional.definirServicos(List.of(new ServicoProfissional(OUTRO_SERVICO_ID, BigDecimal.TEN)));

			assertThatThrownBy(() -> profissional.definirServicos(List.of(
					new ServicoProfissional(SERVICO_ID, BigDecimal.TEN),
					new ServicoProfissional(SERVICO_ID, BigDecimal.ONE))))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Serviço " + SERVICO_ID + " repetido");
			assertThat(profissional.executa(OUTRO_SERVICO_ID)).isTrue();
		}

		@Test
		void listaNulaLimpa() {
			Profissional profissional = profissional();
			profissional.definirServicos(List.of(new ServicoProfissional(SERVICO_ID, BigDecimal.TEN)));

			profissional.definirServicos(null);

			assertThat(profissional.servicos()).isEmpty();
		}

	}

	@Nested
	class Jornada {

		@Test
		void aceitaManhaETardeNoMesmoDiaOrdenadas() {
			Profissional profissional = profissional();
			PeriodoTrabalho tarde = periodo(DayOfWeek.MONDAY, 13, 18);
			PeriodoTrabalho manha = periodo(DayOfWeek.MONDAY, 8, 12);
			PeriodoTrabalho terca = periodo(DayOfWeek.TUESDAY, 9, 17);

			profissional.definirJornada(List.of(terca, tarde, manha));

			assertThat(profissional.jornadaEm(DayOfWeek.MONDAY)).containsExactly(manha, tarde);
			assertThat(profissional.jornadaEm(DayOfWeek.SUNDAY)).isEmpty();
			assertThat(profissional.jornada()).containsExactly(manha, tarde, terca);
		}

		@Test
		void aceitaPeriodosQueSoEncostam() {
			Profissional profissional = profissional();

			profissional.definirJornada(List.of(periodo(DayOfWeek.MONDAY, 8, 12), periodo(DayOfWeek.MONDAY, 12, 18)));

			assertThat(profissional.jornadaEm(DayOfWeek.MONDAY)).hasSize(2);
		}

		@Test
		void recusaPeriodosSobrepostosSemAlterarAJornadaAtual() {
			Profissional profissional = profissional();
			PeriodoTrabalho atual = periodo(DayOfWeek.FRIDAY, 9, 17);
			profissional.definirJornada(List.of(atual));

			assertThatThrownBy(() -> profissional.definirJornada(List.of(
					periodo(DayOfWeek.MONDAY, 8, 12), periodo(DayOfWeek.MONDAY, 11, 14))))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Períodos de trabalho sobrepostos em MONDAY");
			assertThat(profissional.jornada()).containsExactly(atual);
		}

		@Test
		void listaNulaDeixaSemJornada() {
			Profissional profissional = profissional();
			profissional.definirJornada(List.of(periodo(DayOfWeek.MONDAY, 8, 12)));

			profissional.definirJornada(null);

			assertThat(profissional.jornada()).isEmpty();
		}

	}

	@Nested
	class Reconstituicao {

		@Test
		void reconstituiComOsDadosGravados() {
			UUID id = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
			UUID especialidade = UUID.randomUUID();
			ServicoProfissional servico = new ServicoProfissional(SERVICO_ID, new BigDecimal("80.00"));
			PeriodoTrabalho periodo = periodo(DayOfWeek.MONDAY, 9, 18);

			Profissional profissional = Profissional.reconstituir(id, ESTABELECIMENTO_ID, null, "Bia", "bia@salao.com",
					null, new BigDecimal("4.50"), 12, false, List.of(especialidade), List.of(servico), List.of(periodo));

			assertThat(profissional.id()).isEqualTo(id);
			assertThat(profissional.notaMedia()).isEqualTo(new BigDecimal("4.50"));
			assertThat(profissional.totalAvaliacoes()).isEqualTo(12);
			assertThat(profissional.ativo()).isFalse();
			assertThat(profissional.especialidadeIds()).containsExactly(especialidade);
			assertThat(profissional.servicos()).containsExactly(servico);
			assertThat(profissional.jornada()).containsExactly(periodo);
		}

		@Test
		void reconstituicaoExigeId() {
			assertThatThrownBy(() -> Profissional.reconstituir(null, ESTABELECIMENTO_ID, null, "Bia", "bia@salao.com",
					null, null, 0, true, null, null, null))
					.hasMessage("id é obrigatório");
		}

	}

	@Nested
	class Avaliacoes {

		@Test
		void atualizaANotaMediaEOTotal() {
			Profissional profissional = profissional();

			profissional.atualizarAvaliacoes(NotaMedia.de(13, 3));

			assertThat(profissional.notaMedia()).isEqualTo(new BigDecimal("4.33"));
			assertThat(profissional.totalAvaliacoes()).isEqualTo(3);
		}

	}

}
