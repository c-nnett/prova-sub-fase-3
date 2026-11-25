package br.com.fiap.agendabeleza.api.compartilhado.domain;

import java.util.regex.Pattern;

/**
 * Validações reutilizadas pelas entidades. Toda falha gera {@link RegraNegocioException} com o nome do campo.
 */
public final class Validacoes {

	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
	private static final int TAMANHO_MAXIMO_EMAIL = 180;

	private Validacoes() {
	}

	/**
	 * Exige um e-mail com usuário, domínio e extensão, sem espaços. O tamanho segue as colunas de e-mail da
	 * modelagem ({@code varchar(180)}).
	 */
	public static String email(String valor, String campo) {
		String texto = textoObrigatorio(valor, campo, TAMANHO_MAXIMO_EMAIL);
		if (!EMAIL.matcher(texto).matches()) {
			throw new RegraNegocioException(campo + " inválido");
		}
		return texto;
	}

	public static <T> T obrigatorio(T valor, String campo) {
		if (valor == null) {
			throw new RegraNegocioException(campo + " é obrigatório");
		}
		return valor;
	}

	/** Remove espaços das pontas e exige conteúdo dentro do tamanho máximo. */
	public static String textoObrigatorio(String valor, String campo, int tamanhoMaximo) {
		String texto = textoOpcional(valor, campo, tamanhoMaximo);
		if (texto == null) {
			throw new RegraNegocioException(campo + " é obrigatório");
		}
		return texto;
	}

	/** Remove espaços das pontas. Texto vazio vira {@code null}. */
	public static String textoOpcional(String valor, String campo, int tamanhoMaximo) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		String texto = valor.strip();
		if (texto.length() > tamanhoMaximo) {
			throw new RegraNegocioException(campo + " deve ter no máximo " + tamanhoMaximo + " caracteres");
		}
		return texto;
	}

}
