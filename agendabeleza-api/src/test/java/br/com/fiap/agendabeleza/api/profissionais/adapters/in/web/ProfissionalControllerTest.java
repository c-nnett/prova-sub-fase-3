package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web;

import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.ESTABELECIMENTO_ID;
import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.USUARIO_PROFISSIONAL_ID;
import static br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture.profissional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.AtualizarProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.CadastrarProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ConsultarProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirEspecialidadesUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirJornadaUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirServicosProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DetalharProfissionaisUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ListarProfissionaisUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.PerfilProfissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

@WebMvcTest(ProfissionalController.class)
class ProfissionalControllerTest {

	private static final String USUARIO = "X-Usuario-Id";
	private static final UUID GESTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
	private static final String URL_ESTABELECIMENTO = "/api/v1/estabelecimentos/" + ESTABELECIMENTO_ID + "/profissionais";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CadastrarProfissionalUseCase cadastrar;
	@MockitoBean
	private ListarProfissionaisUseCase listar;
	@MockitoBean
	private ConsultarProfissionalUseCase consultar;
	@MockitoBean
	private AtualizarProfissionalUseCase atualizar;
	@MockitoBean
	private DefinirEspecialidadesUseCase definirEspecialidades;
	@MockitoBean
	private DefinirServicosProfissionalUseCase definirServicos;
	@MockitoBean
	private DefinirJornadaUseCase definirJornada;
	@MockitoBean
	private DetalharProfissionaisUseCase detalhar;

	private final Especialidade coloracao = Especialidade.reconstituir(
			UUID.fromString("e5000000-0000-4000-8000-000000000002"), "Coloração");

	private Profissional ana;

