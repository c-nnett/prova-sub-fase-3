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
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * O cliente ou o gestor move o agendamento para outro início, mantendo a duração. O status volta para
 * {@code AGENDADO}, e o histórico guarda o início anterior e o novo.
 * <p>
 * O novo horário passa pelas mesmas verificações da reserva, sem contar o horário que o próprio agendamento ocupa
 * hoje. Verificação e gravação rodam em {@link ExecucaoSerializada} por profissional, como a reserva e o bloqueio,
 * para que um bloqueio simultâneo no novo horário não passe junto. Na mesma execução, cliente e profissional são
 * avisados do novo horário.
 */
public class ReagendarAgendamentoUseCase extends AlteracaoDeAgendamentoUseCase<ReagendarAgendamentoUseCase.Comando> {

	private final DisponibilidadeDaAgenda disponibilidade;
	private final ExecucaoSerializada execucao;
	private final AvisosDoAgendamento avisos;

	public ReagendarAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, BloqueioAgendaRepository bloqueios,
			ExecucaoSerializada execucao, AvisosDoAgendamento avisos, Clock relogio) {
		super(agendamentos, profissionais, estabelecimentos, relogio, PermissaoNoAgendamento.CLIENTE_OU_GESTOR,
				"reagendar este agendamento");
		this.disponibilidade = new DisponibilidadeDaAgenda(agendamentos, bloqueios);
		this.execucao = execucao;
		this.avisos = avisos;
	}

	@Override
	void aplicar(PartesDoAgendamento partes, Comando comando, Instant agora) {
		DisponibilidadeDaAgenda.exigirProfissionalAtivo(partes.profissional());
		DisponibilidadeDaAgenda.exigirEstabelecimentoAtivo(partes.estabelecimento());
		Agendamento agendamento = partes.agendamento();
		Intervalo intervaloAtual = agendamento.intervalo();
		agendamento.reagendar(comando.novoInicio(), comando.usuarioSolicitante(), agora);
		disponibilidade.garantirHorarioLivreIgnorando(intervaloAtual, agendamento.intervalo(), partes.profissional(),
				partes.estabelecimento(), agora);
	}

	@Override
	<T> T executarNaAgenda(UUID profissionalId, Supplier<T> alteracao) {
		return execucao.executar(profissionalId, alteracao);
	}

	/** O aviso leva o novo início, já gravado. */
	@Override
	void depoisDeGravar(PartesDoAgendamento partes, Agendamento gravado) {
		avisos.reagendado(AvisoDoAgendamento.de(gravado, partes.profissional(), partes.estabelecimento()));
	}

	public record Comando(UUID agendamentoId, UUID usuarioSolicitante, Instant novoInicio)
			implements ComandoDeAlteracao {
	}

}
