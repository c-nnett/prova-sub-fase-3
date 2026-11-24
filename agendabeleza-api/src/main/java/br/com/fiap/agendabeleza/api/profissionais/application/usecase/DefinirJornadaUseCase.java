package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Substitui a jornada semanal do profissional. Cada período precisa caber no horário de funcionamento do
 * estabelecimento naquele dia. Gestor ou o próprio profissional.
 */
public class DefinirJornadaUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;

	public DefinirJornadaUseCase(EstabelecimentoRepository estabelecimentos, ProfissionalRepository profissionais) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
	}

	public Profissional executar(Comando comando) {
		ProfissionalAlteravel alteravel = ProfissionalAlteravel.obter(profissionais, estabelecimentos,
				comando.profissionalId(), comando.usuarioId());
		List<PeriodoTrabalho> periodos = comando.periodos() == null ? List.of() : comando.periodos();
		periodos.forEach(periodo -> garantirDentroDoFuncionamento(alteravel.estabelecimento(), periodo));
		Profissional profissional = alteravel.profissional();
		profissional.definirJornada(periodos);
		return profissionais.salvar(profissional);
	}

	private static void garantirDentroDoFuncionamento(Estabelecimento estabelecimento, PeriodoTrabalho periodo) {
		if (periodo == null) {
			throw new RegraNegocioException("periodo é obrigatório");
		}
		HorarioFuncionamento horario = estabelecimento.horarioEm(periodo.diaSemana())
				.orElseThrow(() -> new RegraNegocioException(
						"O estabelecimento não funciona em " + periodo.diaSemana()));
		if (!horario.abrange(periodo.inicio(), periodo.fim())) {
			throw new RegraNegocioException("Período de " + periodo.diaSemana() + " das " + periodo.inicio()
					+ " às " + periodo.fim() + " fora do funcionamento do estabelecimento");
		}
	}

	public record Comando(UUID profissionalId, UUID usuarioId, List<PeriodoTrabalho> periodos) {
	}

}
