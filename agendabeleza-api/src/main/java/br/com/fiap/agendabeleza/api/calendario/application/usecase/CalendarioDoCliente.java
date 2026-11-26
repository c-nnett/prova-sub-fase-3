package br.com.fiap.agendabeleza.api.calendario.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

/**
 * Feed com os horários que o cliente reservou, em qualquer estabelecimento. Como não há cadastro de usuários, o
 * titular é o próprio {@code X-Usuario-Id}, e o título do evento mostra onde é o atendimento.
 */
public class CalendarioDoCliente implements TitularDoCalendario {

	public static final String NOME_DO_CALENDARIO = "AgendaBeleza - Meus agendamentos";
	public static final String SOMENTE_O_PROPRIO_CLIENTE = "Somente o próprio cliente pode assinar os horários dele";

	private final AgendamentoRepository agendamentos;

	public CalendarioDoCliente(AgendamentoRepository agendamentos) {
		this.agendamentos = agendamentos;
	}

	@Override
	public PapelNoCalendario papel() {
		return PapelNoCalendario.CLIENTE;
	}

	@Override
	public void exigirAcesso(UUID titularId, UUID usuarioId) {
		if (!titularId.equals(usuarioId)) {
			throw new AcessoNegadoException(SOMENTE_O_PROPRIO_CLIENTE);
		}
	}

	@Override
	public List<Agendamento> agendamentosDesde(UUID titularId, Instant desde) {
		return agendamentos.listarDoClienteDesde(titularId, desde);
	}

	@Override
	public String nomeDoCalendario(UUID titularId) {
		return NOME_DO_CALENDARIO;
	}

	@Override
	public String resumoDoEvento(Servico servico, Estabelecimento estabelecimento) {
		return servico.nome() + " em " + estabelecimento.nome();
	}

}
