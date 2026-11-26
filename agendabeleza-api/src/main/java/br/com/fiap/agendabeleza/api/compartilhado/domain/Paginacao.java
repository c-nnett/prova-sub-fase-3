package br.com.fiap.agendabeleza.api.compartilhado.domain;

/**
 * Página pedida de uma listagem, contada a partir de zero. O tamanho é limitado para que uma consulta pública não
 * devolva o catálogo inteiro de uma vez (arquitetura, seção 5.4).
 *
 * @param numero  página, a partir de zero
 * @param tamanho itens por página, de 1 a {@link #TAMANHO_MAXIMO}
 */
public record Paginacao(int numero, int tamanho) {

	public static final int TAMANHO_PADRAO = 20;
	public static final int TAMANHO_MAXIMO = 50;
	public static final String PAGINA_NEGATIVA = "A página começa em zero";
	public static final String TAMANHO_FORA_DO_LIMITE =
			"O tamanho da página deve ficar entre 1 e " + TAMANHO_MAXIMO;

	public Paginacao {
		if (numero < 0) {
			throw new RegraNegocioException(PAGINA_NEGATIVA);
		}
		if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
			throw new RegraNegocioException(TAMANHO_FORA_DO_LIMITE);
		}
	}

	/** Valores ausentes viram a primeira página com {@link #TAMANHO_PADRAO} itens. */
	public static Paginacao de(Integer numero, Integer tamanho) {
		return new Paginacao(numero == null ? 0 : numero, tamanho == null ? TAMANHO_PADRAO : tamanho);
	}

	/** Quantidade de itens das páginas anteriores. */
	public long deslocamento() {
		return (long) numero * tamanho;
	}

}
