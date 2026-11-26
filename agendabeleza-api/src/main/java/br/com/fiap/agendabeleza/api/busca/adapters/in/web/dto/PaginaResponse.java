package br.com.fiap.agendabeleza.api.busca.adapters.in.web.dto;

import java.util.List;
import java.util.function.Function;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;

/**
 * Uma página de resultados com os totais da consulta inteira.
 *
 * @param pagina a partir de zero
 */
public record PaginaResponse<T>(List<T> itens, int pagina, int tamanho, long totalDeItens, int totalDePaginas) {

	public static <D, T> PaginaResponse<T> de(Pagina<D> pagina, Function<? super D, ? extends T> conversao) {
		Pagina<T> convertida = pagina.converter(conversao);
		return new PaginaResponse<>(convertida.itens(), convertida.numero(), convertida.tamanho(),
				convertida.totalDeItens(), convertida.totalDePaginas());
	}

}
