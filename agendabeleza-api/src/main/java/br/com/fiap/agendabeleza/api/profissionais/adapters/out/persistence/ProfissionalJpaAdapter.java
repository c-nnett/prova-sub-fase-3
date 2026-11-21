package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

/**
 * Implementa {@link ProfissionalRepository} com JPA. A cada gravação, monta um grafo novo a partir do agregado:
 * o {@code merge} recria as coleções de especialidades e serviços, e o {@code orphanRemoval} apaga os períodos
 * retirados da jornada.
 */
@Component
class ProfissionalJpaAdapter implements ProfissionalRepository {

	private final ProfissionalJpaRepository repositorio;

	ProfissionalJpaAdapter(ProfissionalJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional
	public Profissional salvar(Profissional profissional) {
		repositorio.save(paraEntidade(profissional));
		return profissional;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Profissional> buscarPorId(UUID id) {
		return repositorio.findById(id).map(ProfissionalJpaAdapter::paraDominio);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Profissional> listarPorEstabelecimento(UUID estabelecimentoId) {
		return repositorio.findByEstabelecimentoIdOrderByNome(estabelecimentoId).stream()
				.map(ProfissionalJpaAdapter::paraDominio)
				.toList();
	}

	private static ProfissionalJpaEntity paraEntidade(Profissional origem) {
		ProfissionalJpaEntity entidade = new ProfissionalJpaEntity();
		entidade.setId(origem.id());
		entidade.setEstabelecimentoId(origem.estabelecimentoId());
		entidade.setUsuarioId(origem.usuarioId());
		entidade.setNome(origem.nome());
		entidade.setEmail(origem.email());
		entidade.setBio(origem.bio());
		entidade.setNotaMedia(origem.notaMedia());
		entidade.setTotalAvaliacoes(origem.totalAvaliacoes());
		entidade.setAtivo(origem.ativo());
		entidade.getEspecialidadeIds().addAll(origem.especialidadeIds());
		for (ServicoProfissional servico : origem.servicos()) {
			entidade.getServicos().add(new ServicoProfissionalJpaEmbeddable(servico.servicoId(), servico.tarifa()));
		}
		for (PeriodoTrabalho periodo : origem.jornada()) {
			DisponibilidadeJpaEntity disponibilidade = new DisponibilidadeJpaEntity();
			// O período não tem identidade no domínio: a jornada é sempre substituída inteira.
			disponibilidade.setId(UUID.randomUUID());
			disponibilidade.setProfissional(entidade);
			disponibilidade.setDiaSemana((short) periodo.diaSemana().getValue());
			disponibilidade.setHoraInicio(periodo.inicio());
			disponibilidade.setHoraFim(periodo.fim());
			entidade.getDisponibilidades().add(disponibilidade);
		}
		return entidade;
	}

	private static Profissional paraDominio(ProfissionalJpaEntity entidade) {
		return Profissional.reconstituir(
				entidade.getId(),
				entidade.getEstabelecimentoId(),
				entidade.getUsuarioId(),
				entidade.getNome(),
				entidade.getEmail(),
				entidade.getBio(),
				entidade.getNotaMedia(),
				entidade.getTotalAvaliacoes(),
				entidade.isAtivo(),
				List.copyOf(entidade.getEspecialidadeIds()),
				entidade.getServicos().stream()
						.map(servico -> new ServicoProfissional(servico.getServicoId(), servico.getTarifa()))
						.toList(),
				entidade.getDisponibilidades().stream()
						.map(disponibilidade -> new PeriodoTrabalho(DayOfWeek.of(disponibilidade.getDiaSemana()),
								disponibilidade.getHoraInicio(), disponibilidade.getHoraFim()))
						.toList());
	}

}
