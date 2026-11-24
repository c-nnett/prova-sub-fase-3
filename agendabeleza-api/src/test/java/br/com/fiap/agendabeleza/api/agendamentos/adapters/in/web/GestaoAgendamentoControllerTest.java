package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CancelarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ComandoDeAlteracao;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConfirmarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.TransicaoInvalidaException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;

@WebMvcTest(GestaoAgendamentoController.class)
class GestaoAgendamentoControllerTest {

	private static final String USUARIO = "X-Usuario-Id";
	private static final String URL = "/api/v1/agendamentos";
	private static final UUID GESTOR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
	private static final Instant INICIO = Instant.parse("2030-03-04T13:00:00Z");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConfirmarAgendamentoUseCase confirmar;
	@MockitoBean
	private CancelarAgendamentoUseCase cancelar;

	private final UUID agendamentoId = UUID.randomUUID();

	private Agendamento agendamentoCom(StatusAgendamento status) {
		return Agendamento.reconstituir(agendamentoId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), INICIO, INICIO.plusSeconds(3600), status, new BigDecimal("95.00"), null, 1L);
	}

	private MockHttpServletRequestBuilder acao(String nomeDaAcao) {
		return post(URL + "/" + agendamentoId + "/" + nomeDaAcao).header(USUARIO, GESTOR_ID);
	}

	private ComandoDeAlteracao.Simples comandoDoGestor() {
		return new ComandoDeAlteracao.Simples(agendamentoId, GESTOR_ID);
	}

	// Confirmar

	@Test
	void confirmaEDevolveOAgendamento() throws Exception {
		given(confirmar.executar(comandoDoGestor())).willReturn(agendamentoCom(StatusAgendamento.CONFIRMADO));

		mvc.perform(acao("confirmar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(agendamentoId.toString()))
				.andExpect(jsonPath("$.status").value("CONFIRMADO"));
	}

	@Test
	void confirmarSemCabecalhoDoUsuarioDevolve400() throws Exception {
		mvc.perform(post(URL + "/" + agendamentoId + "/confirmar"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(confirmar);
	}

	@Test
	void confirmarPorQuemNaoPodeDevolve403() throws Exception {
		given(confirmar.executar(comandoDoGestor())).willThrow(new AcessoNegadoException("negado"));

		mvc.perform(acao("confirmar"))
				.andExpect(status().isForbidden());
	}

	@Test
	void confirmarInexistenteDevolve404() throws Exception {
		given(confirmar.executar(comandoDoGestor())).willThrow(new RecursoNaoEncontradoException("inexistente"));

		mvc.perform(acao("confirmar"))
				.andExpect(status().isNotFound());
	}

	@Test
	void confirmarForaDoDiagramaDevolve422() throws Exception {
		given(confirmar.executar(comandoDoGestor()))
				.willThrow(new TransicaoInvalidaException("confirmar", StatusAgendamento.CANCELADO));

		mvc.perform(acao("confirmar"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Não é possível confirmar um agendamento CANCELADO"));
	}

	// Cancelar. Os erros comuns a todas as ações (403, 404, 422) já estão cobertos na confirmação.

	@Test
	void cancelaComMotivo() throws Exception {
		given(cancelar.executar(new CancelarAgendamentoUseCase.Comando(agendamentoId, GESTOR_ID, "Salão fechado")))
				.willReturn(agendamentoCom(StatusAgendamento.CANCELADO));

		mvc.perform(acao("cancelar").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "motivo": "Salão fechado" }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADO"));
	}

	@Test
	void cancelaSemCorpo() throws Exception {
		given(cancelar.executar(new CancelarAgendamentoUseCase.Comando(agendamentoId, GESTOR_ID, null)))
				.willReturn(agendamentoCom(StatusAgendamento.CANCELADO));

		mvc.perform(acao("cancelar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADO"));
	}

	@Test
	void recusaMotivoDeCancelamentoLongo() throws Exception {
		mvc.perform(acao("cancelar").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "motivo": "%s" }
						""".formatted("x".repeat(251))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("motivo"));

		verifyNoInteractions(cancelar);
	}

}
