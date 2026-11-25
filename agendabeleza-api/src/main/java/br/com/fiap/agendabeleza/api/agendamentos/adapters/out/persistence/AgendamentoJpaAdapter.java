package br.com.fiap.agendabeleza.api.agendamentos.adapters.out.persistence;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.FiltroDaAgenda;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.AlteracaoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;

/**
 * Implementa {@link AgendamentoRepository} com JPA. O {@code saveAndFlush} faz a restrição
 * {@code agendamento_sem_conflito} disparar dentro do método, onde o SQLState {@code 23P01} vira
 * {@link HorarioIndisponivelException}.
 */
@Component
class AgendamentoJpaAdapter implements AgendamentoRepository {

	/** {@code exclusion_violation} do PostgreSQL. */
	static final String VIOLACAO_EXCLUSAO = "23P01";

	private static final List<StatusAgendamento> STATUS_QUE_OCUPAM = Arrays.stream(StatusAgendamento.values())
			.filter(StatusAgendamento::ocupaHorario)
			.toList();

	private static final Sort ORDEM_DE_INICIO = Sort.by("inicio");

	private final AgendamentoJpaRepository repositorio;
	private final AgendamentoHistoricoJpaRepository historico;

	AgendamentoJpaAdapter(AgendamentoJpaRepository repositorio, AgendamentoHistoricoJpaRepository historico) {
		this.repositorio = repositorio;
		this.historico = historico;
	}

	@Override
	@Transactional
	public Agendamento salvar(Agendamento agendamento) {
		AgendamentoJpaEntity gravado;
		try {
			gravado = repositorio.saveAndFlush(paraEntidade(agendamento));
		} catch (DataIntegrityViolationException excecao) {
			if (violouExclusao(excecao)) {
				throw new HorarioIndisponivelException();
			}
			throw excecao;
		}
		historico.saveAll(agendamento.alteracoesPendentes().stream()
				.map(alteracao -> paraHistorico(agendamento.id(), alteracao))
				.toList());
		return paraDominio(gravado);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Agendamento> buscarPorId(UUID id) {
		return repositorio.findById(id).map(AgendamentoJpaAdapter::paraDominio);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Intervalo> ocupacoesDoProfissional(UUID profissionalId, Intervalo periodo) {
		return repositorio.buscarQueCruzam(profissionalId, STATUS_QUE_OCUPAM, periodo.inicio(), periodo.fim())
				.stream()
				.map(entidade -> new Intervalo(entidade.getInicio(), entidade.getFim()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Agendamento> listarDoCliente(UUID clienteId) {
		return repositorio.findByClienteIdOrderByInicioDesc(clienteId).stream()
				.map(AgendamentoJpaAdapter::paraDominio)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Agendamento> listarDoEstabelecimento(FiltroDaAgenda filtro) {
		return repositorio.findAll(AgendamentoEspecificacoes.da(filtro), ORDEM_DE_INICIO).stream()
				.map(AgendamentoJpaAdapter::paraDominio)
				.toList();
	}

	static boolean violouExclusao(Throwable excecao) {
		for (Throwable causa = excecao; causa != null; causa = causa.getCause()) {
			if (causa instanceof SQLException sql && VIOLACAO_EXCLUSAO.equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}

	private static AgendamentoJpaEntity paraEntidade(Agendamento origem) {
		AgendamentoJpaEntity entidade = new AgendamentoJpaEntity();
		entidade.setId(origem.id());
		entidade.setClienteId(origem.clienteId());
		entidade.setEstabelecimentoId(origem.estabelecimentoId());
		entidade.setProfissionalId(origem.profissionalId());
		entidade.setServicoId(origem.servicoId());
		entidade.setInicio(origem.inicio());
		entidade.setFim(origem.fim());
		entidade.setStatus(origem.status());
		entidade.setPreco(origem.preco());
		entidade.setObservacoes(origem.observacoes());
		entidade.setVersao(origem.versao());
		return entidade;
	}

	private static AgendamentoHistoricoJpaEntity paraHistorico(UUID agendamentoId, AlteracaoAgendamento alteracao) {
		AgendamentoHistoricoJpaEntity entidade = new AgendamentoHistoricoJpaEntity();
		entidade.setId(UUID.randomUUID());
		entidade.setAgendamentoId(agendamentoId);
		entidade.setStatusAnterior(alteracao.statusAnterior());
		entidade.setStatusNovo(alteracao.statusNovo());
		entidade.setInicioAnterior(alteracao.inicioAnterior());
		entidade.setInicioNovo(alteracao.inicioNovo());
		entidade.setMotivo(alteracao.motivo());
		entidade.setAlteradoPor(alteracao.alteradoPor());
		entidade.setOcorridoEm(alteracao.ocorridoEm());
		return entidade;
	}

	private static Agendamento paraDominio(AgendamentoJpaEntity entidade) {
		return Agendamento.reconstituir(
				entidade.getId(),
				entidade.getClienteId(),
				entidade.getEstabelecimentoId(),
				entidade.getProfissionalId(),
				entidade.getServicoId(),
				entidade.getInicio(),
				entidade.getFim(),
				entidade.getStatus(),
				entidade.getPreco(),
				entidade.getObservacoes(),
				entidade.getVersao());
	}

}
