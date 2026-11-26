package br.com.fiap.agendabeleza.api.busca.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resumo de um estabelecimento no resultado da busca. Os detalhes (serviços, profissionais, horários) ficam em
 * {@code GET /estabelecimentos/{id}}.
 *
 * @param notaMedia nula enquanto não houver avaliação
 */
public record EstabelecimentoEncontrado(UUID id, String nome, String bairro, String cidade, String uf,
		BigDecimal notaMedia, int totalAvaliacoes) {
}
