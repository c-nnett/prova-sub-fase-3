package br.com.fiap.agendabeleza.api.busca.domain;

import java.math.BigDecimal;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;

/**
 * Critérios combináveis da busca de estabelecimentos (funcionalidade 5). Critério nulo não filtra, e texto em branco
 * vira nulo. Só estabelecimentos ativos aparecem.
 * <p>
 * Serviço, categoria e faixa de preço valem para <b>o mesmo serviço</b>: "corte entre R$ 50 e R$ 90" exige um
 * serviço ativo de corte com uma tarifa de profissional ativo nessa faixa. O preço é a tarifa do profissional, que é o
 * valor cobrado na reserva (modelagem, seção 4.3).
 *
 * @param nome            trecho do nome do estabelecimento, sem diferenciar maiúsculas
 * @param cidade          cidade exata, sem diferenciar maiúsculas
 * @param bairro          bairro exato, sem diferenciar maiúsculas
 * @param servico         trecho do nome de um serviço
 * @param notaMinima      nota média mínima, de 0 a 5; estabelecimentos sem avaliação ficam de fora quando informada
 * @param disponibilidade nula não filtra; com serviço, categoria ou preço, o profissional disponível precisa executar
 *                        o serviço filtrado
 */
@SuppressWarnings("java:S107") // Um componente por critério da busca.
public record FiltroDaBusca(String nome, String cidade, String bairro, String servico, CategoriaServico categoria,
		BigDecimal notaMinima, BigDecimal precoMinimo, BigDecimal precoMaximo,
		JanelaDeDisponibilidade disponibilidade) {

	public static final String FAIXA_DE_PRECO_INVERTIDA = "O preço mínimo não pode ser maior que o preço máximo";
	public static final String PRECO_NEGATIVO = "O preço da busca não pode ser negativo";
	public static final String NOTA_MINIMA_FORA_DA_ESCALA = "A nota mínima deve ficar entre 0 e 5";

	private static final BigDecimal NOTA_MAXIMA = BigDecimal.valueOf(5);
	private static final int TAMANHO_NOME_ESTABELECIMENTO = 150;
	private static final int TAMANHO_CIDADE = 80;
	private static final int TAMANHO_BAIRRO = 80;
	private static final int TAMANHO_NOME_SERVICO = 120;

	public FiltroDaBusca {
		nome = Validacoes.textoOpcional(nome, "nome", TAMANHO_NOME_ESTABELECIMENTO);
		cidade = Validacoes.textoOpcional(cidade, "cidade", TAMANHO_CIDADE);
		bairro = Validacoes.textoOpcional(bairro, "bairro", TAMANHO_BAIRRO);
		servico = Validacoes.textoOpcional(servico, "servico", TAMANHO_NOME_SERVICO);
		exigirNotaDaEscala(notaMinima);
		exigirPrecoNaoNegativo(precoMinimo);
		exigirPrecoNaoNegativo(precoMaximo);
		if (precoMinimo != null && precoMaximo != null && precoMinimo.compareTo(precoMaximo) > 0) {
			throw new RegraNegocioException(FAIXA_DE_PRECO_INVERTIDA);
		}
	}

	/** Indica se algum critério exige um serviço do estabelecimento. */
	public boolean filtraPorServico() {
		return servico != null || categoria != null || precoMinimo != null || precoMaximo != null;
	}

	private static void exigirNotaDaEscala(BigDecimal nota) {
		if (nota != null && (nota.signum() < 0 || nota.compareTo(NOTA_MAXIMA) > 0)) {
			throw new RegraNegocioException(NOTA_MINIMA_FORA_DA_ESCALA);
		}
	}

	private static void exigirPrecoNaoNegativo(BigDecimal preco) {
		if (preco != null && preco.signum() < 0) {
			throw new RegraNegocioException(PRECO_NEGATIVO);
		}
	}

}
