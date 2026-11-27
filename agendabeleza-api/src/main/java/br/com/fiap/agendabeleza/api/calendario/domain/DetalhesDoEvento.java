package br.com.fiap.agendabeleza.api.calendario.domain;

/**
 * Textos do evento, que mudam conforme quem assina: o cliente vê o estabelecimento, o profissional vê o serviço.
 *
 * @param resumo    título do evento
 * @param local     endereço do estabelecimento
 * @param descricao linhas complementares (profissional, serviço, preço)
 */
public record DetalhesDoEvento(String resumo, String local, String descricao) {
}
