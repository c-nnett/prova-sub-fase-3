package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.OUTRO_USUARIO_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.estabelecimento;
import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.USUARIO_PROFISSIONAL_ID;
import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.profissional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.ConflitoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.EstabelecimentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.profissionais.application.BloqueioAgendaRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.application.ProfissionalRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

class BloqueioUseCasesTest {

	private static final Instant AGORA = Instant.parse("2026-11-29T12:00:00Z");
	private static final Instant INICIO = Instant.parse("2026-11-30T13:00:00Z");
	private static final Instant FIM = Instant.parse("2026-11-30T14:00:00Z");

	private final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
	private final EstabelecimentoRepositoryEmMemoria estabelecimentos = new EstabelecimentoRepositoryEmMemoria();
	private final ProfissionalRepositoryEmMemoria profissionais = new ProfissionalRepositoryEmMemoria();
	private final BloqueioAgendaRepositoryEmMemoria bloqueios = new BloqueioAgendaRepositoryEmMemoria();

	private final Estabelecimento estabelecimento = estabelecimentos.salvar(estabelecimento());
	private final Profissional ana = profissionais.salvar(profissional(estabelecimento.id()));

	/** Agendamentos ativos simulados, no lugar do módulo de agendamentos. */
	private final List<Ocupacao> agendados = new ArrayList<>();

	private final CriarBloqueioUseCase criar = new CriarBloqueioUseCase(estabelecimentos, profissionais, bloqueios,
			(profissionalId, periodo) -> agendados.stream().anyMatch(ocupacao ->
					ocupacao.profissionalId().equals(profissionalId) && ocupacao.intervalo().sobrepoe(periodo)),
			relogio);

	private record Ocupacao(UUID profissionalId, Intervalo intervalo) {
	}

	private BloqueioAgenda bloqueioDaAna() {
		return criar.executar(new CriarBloqueioUseCase.Comando(ana.id(), GESTOR_ID, INICIO, FIM, "Consulta"));
	}

	@Nested
	class Criar {

		@Test
		void gestorCriaBloqueio() {
			BloqueioAgenda bloqueio = bloqueioDaAna();

			assertThat(bloqueio.profissionalId()).isEqualTo(ana.id());
			assertThat(bloqueio.motivo()).isEqualTo("Consulta");
			assertThat(bloqueios.buscarPorId(bloqueio.id())).contains(bloqueio);
		}

		@Test
		void recusaBloqueioSobreAgendamentoAtivo() {
			agendados.add(new Ocupacao(ana.id(), new Intervalo(INICIO.plusSeconds(1800), FIM.plusSeconds(1800))));

			assertThatThrownBy(() -> bloqueioDaAna())
					.isInstanceOf(ConflitoException.class)
					.hasMessage("há agendamento ativo no período do bloqueio");
			assertThat(bloqueios.listarVigentes(ana.id(), AGORA)).isEmpty();
		}

		@Test
		void aceitaBloqueioEncostadoNoAgendamentoOuComAgendamentoDeOutroProfissional() {
			agendados.add(new Ocupacao(ana.id(), new Intervalo(FIM, FIM.plusSeconds(3600))));
			agendados.add(new Ocupacao(UUID.randomUUID(), new Intervalo(INICIO, FIM)));

			assertThat(bloqueios.buscarPorId(bloqueioDaAna().id())).isPresent();
		}

		@Test
		void proprioProfissionalCriaBloqueio() {
			BloqueioAgenda bloqueio = criar.executar(
					new CriarBloqueioUseCase.Comando(ana.id(), USUARIO_PROFISSIONAL_ID, INICIO, FIM, null));

			assertThat(bloqueios.buscarPorId(bloqueio.id())).isPresent();
		}

		@Test
		void recusaOutroUsuario() {
			assertThatThrownBy(() -> criar.executar(
					new CriarBloqueioUseCase.Comando(ana.id(), OUTRO_USUARIO_ID, INICIO, FIM, null)))
					.isInstanceOf(AcessoNegadoException.class);
			assertThat(bloqueios.listarVigentes(ana.id(), AGORA)).isEmpty();
		}

