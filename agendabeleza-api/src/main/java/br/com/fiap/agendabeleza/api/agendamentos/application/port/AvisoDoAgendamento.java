package br.com.fiap.agendabeleza.api.agendamentos.application.port;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * O que um aviso precisa saber do agendamento, sem expor os agregados a quem notifica.
 *
 * @param fuso fuso do estabelecimento, para o horário sair como o cliente o reservou
 */
public record AvisoDoAgendamento(
		UUID agendamentoId,
		Instant inicio,
		ZoneId fuso,
		String nomeEstabelecimento,
		String nomeProfissional,
		String emailProfissional) {

	public static AvisoDoAgendamento de(Agendamento agendamento, Profissional profissional,
			Estabelecimento estabelecimento) {
		return new AvisoDoAgendamento(agendamento.id(), agendamento.inicio(), estabelecimento.timezone(),
				estabelecimento.nome(), profissional.nome(), profissional.email());
	}

}
