package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.Clock;
import java.time.Instant;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/** O gestor ou o profissional confirma a reserva: {@code AGENDADO -> CONFIRMADO}. */
public class ConfirmarAgendamentoUseCase extends AlteracaoDeAgendamentoUseCase<ComandoDeAlteracao.Simples> {

	public ConfirmarAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, Clock relogio) {
		super(agendamentos, profissionais, estabelecimentos, relogio, PermissaoNoAgendamento.GESTOR_OU_PROFISSIONAL,
				"confirmar este agendamento");
	}

	@Override
	void aplicar(PartesDoAgendamento partes, ComandoDeAlteracao.Simples comando, Instant agora) {
		partes.agendamento().confirmar(comando.usuarioSolicitante(), agora);
	}

}
