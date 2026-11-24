package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.OUTRO_USUARIO_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.estabelecimento;
import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.USUARIO_PROFISSIONAL_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.EstabelecimentoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.ServicoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.EspecialidadeRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.application.ProfissionalRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

class ProfissionalUseCasesTest {

	private final EstabelecimentoRepositoryEmMemoria estabelecimentos = new EstabelecimentoRepositoryEmMemoria();
	private final ServicoRepositoryEmMemoria servicos = new ServicoRepositoryEmMemoria();
	private final ProfissionalRepositoryEmMemoria profissionais = new ProfissionalRepositoryEmMemoria();
	private final EspecialidadeRepositoryEmMemoria especialidades = new EspecialidadeRepositoryEmMemoria();

	private final CadastrarProfissionalUseCase cadastrar = new CadastrarProfissionalUseCase(estabelecimentos, profissionais);

	private final Estabelecimento estabelecimento = estabelecimentos.salvar(estabelecimento());

	private Profissional cadastrarAna() {
		return cadastrar.executar(new CadastrarProfissionalUseCase.Comando(estabelecimento.id(), GESTOR_ID,
				USUARIO_PROFISSIONAL_ID, "Ana Souza", "ana@salao.com", null));
	}

	private Servico salvarServico(UUID estabelecimentoId, String nome, String precoBase) {
		return servicos.salvar(Servico.cadastrar(estabelecimentoId, nome, null, CategoriaServico.CABELO, 60,
				new BigDecimal(precoBase)));
	}

	private static PeriodoTrabalho periodo(DayOfWeek dia, int inicio, int fim) {
		return new PeriodoTrabalho(dia, LocalTime.of(inicio, 0), LocalTime.of(fim, 0));
	}

	@Nested
	class Cadastrar {

		@Test
		void gestorCadastraProfissionalNoEstabelecimento() {
			Profissional profissional = cadastrarAna();

			assertThat(profissional.estabelecimentoId()).isEqualTo(estabelecimento.id());
			assertThat(profissional.usuarioId()).isEqualTo(USUARIO_PROFISSIONAL_ID);
			assertThat(profissionais.buscarPorId(profissional.id())).contains(profissional);
		}

		@Test
		void recusaOutroUsuario() {
			assertThatThrownBy(() -> cadastrar.executar(new CadastrarProfissionalUseCase.Comando(estabelecimento.id(),
					OUTRO_USUARIO_ID, null, "Ana", "ana@salao.com", null)))
					.isInstanceOf(AcessoNegadoException.class);
			assertThat(profissionais.listarPorEstabelecimento(estabelecimento.id())).isEmpty();
		}

		@Test
		void recusaEstabelecimentoInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> cadastrar.executar(new CadastrarProfissionalUseCase.Comando(inexistente,
					GESTOR_ID, null, "Ana", "ana@salao.com", null)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Estabelecimento " + inexistente + " não encontrado");
		}

	}

	@Nested
	class Atualizar {

		private final AtualizarProfissionalUseCase atualizar = new AtualizarProfissionalUseCase(estabelecimentos, profissionais);

		@Test
		void gestorAtualizaPerfil() {
			Profissional profissional = cadastrarAna();

			Profissional atualizado = atualizar.executar(new AtualizarProfissionalUseCase.Comando(profissional.id(),
					GESTOR_ID, "Ana S.", "ana.s@salao.com", "Especialista em loiros", false));

			assertThat(atualizado.nome()).isEqualTo("Ana S.");
			assertThat(atualizado.ativo()).isFalse();
			assertThat(profissionais.buscarPorId(profissional.id())).contains(atualizado);
		}

		@Test
		void proprioProfissionalAtualizaPerfil() {
			Profissional profissional = cadastrarAna();

			Profissional atualizado = atualizar.executar(new AtualizarProfissionalUseCase.Comando(profissional.id(),
					USUARIO_PROFISSIONAL_ID, "Ana", "ana@salao.com", "Nova bio", true));

			assertThat(atualizado.bio()).isEqualTo("Nova bio");
		}