	@BeforeEach
	void prepararPerfil() {
		ana = profissional();
		ana.definirEspecialidades(List.of(coloracao.id()));
		ana.definirServicos(List.of(new ServicoProfissional(SERVICO_ID, new BigDecimal("95.00"))));
		ana.definirJornada(List.of(new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))));
		given(detalhar.executar(any(Profissional.class)))
				.willAnswer(invocacao -> new PerfilProfissional(invocacao.getArgument(0), List.of(coloracao)));
		given(detalhar.executar(anyList())).willAnswer(invocacao -> {
			List<Profissional> profissionais = invocacao.getArgument(0);
			return profissionais.stream().map(p -> new PerfilProfissional(p, List.of(coloracao))).toList();
		});
	}

	private String url(Profissional profissional) {
		return "/api/v1/profissionais/" + profissional.id();
	}

	@Test
	void cadastraProfissionalComLocationDoPerfil() throws Exception {
		given(cadastrar.executar(any())).willReturn(ana);

		mvc.perform(post(URL_ESTABELECIMENTO).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "usuarioId": "%s", "nome": "Ana Souza", "email": "ana@salao.com", "bio": "Colorista" }
						""".formatted(USUARIO_PROFISSIONAL_ID)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/v1/profissionais/" + ana.id())))
				.andExpect(jsonPath("$.id").value(ana.id().toString()))
				.andExpect(jsonPath("$.estabelecimentoId").value(ESTABELECIMENTO_ID.toString()))
				.andExpect(jsonPath("$.email").value("ana@salao.com"))
				.andExpect(jsonPath("$.ativo").value(true))
				.andExpect(jsonPath("$.totalAvaliacoes").value(0))
				.andExpect(jsonPath("$.especialidades[0].nome").value("Coloração"))
				.andExpect(jsonPath("$.servicos[0].servicoId").value(SERVICO_ID.toString()))
				.andExpect(jsonPath("$.servicos[0].tarifa").value(95.0))
				.andExpect(jsonPath("$.jornada[0].diaSemana").value("MONDAY"))
				.andExpect(jsonPath("$.jornada[0].inicio").value("09:00"))
				.andExpect(jsonPath("$.jornada[0].fim").value("12:00"));

		ArgumentCaptor<CadastrarProfissionalUseCase.Comando> comando =
				ArgumentCaptor.forClass(CadastrarProfissionalUseCase.Comando.class);
		verify(cadastrar).executar(comando.capture());
		assertThat(comando.getValue().estabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
		assertThat(comando.getValue().usuarioId()).isEqualTo(GESTOR_ID);
		assertThat(comando.getValue().usuarioProfissionalId()).isEqualTo(USUARIO_PROFISSIONAL_ID);
		assertThat(comando.getValue().email()).isEqualTo("ana@salao.com");
	}

	@Test
	void recusaCadastroComEmailInvalidoENomeVazio() throws Exception {
		mvc.perform(post(URL_ESTABELECIMENTO).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"nome\": \"\", \"email\": \"ana.salao.com\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("email"))
				.andExpect(jsonPath("$.erros[1].campo").value("nome"));
	}

	@Test
	void recusaCadastroSemCabecalhoDoUsuario() throws Exception {
		mvc.perform(post(URL_ESTABELECIMENTO).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"nome\": \"Ana\", \"email\": \"ana@salao.com\" }"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void usuarioJaVinculadoAOutroProfissionalDevolve409() throws Exception {
		given(cadastrar.executar(any())).willThrow(new DataIntegrityViolationException("profissional_usuario_uk"));

		mvc.perform(post(URL_ESTABELECIMENTO).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"usuarioId\": \"%s\", \"nome\": \"Ana\", \"email\": \"ana@salao.com\" }"
						.formatted(USUARIO_PROFISSIONAL_ID)))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void listaProfissionaisSemEmail() throws Exception {
		given(listar.executar(ESTABELECIMENTO_ID)).willReturn(List.of(ana));

		mvc.perform(get(URL_ESTABELECIMENTO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(ana.id().toString()))
				.andExpect(jsonPath("$[0].nome").value("Ana Souza"))
				.andExpect(jsonPath("$[0].especialidades[0].nome").value("Coloração"))
				.andExpect(jsonPath("$[0].email").doesNotExist());
	}

	@Test
	void consultaPerfilPublicoSemEmail() throws Exception {
		given(consultar.executar(ana.id())).willReturn(ana);

		mvc.perform(get(url(ana)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Souza"))
				.andExpect(jsonPath("$.bio").value("Colorista há 10 anos"))
				.andExpect(jsonPath("$.email").doesNotExist());
	}

	@Test
	void perfilInexistenteDevolve404() throws Exception {
		UUID inexistente = UUID.randomUUID();
		given(consultar.executar(inexistente))
				.willThrow(new RecursoNaoEncontradoException("Profissional " + inexistente + " não encontrado"));

		mvc.perform(get("/api/v1/profissionais/" + inexistente))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void atualizaPerfil() throws Exception {
		given(atualizar.executar(any())).willReturn(ana);

		mvc.perform(put(url(ana)).header(USUARIO, USUARIO_PROFISSIONAL_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"nome\": \"Ana S.\", \"email\": \"ana@salao.com\", \"ativo\": false }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ana@salao.com"));

		ArgumentCaptor<AtualizarProfissionalUseCase.Comando> comando =
				ArgumentCaptor.forClass(AtualizarProfissionalUseCase.Comando.class);
		verify(atualizar).executar(comando.capture());
		assertThat(comando.getValue().profissionalId()).isEqualTo(ana.id());
		assertThat(comando.getValue().usuarioId()).isEqualTo(USUARIO_PROFISSIONAL_ID);
		assertThat(comando.getValue().nome()).isEqualTo("Ana S.");
		assertThat(comando.getValue().ativo()).isFalse();
	}

	@Test
	void atualizacaoSemAtivoDevolve400() throws Exception {
		mvc.perform(put(url(ana)).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"nome\": \"Ana\", \"email\": \"ana@salao.com\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("ativo"));
	}

	@Test
	void outroUsuarioDevolve403() throws Exception {
		given(atualizar.executar(any())).willThrow(new AcessoNegadoException("Somente o gestor"));

		mvc.perform(put(url(ana)).header(USUARIO, UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"nome\": \"Ana\", \"email\": \"ana@salao.com\", \"ativo\": true }"))
				.andExpect(status().isForbidden());
	}

	@Test
	void defineEspecialidades() throws Exception {
		given(definirEspecialidades.executar(any())).willReturn(ana);

		mvc.perform(put(url(ana) + "/especialidades").header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"especialidadeIds\": [\"%s\"] }".formatted(coloracao.id())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.especialidades[0].id").value(coloracao.id().toString()));

		ArgumentCaptor<DefinirEspecialidadesUseCase.Comando> comando =
				ArgumentCaptor.forClass(DefinirEspecialidadesUseCase.Comando.class);
		verify(definirEspecialidades).executar(comando.capture());
		assertThat(comando.getValue().profissionalId()).isEqualTo(ana.id());
		assertThat(comando.getValue().usuarioId()).isEqualTo(GESTOR_ID);
		assertThat(comando.getValue().especialidadeIds()).containsExactly(coloracao.id());
	}

	@Test
	void defineServicosComTarifaOpcional() throws Exception {
		UUID escova = UUID.randomUUID();
		given(definirServicos.executar(any())).willReturn(ana);

		mvc.perform(put(url(ana) + "/servicos").header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "servicos": [ { "servicoId": "%s", "tarifa": 95.00 }, { "servicoId": "%s" } ] }
						""".formatted(SERVICO_ID, escova)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.servicos[0].tarifa").value(95.0));

		ArgumentCaptor<DefinirServicosProfissionalUseCase.Comando> comando =
				ArgumentCaptor.forClass(DefinirServicosProfissionalUseCase.Comando.class);
		verify(definirServicos).executar(comando.capture());
		assertThat(comando.getValue().servicos()).containsExactly(
				new DefinirServicosProfissionalUseCase.Item(SERVICO_ID, new BigDecimal("95.00")),
				new DefinirServicosProfissionalUseCase.Item(escova, null));
	}

	@Test
	void recusaTarifaNegativa() throws Exception {
		mvc.perform(put(url(ana) + "/servicos").header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"servicos\": [ { \"servicoId\": \"%s\", \"tarifa\": -1 } ] }".formatted(SERVICO_ID)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("servicos[0].tarifa"));
	}

	@Test
	void defineJornada() throws Exception {
		given(definirJornada.executar(any())).willReturn(ana);

		mvc.perform(put(url(ana) + "/disponibilidades").header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "periodos": [
						    { "diaSemana": "MONDAY", "inicio": "09:00", "fim": "12:00" },
						    { "diaSemana": "MONDAY", "inicio": "13:00", "fim": "18:00" } ] }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.jornada[0].inicio").value("09:00"));

		ArgumentCaptor<DefinirJornadaUseCase.Comando> comando =
				ArgumentCaptor.forClass(DefinirJornadaUseCase.Comando.class);
		verify(definirJornada).executar(comando.capture());
		assertThat(comando.getValue().periodos()).containsExactly(
				new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
				new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(18, 0)));
	}

	@Test
	void jornadaEmDiaFechadoDevolve422() throws Exception {
		given(definirJornada.executar(any()))
				.willThrow(new RegraNegocioException("O estabelecimento não funciona em SUNDAY"));

		mvc.perform(put(url(ana) + "/disponibilidades").header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"periodos\": [ { \"diaSemana\": \"SUNDAY\", \"inicio\": \"09:00\", \"fim\": \"12:00\" } ] }"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("O estabelecimento não funciona em SUNDAY"));
	}

	@Test
	void recusaPeriodoSemDia() throws Exception {
		mvc.perform(put(url(ana) + "/disponibilidades").header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"periodos\": [ { \"inicio\": \"09:00\", \"fim\": \"12:00\" } ] }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("periodos[0].diaSemana"));
	}

}
