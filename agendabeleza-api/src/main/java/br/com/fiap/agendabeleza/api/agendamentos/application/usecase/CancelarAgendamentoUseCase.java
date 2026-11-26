package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisoDoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisosDoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.compartilhado.application.port.ExecucaoSerializada;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * O cliente ou o gestor cancela um agendamento ativo, liberando o horário. O motivo é opcional e fica no histórico.
 * <p>
 * Cliente e profissional são avisados. A gravação e o aviso rodam em {@link ExecucaoSerializada}, que dá a
 * transação única para a outbox; a trava por profissional só faz o cancelamento esperar uma reserva ou bloqueio
 * do mesmo profissional em andamento.
 */
public class CancelarAgendamentoUseCase extends AlteracaoDeAgendamentoUseCase<CancelarAgendamentoUseCase.Comando> {

	private final ExecucaoSerializada execucao;
	private final AvisosDoAgendamento avisos;

	public CancelarAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, ExecucaoSerializada execucao, AvisosDoAgendamento avisos,
			Clock relogio) {
		super(agendamentos, profissionais, estabelecimentos, relogio, PermissaoNoAgendamento.CLIENTE_OU_GESTOR,
				"cancelar este agendamento");
		this.execucao = execucao;
		this.avisos = avisos;
	}

	@Override
	void aplicar(PartesDoAgendamento partes, Comando comando, Instant agora) {
		partes.agendamento().cancelar(comando.motivo(), comando.usuarioSolicitante(), agora);
	}

	@Override
	<T> T executarNaAgenda(UUID profissionalId, Supplier<T> alteracao) {
		return execucao.executar(profissionalId, alteracao);
	}

	@Override
	void depoisDeGravar(PartesDoAgendamento partes, Agendamento gravado) {
		avisos.cancelado(AvisoDoAgendamento.de(gravado, partes.profissional(), partes.estabelecimento()));
	}

	public record Comando(UUID agendamentoId, UUID usuarioSolicitante, String motivo) implements ComandoDeAlteracao {
	}

}
