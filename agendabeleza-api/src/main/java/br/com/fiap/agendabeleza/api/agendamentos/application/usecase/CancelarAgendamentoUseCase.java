package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * O cliente ou o gestor cancela um agendamento ativo, liberando o horário. O motivo é opcional e fica no histórico.
 */
public class CancelarAgendamentoUseCase extends AlteracaoDeAgendamentoUseCase<CancelarAgendamentoUseCase.Comando> {

	public CancelarAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, Clock relogio) {
		super(agendamentos, profissionais, estabelecimentos, relogio, PermissaoNoAgendamento.CLIENTE_OU_GESTOR,
				"cancelar");
	}

	@Override
	void aplicar(PartesDoAgendamento partes, Comando comando, Instant agora) {
		partes.agendamento().cancelar(comando.motivo(), comando.usuarioSolicitante(), agora);
	}

	public record Comando(UUID agendamentoId, UUID usuarioSolicitante, String motivo) implements ComandoDeAlteracao {
	}

}
