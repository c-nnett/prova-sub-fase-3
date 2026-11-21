package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;

/**
 * Substitui os horários de funcionamento do estabelecimento. Somente o gestor dono pode alterar.
 */
public class DefinirHorariosFuncionamentoUseCase {

	private final EstabelecimentoRepository repositorio;

	public DefinirHorariosFuncionamentoUseCase(EstabelecimentoRepository repositorio) {
		this.repositorio = repositorio;
	}

	public Estabelecimento executar(Comando comando) {
		Estabelecimento estabelecimento = EstabelecimentoDoGestor.obter(
				repositorio, comando.estabelecimentoId(), comando.usuarioId());
		estabelecimento.definirHorarios(comando.horarios());
		return repositorio.salvar(estabelecimento);
	}

	public record Comando(UUID estabelecimentoId, UUID usuarioId, List<HorarioFuncionamento> horarios) {
	}

}
