package br.com.fiap.agendabeleza.api.bdd;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.restassured.response.Response;

/**
 * Salão compartilhado pelas features de avaliação e de agendamento: um gestor novo, o serviço "Corte" (60 minutos)
 * e uma profissional que o executa às segundas. Como declara passos, não pode ser base de outra classe; os passos
 * das features o recebem com {@code @Autowired}, e o Cucumber-Spring cria uma instância por cenário.
 */
public class SalaoDoCenario extends ApiDaAplicacao {

	private UUID gestorId;
	private UUID estabelecimentoId;
	private UUID servicoId;
	private UUID profissionalId;

	@Before
	public void prepararGestor() {
		gestorId = UUID.randomUUID();
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

	/** Reserva o corte com a profissional, como o cliente informado. */
	public Response reservar(UUID clienteId, String inicio) {
		return api(clienteId).body("""
				{ "profissionalId": "%s", "servicoId": "%s", "inicio": "%s", "emailCliente": "cliente@email.com" }
				""".formatted(profissionalId, servicoId, inicio)).post("/agendamentos");
	}

	/** Inícios dos horários livres do corte no dia, como instantes, para não depender do offset devolvido. */
	public List<Instant> horariosLivresEm(LocalDate data) {
		Response consulta = api(null).queryParam("servicoId", servicoId).queryParam("data", data.toString())
				.get("/profissionais/{id}/horarios-livres", profissionalId);
		exigirSucesso(consulta);
		return consulta.jsonPath().getList("horarios.inicio", String.class).stream()
				.map(inicio -> OffsetDateTime.parse(inicio).toInstant())
				.toList();
	}

	public UUID gestorId() {
		return gestorId;
	}

	public UUID estabelecimentoId() {
		return estabelecimentoId;
	}

	public UUID profissionalId() {
		return profissionalId;
	}

}
