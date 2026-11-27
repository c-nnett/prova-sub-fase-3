package br.com.fiap.agendabeleza.api.calendario.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;

/**
 * Segredo da URL do feed. Quem tem a URL lê a agenda, então o token faz o papel da autenticação que o Google
 * Calendar e o Outlook não mandam ao buscar uma agenda assinada (planejamento, seção 2.1).
 * <p>
 * São 256 bits aleatórios em Base64 para URL, sem preenchimento. O banco guarda só o {@link #hash()}: um vazamento
 * da tabela não abre nenhum feed. Um token fora do formato recebe a mesma resposta de um token inexistente, para
 * não dar pistas a quem tenta adivinhar.
 */
public final class TokenDoCalendario {

	public static final int TAMANHO = 43;
	public static final String CALENDARIO_NAO_ENCONTRADO = "Calendário não encontrado";

	private static final int QUANTIDADE_DE_BYTES = 32;
	private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9_-]{" + TAMANHO + "}");
	private static final SecureRandom GERADOR_ALEATORIO = new SecureRandom();
	private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();

	private final String valor;

	private TokenDoCalendario(String valor) {
		this.valor = valor;
	}

	public static TokenDoCalendario gerar() {
		byte[] bytesAleatorios = new byte[QUANTIDADE_DE_BYTES];
		GERADOR_ALEATORIO.nextBytes(bytesAleatorios);
		return new TokenDoCalendario(CODIFICADOR.encodeToString(bytesAleatorios));
	}

	/**
	 * @throws RecursoNaoEncontradoException se o texto não tiver o formato de um token gerado
	 */
	public static TokenDoCalendario de(String valor) {
		if (valor == null || !FORMATO.matcher(valor).matches()) {
			throw new RecursoNaoEncontradoException(CALENDARIO_NAO_ENCONTRADO);
		}
		return new TokenDoCalendario(valor);
	}

	/** SHA-256 em hexadecimal minúsculo, com 64 caracteres: a forma gravada e consultada no banco. */
	public String hash() {
		try {
			byte[] resumo = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.US_ASCII));
			return HexFormat.of().formatHex(resumo);
		} catch (NoSuchAlgorithmException falha) {
			throw new IllegalStateException("A JVM não oferece SHA-256", falha);
		}
	}

	public String valor() {
		return valor;
	}

	@Override
	public boolean equals(Object outro) {
		return outro instanceof TokenDoCalendario token && valor.equals(token.valor);
	}

	@Override
	public int hashCode() {
		return valor.hashCode();
	}

	/** Não expõe o segredo em logs. */
	@Override
	public String toString() {
		return "TokenDoCalendario[oculto]";
	}

}
