package br.com.fiap.agendabeleza.api.calendario.domain;

/**
 * Resultado da emissão: a assinatura a gravar e o token em claro, que só existe neste momento e vai na resposta.
 */
public record AssinaturaEmitida(AssinaturaDoCalendario assinatura, TokenDoCalendario token) {
}
