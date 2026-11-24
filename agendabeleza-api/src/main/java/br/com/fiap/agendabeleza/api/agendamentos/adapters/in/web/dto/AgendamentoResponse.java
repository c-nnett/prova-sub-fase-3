package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import io.swagger.v3.oas.annotations.media.Schema;

public record AgendamentoResponse(
		UUID id,
		UUID clienteId,
		UUID estabelecimentoId,
		UUID profissionalId,
		UUID servicoId,
		@Schema(description = "Em UTC") Instant inicio,
		@Schema(description = "Em UTC. O intervalo é [inicio, fim)") Instant fim,
		int duracaoMinutos,
		StatusAgendamento status,
		@Schema(description = "Tarifa do profissional congelada no momento da reserva") BigDecimal preco,
		String observacoes) {

	public static AgendamentoResponse de(Agendamento agendamento) {
		return new AgendamentoResponse(agendamento.id(), agendamento.clienteId(), agendamento.estabelecimentoId(),
				agendamento.profissionalId(), agendamento.servicoId(), agendamento.inicio(), agendamento.fim(),
				agendamento.duracaoMinutos(), agendamento.status(), agendamento.preco(), agendamento.observacoes());
	}

}
