package br.com.fiap.agendabeleza.api.compartilhado.domain;

import java.util.List;
import java.util.function.Function;

/**
 * Uma página de resultados e os totais da consulta inteira. Substitui o {@code Page} do Spring Data, que o núcleo não
 * pode conhecer (arquitetura, seção 4).
 *
 * @param itens        itens desta página
 * @param numero       página, a partir de zero
 * @param tamanho      tamanho pedido da página
 * @param totalDeItens itens de todas as páginas
 */
public record Pagina<T>(List<T> itens, int numero, int tamanho, long totalDeItens) {

	public Pagina {
		if (totalDeItens < 0) {
			throw new IllegalArgumentException("total de itens negativo: " + totalDeItens);
		}
		itens = List.copyOf(itens);
	}

	public static <T> Pagina<T> de(List<T> itens, Paginacao paginacao, long totalDeItens) {
		return new Pagina<>(itens, paginacao.numero(), paginacao.tamanho(), totalDeItens);
	}

	public int totalDePaginas() {
		return (int) ((totalDeItens + tamanho - 1) / tamanho);
	}

	/** Converte os itens, por exemplo do domínio para a resposta HTTP, mantendo os totais. */
	public <R> Pagina<R> converter(Function<? super T, ? extends R> conversao) {
		List<R> convertidos = itens.stream().<R>map(conversao).toList();
		return new Pagina<>(convertidos, numero, tamanho, totalDeItens);
	}

}
