package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web;

import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.GESTOR_ID;
import static br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture.estabelecimento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalTime;
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
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.AdicionarFotoEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.AtualizarEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.CadastrarEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.ConsultarEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.DefinirHorariosFuncionamentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.RemoverFotoEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.FotoEstabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;

@WebMvcTest(EstabelecimentoController.class)
class EstabelecimentoControllerTest {

	private static final String URL = "/api/v1/estabelecimentos";
	private static final String USUARIO = "X-Usuario-Id";

	private static final String CORPO_ESTABELECIMENTO = """
			{
			  "nome": "Salão Bela Vista",
			  "descricao": "Cabelo e unhas",
			  "endereco": {
			    "logradouro": "Rua Augusta", "numero": "100", "bairro": "Consolação",
			    "cidade": "São Paulo", "uf": "SP", "cep": "01305000"
			  },
			  "horarios": [ { "diaSemana": "MONDAY", "abreAs": "09:00", "fechaAs": "18:00" } ],
			  "fotos": [ "https://cdn.exemplo.com/1.jpg" ]
			}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CadastrarEstabelecimentoUseCase cadastrar;
	@MockitoBean
	private ConsultarEstabelecimentoUseCase consultar;
	@MockitoBean
	private AtualizarEstabelecimentoUseCase atualizar;
	@MockitoBean
	private DefinirHorariosFuncionamentoUseCase definirHorarios;
	@MockitoBean
	private AdicionarFotoEstabelecimentoUseCase adicionarFoto;
	@MockitoBean
	private RemoverFotoEstabelecimentoUseCase removerFoto;

	private final HorarioFuncionamento segunda = new HorarioFuncionamento(
			DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

	@Test
	void cadastraEstabelecimento() throws Exception {
		Estabelecimento criado = estabelecimento();
		criado.definirHorarios(List.of(segunda));
		criado.adicionarFoto("https://cdn.exemplo.com/1.jpg");
		given(cadastrar.executar(any())).willReturn(criado);

		mvc.perform(post(URL).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content(CORPO_ESTABELECIMENTO))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith(URL + "/" + criado.id())))
				.andExpect(jsonPath("$.id").value(criado.id().toString()))
				.andExpect(jsonPath("$.gestorId").value(GESTOR_ID.toString()))
				.andExpect(jsonPath("$.nome").value("Salão Bela Vista"))
				.andExpect(jsonPath("$.endereco.uf").value("SP"))
				.andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
				.andExpect(jsonPath("$.ativo").value(true))
				.andExpect(jsonPath("$.horarios[0].diaSemana").value("MONDAY"))
				.andExpect(jsonPath("$.horarios[0].abreAs").value("09:00"))
				.andExpect(jsonPath("$.horarios[0].fechaAs").value("18:00"))
				.andExpect(jsonPath("$.fotos[0].url").value("https://cdn.exemplo.com/1.jpg"))
				.andExpect(jsonPath("$.fotos[0].ordem").value(1));

		ArgumentCaptor<CadastrarEstabelecimentoUseCase.Comando> comando =
				ArgumentCaptor.forClass(CadastrarEstabelecimentoUseCase.Comando.class);
		verify(cadastrar).executar(comando.capture());
		assertThat(comando.getValue().gestorId()).isEqualTo(GESTOR_ID);
		assertThat(comando.getValue().nome()).isEqualTo("Salão Bela Vista");
		assertThat(comando.getValue().endereco().cep()).isEqualTo("01305000");
		assertThat(comando.getValue().horarios()).containsExactly(segunda);
		assertThat(comando.getValue().fotos()).containsExactly("https://cdn.exemplo.com/1.jpg");
	}

	@Test
	void recusaCadastroSemIdentificacaoDoUsuario() throws Exception {
		mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CORPO_ESTABELECIMENTO))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void recusaCadastroComCamposInvalidos() throws Exception {
		mvc.perform(post(URL).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": " ", "endereco": { "logradouro": "Rua", "numero": "1", "bairro": "Centro",
						  "cidade": "São Paulo", "uf": "SP", "cep": "01305000" } }
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.erros[0].campo").value("nome"));
	}

	@Test
	void consultaEstabelecimento() throws Exception {
		Estabelecimento existente = estabelecimento();
		given(consultar.executar(existente.id())).willReturn(existente);

		mvc.perform(get(URL + "/" + existente.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Salão Bela Vista"));
	}

	@Test
	void consultaDeEstabelecimentoInexistenteDevolve404() throws Exception {
		UUID id = UUID.randomUUID();
		given(consultar.executar(id)).willThrow(new RecursoNaoEncontradoException("Estabelecimento " + id + " não encontrado"));

		mvc.perform(get(URL + "/" + id))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Estabelecimento " + id + " não encontrado"));
	}

	@Test
	void atualizaEstabelecimento() throws Exception {
		Estabelecimento existente = estabelecimento();
		given(atualizar.executar(any())).willReturn(existente);

		mvc.perform(put(URL + "/" + existente.id()).header(USUARIO, GESTOR_ID).contentType(MediaType.APPLICATION_JSON)
				.content(CORPO_ESTABELECIMENTO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(existente.id().toString()));

		ArgumentCaptor<AtualizarEstabelecimentoUseCase.Comando> comando =
				ArgumentCaptor.forClass(AtualizarEstabelecimentoUseCase.Comando.class);
		verify(atualizar).executar(comando.capture());
		assertThat(comando.getValue().estabelecimentoId()).isEqualTo(existente.id());
		assertThat(comando.getValue().usuarioId()).isEqualTo(GESTOR_ID);
	}

	@Test
	void atualizacaoPorOutroUsuarioDevolve403() throws Exception {
		given(atualizar.executar(any())).willThrow(new AcessoNegadoException("Somente o gestor do estabelecimento pode alterá-lo"));

		mvc.perform(put(URL + "/" + UUID.randomUUID()).header(USUARIO, UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON).content(CORPO_ESTABELECIMENTO))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Somente o gestor do estabelecimento pode alterá-lo"));
	}

	@Test
	void defineHorarios() throws Exception {
		Estabelecimento existente = estabelecimento();
		existente.definirHorarios(List.of(segunda));
		given(definirHorarios.executar(any())).willReturn(existente);

		mvc.perform(put(URL + "/" + existente.id() + "/horarios").header(USUARIO, GESTOR_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "horarios": [ { "diaSemana": "MONDAY", "abreAs": "09:00", "fechaAs": "18:00" } ] }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].diaSemana").value("MONDAY"))
				.andExpect(jsonPath("$[0].fechaAs").value("18:00"));

		ArgumentCaptor<DefinirHorariosFuncionamentoUseCase.Comando> comando =
				ArgumentCaptor.forClass(DefinirHorariosFuncionamentoUseCase.Comando.class);
		verify(definirHorarios).executar(comando.capture());
		assertThat(comando.getValue().horarios()).containsExactly(segunda);
	}

	@Test
	void adicionaFoto() throws Exception {
		UUID estabelecimentoId = UUID.randomUUID();
		FotoEstabelecimento foto = new FotoEstabelecimento(UUID.randomUUID(), "https://cdn.exemplo.com/1.jpg", 1);
		given(adicionarFoto.executar(new AdicionarFotoEstabelecimentoUseCase.Comando(
				estabelecimentoId, GESTOR_ID, "https://cdn.exemplo.com/1.jpg"))).willReturn(foto);

		mvc.perform(post(URL + "/" + estabelecimentoId + "/fotos").header(USUARIO, GESTOR_ID)
				.contentType(MediaType.APPLICATION_JSON).content("{ \"url\": \"https://cdn.exemplo.com/1.jpg\" }"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(foto.id().toString()))
				.andExpect(jsonPath("$.ordem").value(1));
	}

	@Test
	void fotoInvalidaDevolve422() throws Exception {
		given(adicionarFoto.executar(any()))
				.willThrow(new RegraNegocioException("url da foto deve começar com http:// ou https://"));

		mvc.perform(post(URL + "/" + UUID.randomUUID() + "/fotos").header(USUARIO, GESTOR_ID)
				.contentType(MediaType.APPLICATION_JSON).content("{ \"url\": \"ftp://cdn.exemplo.com/1.jpg\" }"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("url da foto deve começar com http:// ou https://"));
	}

	@Test
	void removeFoto() throws Exception {
		UUID estabelecimentoId = UUID.randomUUID();
		UUID fotoId = UUID.randomUUID();

		mvc.perform(delete(URL + "/" + estabelecimentoId + "/fotos/" + fotoId).header(USUARIO, GESTOR_ID))
				.andExpect(status().isNoContent());

		verify(removerFoto).executar(new RemoverFotoEstabelecimentoUseCase.Comando(estabelecimentoId, GESTOR_ID, fotoId));
	}

	@Test
	void idMalformadoDevolve400() throws Exception {
		mvc.perform(get(URL + "/nao-e-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

}
