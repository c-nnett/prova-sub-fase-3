package br.com.fiap.agendabeleza.api.calendario.adapters.in.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.agendabeleza.api.calendario.application.usecase.CalendarioDoProfissional;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.EmitirAssinaturaDoCalendarioUseCase;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.GerarFeedDoCalendarioUseCase;
import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.TokenDoCalendario;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;

@WebMvcTest(CalendarioController.class)
class CalendarioControllerTest {

	private static final String USUARIO = "X-Usuario-Id";
	private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
	private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJ0123_-9";
	private static final String ENDERECO_DO_FEED = "localhost/api/v1/calendario/feeds/" + TOKEN + ".ics";
	private static final String ASSINAR_PROFISSIONAL = "/api/v1/calendario/assinaturas/profissionais/"
			+ PROFISSIONAL_ID;

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private EmitirAssinaturaDoCalendarioUseCase emitir;
	@MockitoBean
	private GerarFeedDoCalendarioUseCase gerarFeed;

	private void emissaoDevolveOToken(PapelNoCalendario papel, UUID titularId) {
		given(emitir.executar(new EmitirAssinaturaDoCalendarioUseCase.Comando(papel, titularId, USUARIO_ID)))
				.willReturn(TokenDoCalendario.de(TOKEN));
	}

	// Assinaturas

	@Test
	void assinaturaDoProfissionalDevolve201ComAsDuasFormasDaUrl() throws Exception {
		emissaoDevolveOToken(PapelNoCalendario.PROFISSIONAL, PROFISSIONAL_ID);

		mvc.perform(post(ASSINAR_PROFISSIONAL).header(USUARIO, USUARIO_ID))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://" + ENDERECO_DO_FEED))
				.andExpect(jsonPath("$.url").value("http://" + ENDERECO_DO_FEED))
				.andExpect(jsonPath("$.webcal").value("webcal://" + ENDERECO_DO_FEED));
	}

	@Test
	void assinaturaDoClienteUsaOProprioUsuarioComoTitular() throws Exception {
		emissaoDevolveOToken(PapelNoCalendario.CLIENTE, USUARIO_ID);

		mvc.perform(post("/api/v1/calendario/assinaturas/cliente").header(USUARIO, USUARIO_ID))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.url").value("http://" + ENDERECO_DO_FEED));
	}

	@Test
	void assinaturaSemUsuarioDevolve400() throws Exception {
		mvc.perform(post(ASSINAR_PROFISSIONAL)).andExpect(status().isBadRequest());

		verifyNoInteractions(emitir);
	}

	@Test
	void assinaturaDeQuemNaoPodeDevolve403() throws Exception {
		given(emitir.executar(new EmitirAssinaturaDoCalendarioUseCase.Comando(PapelNoCalendario.PROFISSIONAL,
				PROFISSIONAL_ID, USUARIO_ID)))
				.willThrow(new AcessoNegadoException(CalendarioDoProfissional.SOMENTE_GESTOR_OU_PROFISSIONAL));

		mvc.perform(post(ASSINAR_PROFISSIONAL).header(USUARIO, USUARIO_ID))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value(CalendarioDoProfissional.SOMENTE_GESTOR_OU_PROFISSIONAL));
	}

	@Test
	void assinaturaDeProfissionalInexistenteDevolve404() throws Exception {
		given(emitir.executar(new EmitirAssinaturaDoCalendarioUseCase.Comando(PapelNoCalendario.PROFISSIONAL,
				PROFISSIONAL_ID, USUARIO_ID)))
				.willThrow(new RecursoNaoEncontradoException("Profissional " + PROFISSIONAL_ID + " não encontrado"));

		mvc.perform(post(ASSINAR_PROFISSIONAL).header(USUARIO, USUARIO_ID)).andExpect(status().isNotFound());
	}

	// Feed

	@Test
	void feedDevolveOICalendarComoTextCalendarEmUtf8() throws Exception {
		given(gerarFeed.executar(TOKEN)).willReturn("BEGIN:VCALENDAR\r\nX-WR-CALNAME:Salão\r\nEND:VCALENDAR\r\n");

		mvc.perform(get("/api/v1/calendario/feeds/" + TOKEN + ".ics"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "text/calendar;charset=UTF-8"))
				.andExpect(header().string("Content-Disposition", "inline; filename=\"agenda.ics\""))
				.andExpect(content().bytes(
						"BEGIN:VCALENDAR\r\nX-WR-CALNAME:Salão\r\nEND:VCALENDAR\r\n".getBytes(
								StandardCharsets.UTF_8)));
	}

	@Test
	void feedDeTokenDesconhecidoDevolve404() throws Exception {
		given(gerarFeed.executar(TOKEN))
				.willThrow(new RecursoNaoEncontradoException(TokenDoCalendario.CALENDARIO_NAO_ENCONTRADO));

		mvc.perform(get("/api/v1/calendario/feeds/" + TOKEN + ".ics"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value(TokenDoCalendario.CALENDARIO_NAO_ENCONTRADO));
	}

}
