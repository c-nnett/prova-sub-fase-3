package br.com.fiap.agendabeleza.api.compartilhado.domain;

/**
 * Validações reutilizadas pelas entidades. Toda falha gera {@link RegraNegocioException} com o nome do campo.
 */
public final class Validacoes {

	private Validacoes() {
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
