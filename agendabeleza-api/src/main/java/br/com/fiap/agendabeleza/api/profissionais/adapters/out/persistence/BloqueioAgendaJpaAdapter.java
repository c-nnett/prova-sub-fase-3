package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;

/**
 * Implementa {@link BloqueioAgendaRepository} com JPA. A consulta por período usa o índice
 * {@code idx_bloqueio_prof_periodo}.
 */
@Component
class BloqueioAgendaJpaAdapter implements BloqueioAgendaRepository {

	private final BloqueioAgendaJpaRepository repositorio;

	BloqueioAgendaJpaAdapter(BloqueioAgendaJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional
	public BloqueioAgenda salvar(BloqueioAgenda bloqueio) {
		repositorio.saveAndFlush(paraEntidade(bloqueio));
		return bloqueio;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<BloqueioAgenda> buscarPorId(UUID id) {
		return repositorio.findById(id).map(BloqueioAgendaJpaAdapter::paraDominio);
	}

	@Override
	@Transactional
	public void remover(UUID id) {
		repositorio.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<BloqueioAgenda> listarNoPeriodo(UUID profissionalId, Intervalo periodo) {
		return repositorio.findByProfissionalIdAndInicioBeforeAndFimAfterOrderByInicio(profissionalId,
				periodo.fim(), periodo.inicio()).stream()
				.map(BloqueioAgendaJpaAdapter::paraDominio)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<BloqueioAgenda> listarVigentes(UUID profissionalId, Instant agora) {
		return repositorio.findByProfissionalIdAndFimAfterOrderByInicio(profissionalId, agora).stream()
				.map(BloqueioAgendaJpaAdapter::paraDominio)
				.toList();
	}

	private static BloqueioAgendaJpaEntity paraEntidade(BloqueioAgenda origem) {
		BloqueioAgendaJpaEntity entidade = new BloqueioAgendaJpaEntity();
		entidade.setId(origem.id());
		entidade.setProfissionalId(origem.profissionalId());
		entidade.setInicio(origem.periodo().inicio());
		entidade.setFim(origem.periodo().fim());
		entidade.setMotivo(origem.motivo());
		return entidade;
	}

	private static BloqueioAgenda paraDominio(BloqueioAgendaJpaEntity entidade) {
		return BloqueioAgenda.reconstituir(entidade.getId(), entidade.getProfissionalId(), entidade.getInicio(),
				entidade.getFim(), entidade.getMotivo());
	}

}
