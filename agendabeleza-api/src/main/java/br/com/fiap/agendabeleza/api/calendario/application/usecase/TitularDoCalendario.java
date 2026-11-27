package br.com.fiap.agendabeleza.api.calendario.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

/**
 * Strategy do que muda entre o feed do profissional e o do cliente: quem pode assinar, quais agendamentos entram e
 * como o evento e o calendário são nomeados. A emissão e a geração do feed são as mesmas para os dois papéis.
 */
public interface TitularDoCalendario {

	PapelNoCalendario papel();

	/**
	 * @throws br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException se o titular não
	 *                                                                                         existir
	 * @throws br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException         se o usuário não
	 *                                                                                         puder assinar
	 */
	void exigirAcesso(UUID titularId, UUID usuarioId);

	/** Todos os status, em ordem de início; o cancelado é descartado depois, ao virar evento. */
	List<Agendamento> agendamentosDesde(UUID titularId, Instant desde);

	String nomeDoCalendario(UUID titularId);

	String resumoDoEvento(Servico servico, Estabelecimento estabelecimento);

}
