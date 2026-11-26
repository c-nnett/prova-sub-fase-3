package br.com.fiap.agendabeleza.api.avaliacoes.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import br.com.fiap.agendabeleza.api.avaliacoes.application.usecase.AvaliarAtendimentoUseCase;
import br.com.fiap.agendabeleza.api.avaliacoes.application.usecase.ListarAvaliacoesUseCase;
import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

@WebMvcTest(AvaliacaoController.class)
class AvaliacaoControllerTest {

	private static final String USUARIO = "X-Usuario-Id";
	private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID AGENDAMENTO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
	private static final UUID ESTABELECIMENTO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
	private static final Instant CRIADA_EM = Instant.parse("2030-03-04T16:00:00Z");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private AvaliarAtendimentoUseCase avaliar;
	@MockitoBean
	private ListarAvaliacoesUseCase listar;

	private static Avaliacao avaliacao(String comentario) {
		return Avaliacao.reconstituir(UUID.randomUUID(), AGENDAMENTO_ID, CLIENTE_ID, ESTABELECIMENTO_ID,
				PROFISSIONAL_ID, 5, 4, comentario, CRIADA_EM);
	}

	private static MockHttpServletRequestBuilder avaliarCom(String corpo) {
		return post("/api/v1/agendamentos/" + AGENDAMENTO_ID + "/avaliacao").header(USUARIO, CLIENTE_ID)
				.contentType(MediaType.APPLICATION_JSON).content(corpo);
	}

	private static String corpoComNotas(int notaEstabelecimento, int notaProfissional) {
		return """
				{ "notaEstabelecimento": %d, "notaProfissional": %d }
				""".formatted(notaEstabelecimento, notaProfissional);
	}

	// Avaliar

	@Test
	void avaliaEDevolve201ComALista() throws Exception {
		Avaliacao avaliada = avaliacao("Excelente");
		given(avaliar.executar(new AvaliarAtendimentoUseCase.Comando(AGENDAMENTO_ID, CLIENTE_ID, 5, 4,
				"Excelente"))).willReturn(avaliada);

		mvc.perform(avaliarCom("""
				{ "notaEstabelecimento": 5, "notaProfissional": 4, "comentario": "Excelente" }
				"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						"http://localhost/api/v1/estabelecimentos/" + ESTABELECIMENTO_ID + "/avaliacoes"))
				.andExpect(jsonPath("$.id").value(avaliada.id().toString()))
				.andExpect(jsonPath("$.agendamentoId").value(AGENDAMENTO_ID.toString()))
				.andExpect(jsonPath("$.notaEstabelecimento").value(5))
				.andExpect(jsonPath("$.notaProfissional").value(4))
				.andExpect(jsonPath("$.comentario").value("Excelente"))
				.andExpect(jsonPath("$.criadaEm").value("2030-03-04T16:00:00Z"))
				.andExpect(jsonPath("$.clienteId").doesNotExist());
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 6 })
	void notaForaDaEscalaDevolve400(int nota) throws Exception {
		mvc.perform(avaliarCom(corpoComNotas(nota, 3)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("notaEstabelecimento"));

		verifyNoInteractions(avaliar);
	}

	@Test
	void semAsNotasDevolve400ComOsDoisCampos() throws Exception {
		mvc.perform(avaliarCom("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("notaEstabelecimento"))
				.andExpect(jsonPath("$.erros[1].campo").value("notaProfissional"));

		verifyNoInteractions(avaliar);
	}

	@Test
	void comentarioLongoDemaisDevolve400() throws Exception {
		mvc.perform(avaliarCom("""
				{ "notaEstabelecimento": 5, "notaProfissional": 5, "comentario": "%s" }
				""".formatted("a".repeat(1001))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("comentario"));
	}

	@Test
	void semCabecalhoDoUsuarioDevolve400() throws Exception {
		mvc.perform(post("/api/v1/agendamentos/" + AGENDAMENTO_ID + "/avaliacao")
				.contentType(MediaType.APPLICATION_JSON).content(corpoComNotas(5, 5)))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(avaliar);
	}

	@Test
	void quemNaoEOClienteRecebe403() throws Exception {
		given(avaliar.executar(any()))
				.willThrow(new AcessoNegadoException(AvaliarAtendimentoUseCase.SOMENTE_O_CLIENTE));

		mvc.perform(avaliarCom(corpoComNotas(5, 5)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value(AvaliarAtendimentoUseCase.SOMENTE_O_CLIENTE));
	}

	@Test
	void agendamentoInexistenteDevolve404() throws Exception {
		given(avaliar.executar(any())).willThrow(new RecursoNaoEncontradoException("inexistente"));

		mvc.perform(avaliarCom(corpoComNotas(5, 5)))
				.andExpect(status().isNotFound());
	}

	@Test
	void atendimentoJaAvaliadoDevolve422() throws Exception {
		given(avaliar.executar(any())).willThrow(new RegraNegocioException(AvaliarAtendimentoUseCase.JA_AVALIADO));

		mvc.perform(avaliarCom(corpoComNotas(5, 5)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(AvaliarAtendimentoUseCase.JA_AVALIADO));
	}

	@Test
	void atendimentoNaoConcluidoDevolve422() throws Exception {
		given(avaliar.executar(any())).willThrow(new RegraNegocioException(Avaliacao.AGENDAMENTO_NAO_CONCLUIDO));

		mvc.perform(avaliarCom(corpoComNotas(5, 5)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(Avaliacao.AGENDAMENTO_NAO_CONCLUIDO));
	}

	// Listar

	@Test
	void listaAsAvaliacoesDoEstabelecimentoSemOCliente() throws Exception {
		given(listar.doEstabelecimento(ESTABELECIMENTO_ID)).willReturn(List.of(avaliacao("Ótimo"), avaliacao(null)));

		mvc.perform(get("/api/v1/estabelecimentos/" + ESTABELECIMENTO_ID + "/avaliacoes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].comentario").value("Ótimo"))
				.andExpect(jsonPath("$[0].clienteId").doesNotExist());
	}

	@Test
	void listaAsAvaliacoesDoProfissional() throws Exception {
		given(listar.doProfissional(PROFISSIONAL_ID)).willReturn(List.of(avaliacao("Caprichosa")));

		mvc.perform(get("/api/v1/profissionais/" + PROFISSIONAL_ID + "/avaliacoes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].profissionalId").value(PROFISSIONAL_ID.toString()))
				.andExpect(jsonPath("$[0].notaProfissional").value(4));
	}

	@Test
	void listarDeInexistenteDevolve404() throws Exception {
		given(listar.doProfissional(PROFISSIONAL_ID)).willThrow(new RecursoNaoEncontradoException("inexistente"));

		mvc.perform(get("/api/v1/profissionais/" + PROFISSIONAL_ID + "/avaliacoes"))
				.andExpect(status().isNotFound());
	}

}
