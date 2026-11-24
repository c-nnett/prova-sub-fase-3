package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.Clock;
import java.time.Instant;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * O gestor ou o profissional registra o atendimento realizado: {@code CONFIRMADO -> CONCLUIDO}, a partir do início.
 */
public class ConcluirAgendamentoUseCase extends AlteracaoDeAgendamentoUseCase<ComandoDeAlteracao.Simples> {

	public ConcluirAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, Clock relogio) {
		super(agendamentos, profissionais, estabelecimentos, relogio, PermissaoNoAgendamento.GESTOR_OU_PROFISSIONAL,
				"concluir");
	}

	@Override
	void aplicar(PartesDoAgendamento partes, ComandoDeAlteracao.Simples comando, Instant agora) {
		partes.agendamento().concluir(comando.usuarioSolicitante(), agora);
	}

}
