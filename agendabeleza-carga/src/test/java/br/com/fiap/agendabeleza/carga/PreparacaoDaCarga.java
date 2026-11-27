package br.com.fiap.agendabeleza.carga;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Facade sobre o {@link HttpClient} para criar, pela própria API e antes da carga, um salão novo: gestor novo,
 * funcionamento e jornada em todos os dias das {@link #ABERTURA} às {@link #FECHAMENTO}, o serviço "Corte" de
 * {@link #DURACAO_DO_SERVICO_EM_MINUTOS} minutos e os profissionais pedidos. Cada execução usa um salão próprio, então
 * os horários não colidem com execuções anteriores nem com os dados de outros testes.
 * <p>
 * Também faz as consultas de conferência depois da carga. O corpo vai em UTF-8 pela própria API do Java, e o
 * {@code id} criado sai da resposta por expressão regular, sem biblioteca de JSON.
 */
public final class PreparacaoDaCarga {

	public static final LocalTime ABERTURA = LocalTime.of(6, 0);
	public static final LocalTime FECHAMENTO = LocalTime.of(22, 0);
	public static final int DURACAO_DO_SERVICO_EM_MINUTOS = 15;
	public static final String CIDADE = "São Paulo";
	public static final String NOME_DO_SALAO = "Salão da Carga";
	/** Segunda-feira no futuro distante. Em 2031 o fuso de São Paulo é UTC-3 o ano todo. */
	public static final LocalDate PRIMEIRO_DIA = LocalDate.of(2031, 1, 6);

	private static final Pattern ID_CRIADO = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-f-]{36})\"");
	private static final Pattern TOTAL = Pattern.compile("\"total\"\\s*:\\s*(\\d+)");
	private static final HttpClient CLIENTE_HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
			.build();

	private PreparacaoDaCarga() {
	}

	public static SalaoPreparado prepararSalao(int quantidadeDeProfissionais) {
		UUID gestorId = UUID.randomUUID();
		UUID estabelecimentoId = idCriado(enviar("POST", "/estabelecimentos", gestorId, """
				{
				  "nome": "%s %s",
				  "endereco": { "logradouro": "Rua Augusta", "numero": "100", "bairro": "Consolação",
				                "cidade": "%s", "uf": "SP", "cep": "01305-000" },
				  "horarios": [ %s ]
				}
				""".formatted(NOME_DO_SALAO, gestorId.toString().substring(0, 8), CIDADE, todosOsDias(
				"{ \"diaSemana\": \"%s\", \"abreAs\": \"%s\", \"fechaAs\": \"%s\" }"))));
		UUID servicoId = idCriado(enviar("POST", "/estabelecimentos/" + estabelecimentoId + "/servicos", gestorId,
				"""
						{ "nome": "Corte", "categoria": "CABELO", "duracaoMinutos": %d, "precoBase": 50.00 }
						""".formatted(DURACAO_DO_SERVICO_EM_MINUTOS)));
		List<UUID> profissionais = new ArrayList<>();
		for (int numero = 1; numero <= quantidadeDeProfissionais; numero++) {
			profissionais.add(prepararProfissional(gestorId, estabelecimentoId, servicoId, numero));
		}
		return new SalaoPreparado(gestorId, estabelecimentoId, servicoId, profissionais, PRIMEIRO_DIA);
	}

	private static UUID prepararProfissional(UUID gestorId, UUID estabelecimentoId, UUID servicoId, int numero) {
		UUID profissionalId = idCriado(enviar("POST", "/estabelecimentos/" + estabelecimentoId + "/profissionais",
				gestorId, """
						{ "nome": "Profissional %d", "email": "profissional%d-%s@carga.com" }
						""".formatted(numero, numero, gestorId.toString().substring(0, 8))));
		exigirSucesso(enviar("PUT", "/profissionais/" + profissionalId + "/servicos", gestorId, """
				{ "servicos": [ { "servicoId": "%s" } ] }
				""".formatted(servicoId)));
		exigirSucesso(enviar("PUT", "/profissionais/" + profissionalId + "/disponibilidades", gestorId,
				"{ \"periodos\": [ %s ] }".formatted(todosOsDias(
						"{ \"diaSemana\": \"%s\", \"inicio\": \"%s\", \"fim\": \"%s\" }"))));
		return profissionalId;
	}

	/** Quantos agendamentos começam no dia, pelo resumo do painel do gestor. */
	public static int totalDeAgendamentosNoDia(SalaoPreparado salao, LocalDate dia) {
		HttpResponse<String> resumo = enviar("GET", "/estabelecimentos/" + salao.estabelecimentoId()
				+ "/painel/resumo?de=" + dia + "&ate=" + dia, salao.gestorId(), null);
		exigirSucesso(resumo);
		Matcher total = TOTAL.matcher(resumo.body());
		if (!total.find()) {
			throw new IllegalStateException("Resumo sem total: " + resumo.body());
		}
		return Integer.parseInt(total.group(1));
	}

	/**
	 * O mesmo horário, de abertura ao fechamento, para os sete dias da semana.
	 *
	 * @param modelo objeto JSON de um dia, com espaços para o dia, a abertura e o fechamento
	 */
	private static String todosOsDias(String modelo) {
		return Arrays.stream(DayOfWeek.values())
				.map(dia -> modelo.formatted(dia.name(), ABERTURA, FECHAMENTO))
				.collect(Collectors.joining(", "));
	}

	private static HttpResponse<String> enviar(String metodo, String caminho, UUID usuarioId, String corpo) {
		HttpRequest requisicao = HttpRequest.newBuilder(URI.create(ConfiguracaoDaCarga.URL_BASE + "/api/v1" + caminho))
				.timeout(Duration.ofSeconds(30))
				.header("Content-Type", "application/json; charset=utf-8")
				.header(ConfiguracaoDaCarga.USUARIO, usuarioId.toString())
				.method(metodo, corpo == null ? BodyPublishers.noBody()
						: BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
				.build();
		try {
			return CLIENTE_HTTP.send(requisicao, BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (IOException falha) {
			throw new IllegalStateException("A API não respondeu em " + ConfiguracaoDaCarga.URL_BASE
					+ ". O Docker Compose está no ar?", falha);
		} catch (InterruptedException interrupcao) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Preparação interrompida", interrupcao);
		}
	}

	private static UUID idCriado(HttpResponse<String> resposta) {
		exigirSucesso(resposta);
		Matcher id = ID_CRIADO.matcher(resposta.body());
		if (!id.find()) {
			throw new IllegalStateException("Resposta sem id: " + resposta.body());
		}
		return UUID.fromString(id.group(1));
	}

	private static void exigirSucesso(HttpResponse<String> resposta) {
		if (resposta.statusCode() < 200 || resposta.statusCode() > 299) {
			throw new IllegalStateException("Preparação recusada com " + resposta.statusCode() + " em "
					+ resposta.request().uri() + ": " + resposta.body());
		}
	}

}
