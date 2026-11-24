package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.EspecialidadeRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Substitui as especialidades do profissional. Todas precisam existir no catálogo. Gestor ou o próprio
 * profissional.
 */
public class DefinirEspecialidadesUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;
	private final EspecialidadeRepository especialidades;

	public DefinirEspecialidadesUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais, EspecialidadeRepository especialidades) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
		this.especialidades = especialidades;
	}

	public Profissional executar(Comando comando) {
		Profissional profissional = ProfissionalAlteravel.obter(profissionais, estabelecimentos,
				comando.profissionalId(), comando.usuarioId()).profissional();
		List<UUID> solicitadas = comando.especialidadeIds() == null ? List.of() : comando.especialidadeIds();
		Set<UUID> encontradas = especialidades.buscarPorIds(solicitadas).stream()
				.map(Especialidade::id)
				.collect(Collectors.toSet());
		solicitadas.stream()
				.filter(id -> !encontradas.contains(id))
				.findFirst()
				.ifPresent(id -> {
					throw new RegraNegocioException("Especialidade " + id + " não encontrada");
				});
		profissional.definirEspecialidades(solicitadas);
		return profissionais.salvar(profissional);
	}

	public record Comando(UUID profissionalId, UUID usuarioId, List<UUID> especialidadeIds) {
	}

}
