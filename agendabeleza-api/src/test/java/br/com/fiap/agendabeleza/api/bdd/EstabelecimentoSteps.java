package br.com.fiap.agendabeleza.api.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.restassured.response.Response;

/**
 * Passos da feature de cadastro de estabelecimento. A instância vale por cenário, então o estado abaixo não
 * passa de um cenário para outro. Cada cenário usa um gestor novo, sem limpar o banco.
 */
public class EstabelecimentoSteps extends ApiDaAplicacao {

	private UUID gestorId;
	private UUID estabelecimentoId;
	private Response resposta;

	@Before
	public void prepararCliente() {
		gestorId = UUID.randomUUID();
	}

	private static String estabelecimentoJson(Map<String, String> campos, String horariosJson) {
		return """
				{
				  "nome": "%s",
				  "endereco": {
				    "logradouro": "Rua Augusta", "numero": "100", "bairro": "%s",
				    "cidade": "%s", "uf": "%s", "cep": "%s"
				  },
				  "horarios": %s
				}
				""".formatted(campos.get("nome"), campos.get("bairro"), campos.get("cidade"), campos.get("uf"),
				campos.get("cep"), horariosJson);
	}

	private static Map<String, String> camposValidos(String nome) {
		Map<String, String> campos = new HashMap<>();
		campos.put("nome", nome);
		campos.put("bairro", "Consolação");
		campos.put("cidade", "São Paulo");
		campos.put("uf", "SP");
		campos.put("cep", "01305-000");
		return campos;
	}

	private Response cadastrar(Map<String, String> campos, String horariosJson) {
		return api(gestorId).body(estabelecimentoJson(campos, horariosJson)).post("/estabelecimentos");
	}

	private Response consultaPublica() {
		return api(null).get("/estabelecimentos/{id}", estabelecimentoId);
	}

	@Dado("que sou um gestor identificado")
	public void queSouUmGestorIdentificado() {
		assertThat(gestorId).isNotNull();
	}

	@Dado("que cadastrei o estabelecimento {string}")
	public void queCadastreiOEstabelecimento(String nome) {
		resposta = cadastrar(camposValidos(nome), "[]");
		assertThat(resposta.statusCode()).isEqualTo(201);
		estabelecimentoId = UUID.fromString(resposta.jsonPath().getString("id"));
	}

	@Quando("cadastro o estabelecimento {string} em {string}, bairro {string}, com os horários:")
	public void cadastroOEstabelecimento(String nome, String cidade, String bairro, DataTable horarios) {
		Map<String, String> campos = camposValidos(nome);
		campos.put("cidade", cidade);
		campos.put("bairro", bairro);
		String horariosJson = horarios.asMaps().stream()
				.map(linha -> "{ \"diaSemana\": \"%s\", \"abreAs\": \"%s\", \"fechaAs\": \"%s\" }"
						.formatted(linha.get("dia"), linha.get("abre"), linha.get("fecha")))
				.collect(Collectors.joining(", ", "[", "]"));
		resposta = cadastrar(campos, horariosJson);
		if (resposta.statusCode() == 201) {
			estabelecimentoId = UUID.fromString(resposta.jsonPath().getString("id"));
		}
	}

	@Quando("tento cadastrar um estabelecimento com {word} igual a {string}")
	public void tentoCadastrarComCampoIgualA(String campo, String valor) {
		Map<String, String> campos = camposValidos("Salão Bela Vista");
		assertThat(campos).containsKey(campo);
		campos.put(campo, valor);
		resposta = cadastrar(campos, "[]");
	}

	@Quando("cadastro o serviço {string} da categoria {string} com {int} minutos por {string} reais")
	public void cadastroOServico(String nome, String categoria, int duracao, String preco) {
		resposta = api(gestorId).body("""
				{ "nome": "%s", "categoria": "%s", "duracaoMinutos": %d, "precoBase": %s }
				""".formatted(nome, categoria, duracao, preco))
				.post("/estabelecimentos/{id}/servicos", estabelecimentoId);
	}

	@Quando("adiciono a foto {string}")
	public void adicionoAFoto(String url) {
		resposta = api(gestorId).body("{ \"url\": \"%s\" }".formatted(url))
				.post("/estabelecimentos/{id}/fotos", estabelecimentoId);
	}

	@Quando("outro usuário tenta alterar o nome para {string}")
	public void outroUsuarioTentaAlterarONome(String nome) {
		String dados = estabelecimentoJson(camposValidos(nome), "[]").replace(",\n  \"horarios\": []", "");
		resposta = api(UUID.randomUUID()).body(dados).put("/estabelecimentos/{id}", estabelecimentoId);
	}

	@Entao("a resposta tem status {int}")
	public void aRespostaTemStatus(int status) {
		assertThat(resposta.statusCode()).as(resposta.asString()).isEqualTo(status);
	}

	@E("a resposta informa o endereço do novo estabelecimento")
	public void aRespostaInformaOEnderecoDoNovoEstabelecimento() {
		assertThat(resposta.header("Location")).endsWith("/api/v1/estabelecimentos/" + estabelecimentoId);
	}

	@E("a consulta pública mostra o nome {string}, {int} dias de funcionamento e o fuso {string}")
	public void aConsultaPublicaMostra(String nome, int dias, String fuso) {
		Response consulta = consultaPublica();
		assertThat(consulta.statusCode()).isEqualTo(200);
		assertThat(consulta.jsonPath().getString("nome")).isEqualTo(nome);
		assertThat(consulta.jsonPath().getList("horarios")).hasSize(dias);
		assertThat(consulta.jsonPath().getString("timezone")).isEqualTo(fuso);
		assertThat(consulta.jsonPath().getString("endereco.cep")).isEqualTo("01305000");
	}

	@E("a consulta pública continua mostrando o nome {string}")
	public void aConsultaPublicaContinuaMostrandoONome(String nome) {
		assertThat(consultaPublica().jsonPath().getString("nome")).isEqualTo(nome);
	}

	@E("a consulta pública mostra {int} foto(s)")
	public void aConsultaPublicaMostraFotos(int quantidade) {
		assertThat(consultaPublica().jsonPath().getList("fotos")).hasSize(quantidade);
	}

	@E("a lista pública de serviços contém {string} por {string} reais")
	public void aListaPublicaDeServicosContem(String nome, String preco) {
		Response lista = api(null).get("/estabelecimentos/{id}/servicos", estabelecimentoId);
		assertThat(lista.statusCode()).isEqualTo(200);
		List<Map<String, Object>> servicos = lista.jsonPath().getList("$");
		assertThat(servicos)
				.filteredOn(servico -> nome.equals(servico.get("nome")))
				.singleElement()
				.satisfies(servico -> assertThat(new BigDecimal(servico.get("precoBase").toString()))
						.isEqualByComparingTo(preco));
	}

}
