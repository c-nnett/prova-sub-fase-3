package br.com.fiap.agendabeleza.api.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.restassured.response.Response;

/**
 * Passos da feature de avaliação. O cenário inteiro passa pela API: cadastro do salão e da profissional, reserva,
 * confirmação, conclusão e avaliação. Para concluir, o {@link RelogioAjustavel} leva o "agora" para depois do início
 * do atendimento, que é reservado no futuro.
 */
public class AvaliacaoSteps extends ApiDaAplicacao {

	/** Segunda-feira, 10h no horário de Brasília. */
	private static final String INICIO_DO_ATENDIMENTO = "2030-03-04T10:00:00-03:00";
	/** Mesma segunda, 12h no horário de Brasília: o atendimento de uma hora já terminou. */
	private static final Instant DEPOIS_DO_ATENDIMENTO = Instant.parse("2030-03-04T15:00:00Z");

	@Autowired
	private RelogioAjustavel relogio;

	private UUID gestorId;
	private UUID clienteId;
	private UUID estabelecimentoId;
	private UUID servicoId;
	private UUID profissionalId;
	private UUID agendamentoId;
	private Response resposta;

	@Before
	public void prepararUsuarios() {
		gestorId = UUID.randomUUID();
		clienteId = UUID.randomUUID();
	}

	@After
	public void voltarAoRelogioReal() {
		relogio.voltarAoRelogioReal();
	}

	private static UUID idCriado(Response resposta) {
		exigirSucesso(resposta);
		return UUID.fromString(resposta.jsonPath().getString("id"));
	}

	private static void exigirSucesso(Response resposta) {
		assertThat(resposta.statusCode()).as(resposta.asString()).isBetween(200, 299);
	}

	private Response avaliar(int notaSalao, int notaProfissional, String comentario) {
		return api(clienteId).body("""
				{ "notaEstabelecimento": %d, "notaProfissional": %d, "comentario": "%s" }
				""".formatted(notaSalao, notaProfissional, comentario))
				.post("/agendamentos/{id}/avaliacao", agendamentoId);
	}

	private static void exigirNotaMedia(Response consulta, String notaMedia, int totalAvaliacoes) {
		assertThat(consulta.statusCode()).isEqualTo(200);
		assertThat(new BigDecimal(consulta.jsonPath().getString("notaMedia"))).isEqualByComparingTo(notaMedia);
		assertThat(consulta.jsonPath().getInt("totalAvaliacoes")).isEqualTo(totalAvaliacoes);
	}

	@Dado("um salão com a profissional {string}, que atende às segundas das {word} às {word}")
	public void umSalaoComAProfissional(String nome, String inicio, String fim) {
		estabelecimentoId = idCriado(api(gestorId).body("""
				{
				  "nome": "Salão Bela Vista",
				  "endereco": { "logradouro": "Rua Augusta", "numero": "100", "bairro": "Consolação",
				                "cidade": "São Paulo", "uf": "SP", "cep": "01305-000" },
				  "horarios": [ { "diaSemana": "MONDAY", "abreAs": "08:00", "fechaAs": "20:00" } ]
				}
				""").post("/estabelecimentos"));
		servicoId = idCriado(api(gestorId).body("""
				{ "nome": "Corte", "categoria": "CABELO", "duracaoMinutos": 60, "precoBase": 80.00 }
				""").post("/estabelecimentos/{id}/servicos", estabelecimentoId));
		profissionalId = idCriado(api(gestorId).body("""
				{ "nome": "%s", "email": "%s@salao.com" }
				""".formatted(nome, nome.toLowerCase())).post("/estabelecimentos/{id}/profissionais", estabelecimentoId));
		exigirSucesso(api(gestorId).body("""
				{ "servicos": [ { "servicoId": "%s" } ] }
				""".formatted(servicoId)).put("/profissionais/{id}/servicos", profissionalId));
		exigirSucesso(api(gestorId).body("""
				{ "periodos": [ { "diaSemana": "MONDAY", "inicio": "%s", "fim": "%s" } ] }
				""".formatted(inicio, fim)).put("/profissionais/{id}/disponibilidades", profissionalId));
	}

	@Dado("que fui atendido por ela e o gestor concluiu o atendimento")
	public void queFuiAtendidoEOGestorConcluiu() {
		agendamentoId = idCriado(api(clienteId).body("""
				{ "profissionalId": "%s", "servicoId": "%s", "inicio": "%s", "emailCliente": "cliente@email.com" }
				""".formatted(profissionalId, servicoId, INICIO_DO_ATENDIMENTO)).post("/agendamentos"));
		exigirSucesso(api(gestorId).post("/agendamentos/{id}/confirmar", agendamentoId));
		relogio.definirAgora(DEPOIS_DO_ATENDIMENTO);
		exigirSucesso(api(gestorId).post("/agendamentos/{id}/concluir", agendamentoId));
	}

	@Dado("que já avaliei o atendimento com nota {int} para o salão e {int} para a profissional")
	public void queJaAvalieiOAtendimento(int notaSalao, int notaProfissional) {
		exigirSucesso(avaliar(notaSalao, notaProfissional, "Primeira avaliação"));
	}

	@Quando("avalio o atendimento com nota {int} para o salão, {int} para a profissional e o comentário {string}")
	public void avalioOAtendimento(int notaSalao, int notaProfissional, String comentario) {
		resposta = avaliar(notaSalao, notaProfissional, comentario);
	}

	@Entao("a avaliação é registrada")
	public void aAvaliacaoERegistrada() {
		assertThat(resposta.statusCode()).as(resposta.asString()).isEqualTo(201);
		assertThat(resposta.jsonPath().getString("agendamentoId")).isEqualTo(agendamentoId.toString());
	}

	@Entao("a avaliação é recusada com status {int} e a mensagem {string}")
	public void aAvaliacaoERecusada(int status, String mensagem) {
		assertThat(resposta.statusCode()).as(resposta.asString()).isEqualTo(status);
		assertThat(resposta.jsonPath().getString("detail")).isEqualTo(mensagem);
	}

	@E("o salão tem nota média {string} em {int} avaliação")
	public void oSalaoTemNotaMedia(String notaMedia, int totalAvaliacoes) {
		exigirNotaMedia(api(null).get("/estabelecimentos/{id}", estabelecimentoId), notaMedia, totalAvaliacoes);
	}

	@E("a profissional tem nota média {string} em {int} avaliação")
	public void aProfissionalTemNotaMedia(String notaMedia, int totalAvaliacoes) {
		exigirNotaMedia(api(null).get("/profissionais/{id}", profissionalId), notaMedia, totalAvaliacoes);
	}

	@E("a lista pública de avaliações do salão mostra o comentário {string}")
	public void aListaPublicaMostraOComentario(String comentario) {
		Response lista = api(null).get("/estabelecimentos/{id}/avaliacoes", estabelecimentoId);
		assertThat(lista.statusCode()).isEqualTo(200);
		assertThat(lista.jsonPath().getList("comentario", String.class)).containsExactly(comentario);
	}

}
