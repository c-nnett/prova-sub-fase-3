package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.DadosDoPeriodo;
import br.com.fiap.agendabeleza.api.agendamentos.domain.ResumoDaAgenda;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import io.swagger.v3.oas.annotations.media.Schema;

public record ResumoDaAgendaResponse(
		@Schema(description = "Primeiro dia, no fuso do estabelecimento") LocalDate de,
		@Schema(description = "Último dia, inclusivo") LocalDate ate,
		@Schema(description = "Fuso do estabelecimento", example = "America/Sao_Paulo") ZoneId fuso,
		int total,
		@Schema(description = "Todos os status, com zero quando não houver") Map<StatusAgendamento, Long> porStatus,
		@Schema(description = "Soma dos preços dos agendamentos CONCLUIDO") BigDecimal receitaRealizada,
		@Schema(description = "Soma dos preços dos agendamentos AGENDADO e CONFIRMADO") BigDecimal receitaPrevista) {

	public static ResumoDaAgendaResponse de(DadosDoPeriodo<ResumoDaAgenda> resumoDoPeriodo) {
		ResumoDaAgenda resumo = resumoDoPeriodo.dados();
		return new ResumoDaAgendaResponse(resumoDoPeriodo.periodo().de(), resumoDoPeriodo.periodo().ate(),
				resumoDoPeriodo.periodo().fuso(), resumo.total(), resumo.porStatus(), resumo.receitaRealizada(),
				resumo.receitaPrevista());
	}

}
