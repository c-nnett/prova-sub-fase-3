package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * Detalhes de um agendamento para as partes envolvidas: o cliente, o gestor do estabelecimento e o profissional.
 */
public class ConsultarAgendamentoUseCase {

	private final AgendamentoRepository agendamentos;
	private final ProfissionalRepository profissionais;
	private final EstabelecimentoRepository estabelecimentos;

	public ConsultarAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos) {
		this.agendamentos = agendamentos;
		this.profissionais = profissionais;
		this.estabelecimentos = estabelecimentos;
	}

	public Agendamento executar(UUID agendamentoId, UUID usuarioId) {
		PartesDoAgendamento partes = PartesDoAgendamento.carregar(agendamentos, profissionais, estabelecimentos,
				agendamentoId);
		PermissaoNoAgendamento.PARTES_ENVOLVIDAS.exigir(partes, usuarioId, "consultar");
		return partes.agendamento();
	}

}
