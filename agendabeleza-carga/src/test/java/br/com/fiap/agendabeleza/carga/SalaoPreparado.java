package br.com.fiap.agendabeleza.carga;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dados criados pela {@link PreparacaoDaCarga} antes da carga.
 *
 * @param primeiroDia primeiro dia usado nas reservas e consultas, no futuro distante para não envelhecer
 */
public record SalaoPreparado(
		UUID gestorId,
		UUID estabelecimentoId,
		UUID servicoId,
		List<UUID> profissionais,
		LocalDate primeiroDia) {

	public SalaoPreparado {
		profissionais = List.copyOf(profissionais);
	}

}
