package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;
import io.swagger.v3.oas.annotations.media.Schema;

public record BloqueioResponse(
		UUID id,
		@Schema(description = "Início, em UTC") Instant inicio,
		@Schema(description = "Fim, exclusivo e em UTC") Instant fim,
		String motivo) {

	public static BloqueioResponse de(BloqueioAgenda bloqueio) {
		return new BloqueioResponse(bloqueio.id(), bloqueio.periodo().inicio(), bloqueio.periodo().fim(),
				bloqueio.motivo());
	}

}
