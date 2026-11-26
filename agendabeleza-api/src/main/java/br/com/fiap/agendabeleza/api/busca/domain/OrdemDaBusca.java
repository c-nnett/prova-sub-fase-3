package br.com.fiap.agendabeleza.api.busca.domain;

/**
 * Ordens aceitas na busca. A lista é fechada, em vez de um {@code sort} livre com nome de propriedade, para não
 * expor o modelo de persistência. Toda ordem desempata pelo identificador, para que as páginas sejam estáveis.
 */
public enum OrdemDaBusca {

	/** Maior nota média primeiro; sem avaliação vai para o fim; depois por nome. */
	MELHOR_AVALIADOS,

	/** Nome em ordem alfabética. */
	NOME;

	public static final OrdemDaBusca PADRAO = MELHOR_AVALIADOS;

}
