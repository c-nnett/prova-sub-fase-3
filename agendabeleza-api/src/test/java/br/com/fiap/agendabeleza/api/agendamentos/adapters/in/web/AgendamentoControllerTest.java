package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CriarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ListarMeusAgendamentosUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

@WebMvcTest(AgendamentoController.class)
class AgendamentoControllerTest {

	private static final String USUARIO = "X-Usuario-Id";
	private static final String URL = "/api/v1/agendamentos";
	private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
	private static final Instant INICIO = Instant.parse("2030-03-04T13:00:00Z");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CriarAgendamentoUseCase criar;
	@MockitoBean
	private ListarMeusAgendamentosUseCase listarMeus;
	@MockitoBean
	private ConsultarAgendamentoUseCase consultar;

	private final Agendamento agendamento = Agendamento.reconstituir(UUID.randomUUID(), CLIENTE_ID,
			UUID.randomUUID(), PROFISSIONAL_ID, SERVICO_ID, INICIO, INICIO.plusSeconds(3600),
			StatusAgendamento.AGENDADO, new BigDecimal("95.00"),
			"Primeira vez", 0L);

	private static String corpo(Object profissionalId, Object servicoId, Object inicio) {
		return """
				{ "profissionalId": %s, "servicoId": %s, "inicio": %s, "observacoes": "Primeira vez" }
				""".formatted(json(profissionalId), json(servicoId), json(inicio));
	}

	private static String json(Object valor) {
		return valor == null ? "null" : "\"" + valor + "\"";
	}

	@Test
	void criaAgendamentoEDevolve201ComLocation() throws Exception {
		given(criar.executar(any())).willReturn(agendamento);

		mvc.perform(post(URL).header(USUARIO, CLIENTE_ID).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(PROFISSIONAL_ID, SERVICO_ID, "2030-03-04T10:00:00-03:00")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith(URL + "/" + agendamento.id())))
				.andExpect(jsonPath("$.id").value(agendamento.id().toString()))
				.andExpect(jsonPath("$.estabelecimentoId").value(agendamento.estabelecimentoId().toString()))
				.andExpect(jsonPath("$.profissionalId").value(PROFISSIONAL_ID.toString()))
				.andExpect(jsonPath("$.servicoId").value(SERVICO_ID.toString()))
				.andExpect(jsonPath("$.inicio").value("2030-03-04T13:00:00Z"))
				.andExpect(jsonPath("$.fim").value("2030-03-04T14:00:00Z"))
				.andExpect(jsonPath("$.duracaoMinutos").value(60))
				.andExpect(jsonPath("$.status").value("AGENDADO"))
				.andExpect(jsonPath("$.preco").value(95.00))
				.andExpect(jsonPath("$.observacoes").value("Primeira vez"));

		ArgumentCaptor<CriarAgendamentoUseCase.Comando> comando =
				ArgumentCaptor.forClass(CriarAgendamentoUseCase.Comando.class);
		verify(criar).executar(comando.capture());
		assertThat(comando.getValue()).isEqualTo(new CriarAgendamentoUseCase.Comando(CLIENTE_ID, PROFISSIONAL_ID,
				SERVICO_ID, INICIO, "Primeira vez"));
	}

	@Test
	void recusaCriacaoSemCabecalhoDoUsuario() throws Exception {
		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(PROFISSIONAL_ID, SERVICO_ID, INICIO)))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(criar);
	}

	@Test
	void recusaCorpoSemCamposObrigatorios() throws Exception {
		mvc.perform(post(URL).header(USUARIO, CLIENTE_ID).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(null, null, null)))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.erros.length()").value(3));

		verifyNoInteractions(criar);
	}

	@Test
	void recusaObservacoesLongas() throws Exception {
		mvc.perform(post(URL).header(USUARIO, CLIENTE_ID).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "profissionalId": "%s", "servicoId": "%s", "inicio": "%s", "observacoes": "%s" }
						""".formatted(PROFISSIONAL_ID, SERVICO_ID, INICIO, "x".repeat(501))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void horarioOcupadoDevolve409() throws Exception {
		given(criar.executar(any())).willThrow(new HorarioIndisponivelException());

		mvc.perform(post(URL).header(USUARIO, CLIENTE_ID).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(PROFISSIONAL_ID, SERVICO_ID, INICIO)))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Conflito"))
				.andExpect(jsonPath("$.detail").value("O profissional já tem um agendamento nesse horário"));
	}

	@Test
	void versaoDesatualizadaDevolve409() throws Exception {
		given(consultar.executar(agendamento.id(), CLIENTE_ID)).willThrow(
				new ObjectOptimisticLockingFailureException(Agendamento.class, agendamento.id()));

		mvc.perform(get(URL + "/" + agendamento.id()).header(USUARIO, CLIENTE_ID))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Conflito"))
				.andExpect(jsonPath("$.detail").value(
						"O registro foi alterado por outra operação. Consulte-o de novo e repita a operação"));
	}

	@Test
	void regraVioladaDevolve422() throws Exception {
		given(criar.executar(any())).willThrow(new RegraNegocioException("inicio deve estar no futuro"));

		mvc.perform(post(URL).header(USUARIO, CLIENTE_ID).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(PROFISSIONAL_ID, SERVICO_ID, INICIO)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("inicio deve estar no futuro"));
	}

	@Test
	void profissionalInexistenteDevolve404() throws Exception {
		given(criar.executar(any())).willThrow(new RecursoNaoEncontradoException("Profissional não encontrado"));

		mvc.perform(post(URL).header(USUARIO, CLIENTE_ID).contentType(MediaType.APPLICATION_JSON)
				.content(corpo(PROFISSIONAL_ID, SERVICO_ID, INICIO)))
				.andExpect(status().isNotFound());
	}

	@Test
	void listaOsAgendamentosDoUsuario() throws Exception {
		given(listarMeus.executar(CLIENTE_ID)).willReturn(List.of(agendamento));

		mvc.perform(get(URL + "/meus").header(USUARIO, CLIENTE_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(agendamento.id().toString()));
	}

	@Test
	void consultaOAgendamento() throws Exception {
		given(consultar.executar(agendamento.id(), CLIENTE_ID)).willReturn(agendamento);

		mvc.perform(get(URL + "/" + agendamento.id()).header(USUARIO, CLIENTE_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(agendamento.id().toString()));
	}

	@Test
	void consultaPorTerceiroDevolve403() throws Exception {
		UUID terceiro = UUID.randomUUID();
		given(consultar.executar(agendamento.id(), terceiro)).willThrow(new AcessoNegadoException("negado"));

		mvc.perform(get(URL + "/" + agendamento.id()).header(USUARIO, terceiro))
				.andExpect(status().isForbidden());
	}

}
