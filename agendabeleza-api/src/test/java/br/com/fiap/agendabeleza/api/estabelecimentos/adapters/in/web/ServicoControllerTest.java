package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.AtualizarServicoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.CadastrarServicoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.ListarServicosUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

@WebMvcTest(ServicoController.class)
class ServicoControllerTest {

	private static final String USUARIO = "X-Usuario-Id";
	private static final UUID ESTABELECIMENTO_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	private static final String URL = "/api/v1/estabelecimentos/" + ESTABELECIMENTO_ID + "/servicos";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CadastrarServicoUseCase cadastrar;
	@MockitoBean
	private ListarServicosUseCase listar;
	@MockitoBean
	private AtualizarServicoUseCase atualizar;

	private Servico corte() {
		return Servico.cadastrar(ESTABELECIMENTO_ID, "Corte", "Lavagem inclusa",
				CategoriaServico.CABELO, 60, new BigDecimal("80.00"));
	}

	@Test
	void cadastraServico() throws Exception {
		Servico criado = corte();
		given(cadastrar.executar(any())).willReturn(criado);

		mvc.perform(post(URL).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Corte", "descricao": "Lavagem inclusa", "categoria": "CABELO",
						  "duracaoMinutos": 60, "precoBase": 80.00 }
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith(URL + "/" + criado.id())))
				.andExpect(jsonPath("$.id").value(criado.id().toString()))
				.andExpect(jsonPath("$.estabelecimentoId").value(ESTABELECIMENTO_ID.toString()))
				.andExpect(jsonPath("$.categoria").value("CABELO"))
				.andExpect(jsonPath("$.duracaoMinutos").value(60))
				.andExpect(jsonPath("$.precoBase").value(80.0))
				.andExpect(jsonPath("$.ativo").value(true));

		ArgumentCaptor<CadastrarServicoUseCase.Comando> comando =
				ArgumentCaptor.forClass(CadastrarServicoUseCase.Comando.class);
		verify(cadastrar).executar(comando.capture());
		assertThat(comando.getValue().estabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
		assertThat(comando.getValue().usuarioId()).isEqualTo(GESTOR_ID);
		assertThat(comando.getValue().precoBase()).isEqualByComparingTo("80");
	}

	@Test
	void recusaServicoSemCategoria() throws Exception {
		mvc.perform(post(URL).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"nome\": \"Corte\", \"duracaoMinutos\": 60, \"precoBase\": 80 }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("categoria"));
	}

	@Test
	void listaServicos() throws Exception {
		Servico servico = corte();
		given(listar.executar(ESTABELECIMENTO_ID)).willReturn(List.of(servico));

		mvc.perform(get(URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(servico.id().toString()))
				.andExpect(jsonPath("$[0].nome").value("Corte"));
	}

	@Test
	void atualizaServico() throws Exception {
		Servico servico = corte();
		servico.atualizar("Corte e escova", null, CategoriaServico.CABELO, 90, new BigDecimal("120.50"), false);
		given(atualizar.executar(any())).willReturn(servico);

		mvc.perform(put(URL + "/" + servico.id()).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Corte e escova", "categoria": "CABELO", "duracaoMinutos": 90,
						  "precoBase": 120.50, "ativo": false }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Corte e escova"))
				.andExpect(jsonPath("$.ativo").value(false));

		ArgumentCaptor<AtualizarServicoUseCase.Comando> comando =
				ArgumentCaptor.forClass(AtualizarServicoUseCase.Comando.class);
		verify(atualizar).executar(comando.capture());
		assertThat(comando.getValue().servicoId()).isEqualTo(servico.id());
		assertThat(comando.getValue().estabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
		assertThat(comando.getValue().ativo()).isFalse();
	}

	@Test
	void servicoDeOutroEstabelecimentoDevolve404() throws Exception {
		UUID servicoId = UUID.randomUUID();
		given(atualizar.executar(any())).willThrow(new RecursoNaoEncontradoException("Serviço " + servicoId + " não encontrado"));

		mvc.perform(put(URL + "/" + servicoId).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Corte", "categoria": "CABELO", "duracaoMinutos": 30, "precoBase": 10, "ativo": true }
						"""))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

}
