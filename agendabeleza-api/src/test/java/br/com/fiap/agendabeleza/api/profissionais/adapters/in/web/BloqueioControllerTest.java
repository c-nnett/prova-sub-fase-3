package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.CriarBloqueioUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ListarBloqueiosUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.RemoverBloqueioUseCase;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;

@WebMvcTest(BloqueioController.class)
class BloqueioControllerTest {

	private static final String USUARIO = "X-Usuario-Id";
	private static final UUID GESTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final String URL = "/api/v1/profissionais/" + PROFISSIONAL_ID + "/bloqueios";
	private static final Instant INICIO = Instant.parse("2026-11-30T13:00:00Z");
	private static final Instant FIM = Instant.parse("2026-11-30T14:00:00Z");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CriarBloqueioUseCase criar;
	@MockitoBean
	private ListarBloqueiosUseCase listar;
	@MockitoBean
	private RemoverBloqueioUseCase remover;

	private final BloqueioAgenda bloqueio = BloqueioAgenda.reconstituir(
			UUID.fromString("00000000-0000-0000-0000-0000000000d1"), PROFISSIONAL_ID, INICIO, FIM, "Consulta");

	@Test
	void criaBloqueioComLocationEHorariosEmUtc() throws Exception {
		given(criar.executar(any())).willReturn(bloqueio);

		mvc.perform(post(URL).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON).content("""
				{ "inicio": "2026-11-30T10:00:00-03:00", "fim": "2026-11-30T11:00:00-03:00", "motivo": "Consulta" }
				"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith(URL + "/" + bloqueio.id())))
				.andExpect(jsonPath("$.id").value(bloqueio.id().toString()))
				.andExpect(jsonPath("$.inicio").value("2026-11-30T13:00:00Z"))
				.andExpect(jsonPath("$.fim").value("2026-11-30T14:00:00Z"))
				.andExpect(jsonPath("$.motivo").value("Consulta"));

		ArgumentCaptor<CriarBloqueioUseCase.Comando> comando = ArgumentCaptor.forClass(CriarBloqueioUseCase.Comando.class);
		verify(criar).executar(comando.capture());
		assertThat(comando.getValue().profissionalId()).isEqualTo(PROFISSIONAL_ID);
		assertThat(comando.getValue().usuarioId()).isEqualTo(GESTOR_ID);
		assertThat(comando.getValue().inicio()).isEqualTo(INICIO);
		assertThat(comando.getValue().fim()).isEqualTo(FIM);
		assertThat(comando.getValue().motivo()).isEqualTo("Consulta");
	}

	@Test
	void recusaBloqueioSemInicioEFimEComMotivoLongo() throws Exception {
		mvc.perform(post(URL).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"motivo\": \"%s\" }".formatted("x".repeat(151))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("fim"))
				.andExpect(jsonPath("$.erros[1].campo").value("inicio"))
				.andExpect(jsonPath("$.erros[2].campo").value("motivo"));
	}

	@Test
	void recusaBloqueioSemCabecalhoDoUsuario() throws Exception {
		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"inicio\": \"2026-11-30T10:00:00Z\", \"fim\": \"2026-11-30T11:00:00Z\" }"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void bloqueioJaEncerradoDevolve422() throws Exception {
		given(criar.executar(any())).willThrow(new RegraNegocioException("O bloqueio já terminou"));

		mvc.perform(post(URL).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"inicio\": \"2020-11-30T10:00:00Z\", \"fim\": \"2020-11-30T11:00:00Z\" }"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("O bloqueio já terminou"));
	}

	@Test
	void outroUsuarioDevolve403() throws Exception {
		given(criar.executar(any())).willThrow(new AcessoNegadoException("Somente o gestor"));

		mvc.perform(post(URL).header(USUARIO, UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"inicio\": \"2026-11-30T10:00:00Z\", \"fim\": \"2026-11-30T11:00:00Z\" }"))
				.andExpect(status().isForbidden());
	}

	@Test
	void listaBloqueiosVigentes() throws Exception {
		given(listar.executar(PROFISSIONAL_ID, GESTOR_ID)).willReturn(List.of(bloqueio));

		mvc.perform(get(URL).header(USUARIO, GESTOR_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(bloqueio.id().toString()))
				.andExpect(jsonPath("$[0].inicio").value("2026-11-30T13:00:00Z"));
	}

	@Test
	void removeBloqueioCom204() throws Exception {
		mvc.perform(delete(URL + "/" + bloqueio.id()).header(USUARIO, GESTOR_ID))
				.andExpect(status().isNoContent());

		verify(remover).executar(new RemoverBloqueioUseCase.Comando(PROFISSIONAL_ID, bloqueio.id(), GESTOR_ID));
	}

	@Test
	void removerBloqueioInexistenteDevolve404() throws Exception {
		UUID inexistente = UUID.randomUUID();
		willThrow(new RecursoNaoEncontradoException("Bloqueio " + inexistente + " não encontrado"))
				.given(remover).executar(any());

		mvc.perform(delete(URL + "/" + inexistente).header(USUARIO, GESTOR_ID))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

}