		@Test
		void recusaBloqueioJaEncerradoPeloRelogio() {
			assertThatThrownBy(() -> criar.executar(new CriarBloqueioUseCase.Comando(ana.id(), GESTOR_ID,
					AGORA.minusSeconds(7200), AGORA.minusSeconds(3600), null)))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O bloqueio já terminou");
		}

		@Test
		void recusaProfissionalInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> criar.executar(
					new CriarBloqueioUseCase.Comando(inexistente, GESTOR_ID, INICIO, FIM, null)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Profissional " + inexistente + " não encontrado");
		}

	}

	@Nested
	class Listar {

		private final ListarBloqueiosUseCase listar = new ListarBloqueiosUseCase(estabelecimentos, profissionais,
				bloqueios, relogio);

		@Test
		void listaSomenteBloqueiosQueAindaNaoTerminaramEmOrdem() {
			BloqueioAgenda depois = bloqueios.salvar(BloqueioAgenda.reconstituir(UUID.randomUUID(), ana.id(),
					FIM.plusSeconds(86400), FIM.plusSeconds(90000), null));
			BloqueioAgenda antes = bloqueioDaAna();
			bloqueios.salvar(BloqueioAgenda.reconstituir(UUID.randomUUID(), ana.id(),
					AGORA.minusSeconds(7200), AGORA, "Encerrado"));

			assertThat(listar.executar(ana.id(), USUARIO_PROFISSIONAL_ID)).containsExactly(antes, depois);
		}

		@Test
		void naoListaBloqueiosDeOutroProfissional() {
			Profissional bia = profissionais.salvar(Profissional.cadastrar(estabelecimento.id(), null, "Bia",
					"bia@salao.com", null));
			bloqueioDaAna();

			assertThat(listar.executar(bia.id(), GESTOR_ID)).isEmpty();
		}

		@Test
		void recusaOutroUsuario() {
			assertThatThrownBy(() -> listar.executar(ana.id(), OUTRO_USUARIO_ID))
					.isInstanceOf(AcessoNegadoException.class);
		}

	}

	@Nested
	class Remover {

		private final RemoverBloqueioUseCase remover = new RemoverBloqueioUseCase(estabelecimentos, profissionais,
				bloqueios);

		@Test
		void gestorRemoveBloqueio() {
			BloqueioAgenda bloqueio = bloqueioDaAna();

			remover.executar(new RemoverBloqueioUseCase.Comando(ana.id(), bloqueio.id(), GESTOR_ID));

			assertThat(bloqueios.buscarPorId(bloqueio.id())).isEmpty();
		}

		@Test
		void recusaBloqueioInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> remover.executar(
					new RemoverBloqueioUseCase.Comando(ana.id(), inexistente, GESTOR_ID)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Bloqueio " + inexistente + " não encontrado");
		}

		@Test
		void recusaBloqueioDeOutroProfissionalComoInexistente() {
			Profissional bia = profissionais.salvar(Profissional.cadastrar(estabelecimento.id(), null, "Bia",
					"bia@salao.com", null));
			BloqueioAgenda daAna = bloqueioDaAna();

			assertThatThrownBy(() -> remover.executar(
					new RemoverBloqueioUseCase.Comando(bia.id(), daAna.id(), GESTOR_ID)))
					.isInstanceOf(RecursoNaoEncontradoException.class);
			assertThat(bloqueios.buscarPorId(daAna.id())).isPresent();
		}

		@Test
		void recusaOutroUsuario() {
			BloqueioAgenda bloqueio = bloqueioDaAna();

			assertThatThrownBy(() -> remover.executar(
					new RemoverBloqueioUseCase.Comando(ana.id(), bloqueio.id(), OUTRO_USUARIO_ID)))
					.isInstanceOf(AcessoNegadoException.class);
			assertThat(bloqueios.buscarPorId(bloqueio.id())).isPresent();
		}

	}

}
