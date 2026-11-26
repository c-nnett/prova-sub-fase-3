package br.com.fiap.agendabeleza.carga;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.http.HttpDsl.http;

import java.time.Duration;
import java.util.UUID;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/**
 * O que todas as simulações compartilham: endereço da API, intensidade, protocolo HTTP e metas. A intensidade padrão
 * é a revista em 26/11 para o Docker Compose local (arquitetura, seção 9.3) e pode ser trocada por propriedades
 * {@code -Dcarga.<nome>=<valor>}, por exemplo para uma rodada curta de conferência.
 */
public final class ConfiguracaoDaCarga {

	public static final String URL_BASE = System.getProperty("carga.urlBase", "http://localhost:8082");
	public static final String USUARIO = "X-Usuario-Id";

	/** Usuários simultâneos das consultas (busca e horários livres). */
	public static final int USUARIOS_SIMULTANEOS = inteiro("usuarios", 100);
	/** Duração da fase constante das consultas e das reservas distintas. */
	public static final Duration DURACAO = Duration.ofSeconds(inteiro("duracao", 120));
	public static final Duration RAMPA = Duration.ofSeconds(inteiro("rampa", 10));
	public static final int RESERVAS_POR_SEGUNDO = inteiro("reservasPorSegundo", 30);
	public static final Duration DURACAO_DAS_RESERVAS = Duration.ofSeconds(inteiro("duracaoDasReservas", 60));
	public static final int CLIENTES_NA_DISPUTA = inteiro("clientesNaDisputa", 100);
	public static final int PROFISSIONAIS = inteiro("profissionais", 10);

	public static final int META_P95_CONSULTAS_MILISSEGUNDOS = 500;
	public static final int META_P95_RESERVAS_MILISSEGUNDOS = 800;
	public static final double META_PERCENTUAL_MAXIMO_DE_FALHAS = 1.0;

	private ConfiguracaoDaCarga() {
	}

	private static int inteiro(String nome, int padrao) {
		return Integer.getInteger("carga." + nome, padrao);
	}

	public static HttpProtocolBuilder protocolo() {
		return http.baseUrl(URL_BASE + "/api/v1")
				.acceptHeader("application/json")
				.contentTypeHeader("application/json; charset=utf-8");
	}

	/** Um cliente diferente por requisição, como na vida real: a trava é por profissional, não por cliente. */
	public static String clienteNovo(Session sessao) {
		return UUID.randomUUID().toString();
	}

	/**
	 * Modelo fechado das consultas: rampa até {@link #USUARIOS_SIMULTANEOS} e o mesmo número de usuários ativos
	 * durante {@link #DURACAO}. Cada usuário faz uma consulta e uma pausa curta, e outro entra no lugar.
	 */
	public static ClosedInjectionStep[] injecaoDasConsultas() {
		return new ClosedInjectionStep[] {
				rampConcurrentUsers(0).to(USUARIOS_SIMULTANEOS).during(RAMPA),
				constantConcurrentUsers(USUARIOS_SIMULTANEOS).during(DURACAO) };
	}

	/** Metas de tempo e de falhas do §9.3, iguais para todos os cenários exceto a disputa. */
	public static Assertion[] metas(int p95EmMilissegundos) {
		return new Assertion[] {
				global().responseTime().percentile(95.0).lt(p95EmMilissegundos),
				global().failedRequests().percent().lt(META_PERCENTUAL_MAXIMO_DE_FALHAS) };
	}

}
