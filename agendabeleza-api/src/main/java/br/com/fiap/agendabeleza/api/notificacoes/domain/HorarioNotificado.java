package br.com.fiap.agendabeleza.api.notificacoes.domain;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Onde, com quem e quando é o atendimento, como aparece no texto do e-mail.
 *
 * @param fuso fuso do estabelecimento, usado para formatar o início
 */
public record HorarioNotificado(String nomeEstabelecimento, String nomeProfissional, Instant inicio, ZoneId fuso) {
}
