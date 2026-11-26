package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.Clock;
import java.time.Instant;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * O gestor ou o profissional registra que o cliente faltou: {@code CONFIRMADO -> NAO_COMPARECEU}, a partir do
 * início.
 */
public class RegistrarNaoComparecimentoUseCase extends AlteracaoDeAgendamentoUseCase<ComandoDeAlteracao.Simples> {

	public RegistrarNaoComparecimentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, Clock relogio) {
		super(agendamentos, profissionais, estabelecimentos, relogio, PermissaoNoAgendamento.GESTOR_OU_PROFISSIONAL,
				"registrar o não comparecimento deste agendamento");
	}

	@Override
	void aplicar(PartesDoAgendamento partes, ComandoDeAlteracao.Simples comando, Instant agora) {
		partes.agendamento().registrarNaoComparecimento(comando.usuarioSolicitante(), agora);
	}

}