		@Test
		void recusaOutroUsuario() {
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> atualizar.executar(new AtualizarProfissionalUseCase.Comando(profissional.id(),
					OUTRO_USUARIO_ID, "Outro", "outro@salao.com", null, true)))
					.isInstanceOf(AcessoNegadoException.class);
			assertThat(profissional.nome()).isEqualTo("Ana Souza");
		}

		@Test
		void recusaProfissionalInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> atualizar.executar(new AtualizarProfissionalUseCase.Comando(inexistente,
					GESTOR_ID, "Ana", "ana@salao.com", null, true)))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Profissional " + inexistente + " não encontrado");
		}

	}

	@Nested
	class Consultar {

		private final ConsultarProfissionalUseCase consultar = new ConsultarProfissionalUseCase(profissionais);
		private final ListarProfissionaisUseCase listar = new ListarProfissionaisUseCase(estabelecimentos, profissionais);

		@Test
		void consultaPorId() {
			Profissional profissional = cadastrarAna();

			assertThat(consultar.executar(profissional.id())).isEqualTo(profissional);
		}

		@Test
		void consultaRecusaInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> consultar.executar(inexistente))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Profissional " + inexistente + " não encontrado");
		}

		@Test
		void listaSomenteOsAtivosDoEstabelecimento() {
			Profissional ativo = cadastrarAna();
			Profissional inativo = cadastrarAna();
			inativo.atualizarPerfil("Inativa", "inativa@salao.com", null, false);
			profissionais.salvar(Profissional.cadastrar(UUID.randomUUID(), null, "Outro salão", "x@y.com", null));

			assertThat(listar.executar(estabelecimento.id())).containsExactly(ativo);
		}

		@Test
		void listagemRecusaEstabelecimentoInexistente() {
			UUID inexistente = UUID.randomUUID();

			assertThatThrownBy(() -> listar.executar(inexistente))
					.isInstanceOf(RecursoNaoEncontradoException.class)
					.hasMessage("Estabelecimento " + inexistente + " não encontrado");
		}

	}

	@Nested
	class Especialidades {

		private final DefinirEspecialidadesUseCase definir = new DefinirEspecialidadesUseCase(
				estabelecimentos, profissionais, especialidades);
		private final ListarEspecialidadesUseCase listar = new ListarEspecialidadesUseCase(especialidades);

		@Test
		void defineEspecialidadesDoCatalogo() {
			Especialidade colorimetria = especialidades.adicionar("Colorimetria");
			Especialidade corte = especialidades.adicionar("Corte");
			Profissional profissional = cadastrarAna();

			Profissional atualizado = definir.executar(new DefinirEspecialidadesUseCase.Comando(profissional.id(),
					USUARIO_PROFISSIONAL_ID, List.of(colorimetria.id(), corte.id())));

			assertThat(atualizado.especialidadeIds()).containsExactly(colorimetria.id(), corte.id());
		}

		@Test
		void recusaEspecialidadeInexistente() {
			Especialidade corte = especialidades.adicionar("Corte");
			UUID inexistente = UUID.randomUUID();
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> definir.executar(new DefinirEspecialidadesUseCase.Comando(profissional.id(),
					GESTOR_ID, List.of(corte.id(), inexistente))))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Especialidade " + inexistente + " não encontrada");
			assertThat(profissional.especialidadeIds()).isEmpty();
		}

		@Test
		void recusaOutroUsuario() {
			Especialidade corte = especialidades.adicionar("Corte");
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> definir.executar(new DefinirEspecialidadesUseCase.Comando(profissional.id(),
					OUTRO_USUARIO_ID, List.of(corte.id()))))
					.isInstanceOf(AcessoNegadoException.class);
		}

		@Test
		void listaCatalogoOrdenadoPorNome() {
			Especialidade manicure = especialidades.adicionar("Manicure");
			Especialidade barba = especialidades.adicionar("Barba");
			Especialidade corte = especialidades.adicionar("Corte");

			assertThat(listar.executar()).containsExactly(barba, corte, manicure);
		}

	}

	@Nested
	class ServicosETarifas {

		private final DefinirServicosProfissionalUseCase definir = new DefinirServicosProfissionalUseCase(
				estabelecimentos, profissionais, servicos);

		@Test
		void tarifaInformadaSobrescreveEAusenteAssumeOPrecoBase() {
			Servico corte = salvarServico(estabelecimento.id(), "Corte", "80");
			Servico escova = salvarServico(estabelecimento.id(), "Escova", "50");
			Profissional profissional = cadastrarAna();

			Profissional atualizado = definir.executar(new DefinirServicosProfissionalUseCase.Comando(profissional.id(),
					GESTOR_ID, List.of(
							new DefinirServicosProfissionalUseCase.Item(corte.id(), new BigDecimal("95")),
							new DefinirServicosProfissionalUseCase.Item(escova.id(), null))));

			assertThat(atualizado.servicos()).containsExactly(
					new ServicoProfissional(corte.id(), new BigDecimal("95.00")),
					new ServicoProfissional(escova.id(), new BigDecimal("50.00")));
			assertThat(profissionais.buscarPorId(profissional.id())).contains(atualizado);
		}

		@Test
		void recusaServicoInexistente() {
			UUID inexistente = UUID.randomUUID();
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> definir.executar(new DefinirServicosProfissionalUseCase.Comando(profissional.id(),
					GESTOR_ID, List.of(new DefinirServicosProfissionalUseCase.Item(inexistente, null)))))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Serviço " + inexistente + " não encontrado no estabelecimento");
		}

		@Test
		void recusaServicoDeOutroEstabelecimento() {
			Servico deOutro = salvarServico(UUID.randomUUID(), "Corte", "80");
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> definir.executar(new DefinirServicosProfissionalUseCase.Comando(profissional.id(),
					GESTOR_ID, List.of(new DefinirServicosProfissionalUseCase.Item(deOutro.id(), null)))))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Serviço " + deOutro.id() + " não encontrado no estabelecimento");
		}

		@Test
		void recusaServicoInativo() {
			Servico inativo = salvarServico(estabelecimento.id(), "Antigo", "30");
			inativo.atualizar("Antigo", null, CategoriaServico.CABELO, 30, BigDecimal.TEN, false);
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> definir.executar(new DefinirServicosProfissionalUseCase.Comando(profissional.id(),
					GESTOR_ID, List.of(new DefinirServicosProfissionalUseCase.Item(inativo.id(), null)))))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Serviço " + inativo.id() + " está inativo");
		}

		@Test
		void recusaOutroUsuario() {
			Servico corte = salvarServico(estabelecimento.id(), "Corte", "80");
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> definir.executar(new DefinirServicosProfissionalUseCase.Comando(profissional.id(),
					OUTRO_USUARIO_ID, List.of(new DefinirServicosProfissionalUseCase.Item(corte.id(), null)))))
					.isInstanceOf(AcessoNegadoException.class);
		}

	}

	@Nested
	class Jornada {

		private final DefinirJornadaUseCase definir = new DefinirJornadaUseCase(estabelecimentos, profissionais);

		Jornada() {
			estabelecimento.definirHorarios(List.of(
					new HorarioFuncionamento(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(19, 0)),
					new HorarioFuncionamento(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(19, 0))));
		}

		@Test
		void defineJornadaComAlmocoDentroDoFuncionamento() {
			Profissional profissional = cadastrarAna();
			PeriodoTrabalho manha = periodo(DayOfWeek.MONDAY, 9, 12);
			PeriodoTrabalho tarde = periodo(DayOfWeek.MONDAY, 13, 19);

			Profissional atualizado = definir.executar(new DefinirJornadaUseCase.Comando(profissional.id(),
					USUARIO_PROFISSIONAL_ID, List.of(tarde, manha)));

			assertThat(atualizado.jornadaEm(DayOfWeek.MONDAY)).containsExactly(manha, tarde);
			assertThat(profissionais.buscarPorId(profissional.id())).contains(atualizado);
		}

		@Test
		void recusaPeriodoEmDiaFechado() {
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> definir.executar(new DefinirJornadaUseCase.Comando(profissional.id(),
					GESTOR_ID, List.of(periodo(DayOfWeek.SUNDAY, 9, 12)))))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("O estabelecimento não funciona em SUNDAY");
			assertThat(profissional.jornada()).isEmpty();
		}

		@Test
		void recusaPeriodoForaDoHorarioDeFuncionamento() {
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> definir.executar(new DefinirJornadaUseCase.Comando(profissional.id(),
					GESTOR_ID, List.of(periodo(DayOfWeek.TUESDAY, 14, 20)))))
					.isInstanceOf(RegraNegocioException.class)
					.hasMessage("Período de TUESDAY das 14:00 às 20:00 fora do funcionamento do estabelecimento");
		}

		@Test
		void recusaOutroUsuario() {
			Profissional profissional = cadastrarAna();

			assertThatThrownBy(() -> definir.executar(new DefinirJornadaUseCase.Comando(profissional.id(),
					OUTRO_USUARIO_ID, List.of(periodo(DayOfWeek.MONDAY, 9, 12)))))
					.isInstanceOf(AcessoNegadoException.class);
		}

		@Test
		void listaVaziaLimpaAJornada() {
			Profissional profissional = cadastrarAna();
			definir.executar(new DefinirJornadaUseCase.Comando(profissional.id(), GESTOR_ID,
					List.of(periodo(DayOfWeek.MONDAY, 9, 12))));

			Profissional atualizado = definir.executar(new DefinirJornadaUseCase.Comando(profissional.id(), GESTOR_ID,
					List.of()));

			assertThat(atualizado.jornada()).isEmpty();
		}

	}

	@Nested
	class Detalhar {

		private final DetalharProfissionaisUseCase detalhar = new DetalharProfissionaisUseCase(especialidades);

		@Test
		void trazAsEspecialidadesOrdenadasPorNome() {
			Especialidade manicure = especialidades.adicionar("Manicure");
			Especialidade barba = especialidades.adicionar("barba");
			Especialidade coloracao = especialidades.adicionar("Coloração");
			Profissional profissional = cadastrarAna();
			// A ordem de definição não é preservada pelo banco, então a resposta usa a ordem do nome.
			profissional.definirEspecialidades(List.of(manicure.id(), coloracao.id(), barba.id()));

			PerfilProfissional perfil = detalhar.executar(profissional);

			assertThat(perfil.profissional()).isEqualTo(profissional);
			assertThat(perfil.especialidades()).containsExactly(barba, coloracao, manicure);
		}

		@Test
		void detalhaVariosProfissionaisComUmaUnicaBuscaNoCatalogo() {
			Especialidade corte = especialidades.adicionar("Corte");
			Especialidade manicure = especialidades.adicionar("Manicure");
			Profissional ana = cadastrarAna();
			ana.definirEspecialidades(List.of(corte.id()));
			Profissional bia = cadastrarAna();
			bia.definirEspecialidades(List.of(manicure.id(), corte.id()));
			Profissional semEspecialidade = cadastrarAna();

			List<PerfilProfissional> perfis = detalhar.executar(List.of(ana, bia, semEspecialidade));

			assertThat(perfis).extracting(PerfilProfissional::profissional).containsExactly(ana, bia, semEspecialidade);
			assertThat(perfis.get(0).especialidades()).containsExactly(corte);
			assertThat(perfis.get(1).especialidades()).containsExactly(corte, manicure);
			assertThat(perfis.get(2).especialidades()).isEmpty();
			assertThat(especialidades.buscasPorIds()).isEqualTo(1);
		}

		@Test
		void listaVaziaNaoConsultaOCatalogo() {
			assertThat(detalhar.executar(List.of())).isEmpty();
			assertThat(especialidades.buscasPorIds()).isZero();
		}

		@Test
		void ignoraEspecialidadeQueSaiuDoCatalogo() {
			Especialidade corte = especialidades.adicionar("Corte");
			Profissional profissional = cadastrarAna();
			profissional.definirEspecialidades(List.of(UUID.randomUUID(), corte.id()));

			assertThat(detalhar.executar(profissional).especialidades()).containsExactly(corte);
		}

	}

}
