package br.com.fiap.agendabeleza.api.notificacoes.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoRepository;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Mensagem;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Notificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.StatusNotificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.TipoNotificacao;

/**
 * Implementa {@link NotificacaoRepository} com JPA. {@link #salvarTodas} entra na transação de quem chama, que é a
 * da alteração do agendamento.
 */
@Component
class NotificacaoJpaAdapter implements NotificacaoRepository {

	private final NotificacaoJpaRepository repositorio;

	NotificacaoJpaAdapter(NotificacaoJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional
	public void salvarTodas(List<Notificacao> notificacoes) {
		repositorio.saveAll(notificacoes.stream().map(NotificacaoJpaAdapter::paraEntidade).toList());
	}

	@Override
	@Transactional
	public void atualizar(Notificacao notificacao) {
		repositorio.save(paraEntidade(notificacao));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notificacao> pendentesAte(Instant momento, int limite) {
		return repositorio.findByStatusAndEnviarEmLessThanEqualOrderByEnviarEm(StatusNotificacao.PENDENTE, momento,
				Limit.of(limite)).stream()
				.map(NotificacaoJpaAdapter::paraDominio)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notificacao> pendentesDoAgendamento(UUID agendamentoId, TipoNotificacao tipo) {
		return repositorio.findByAgendamentoIdAndTipoAndStatus(agendamentoId, tipo, StatusNotificacao.PENDENTE)
				.stream()
				.map(NotificacaoJpaAdapter::paraDominio)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<String> emailDoCliente(UUID agendamentoId) {
		return repositorio.findFirstByAgendamentoIdAndPapelOrderByEnviarEm(agendamentoId, PapelDestinatario.CLIENTE)
				.map(NotificacaoJpaEntity::getDestinatarioEmail);
	}

	private static NotificacaoJpaEntity paraEntidade(Notificacao origem) {
		NotificacaoJpaEntity entidade = new NotificacaoJpaEntity();
		entidade.setId(origem.id());
		entidade.setAgendamentoId(origem.agendamentoId());
		entidade.setTipo(origem.tipo());
		entidade.setPapel(origem.papel());
		entidade.setDestinatarioEmail(origem.mensagem().destinatario());
		entidade.setAssunto(origem.mensagem().assunto());
		entidade.setCorpo(origem.mensagem().corpo());
		entidade.setEnviarEm(origem.enviarEm());
		entidade.setStatus(origem.status());
		entidade.setTentativas(origem.tentativas());
		entidade.setEnviadaEm(origem.enviadaEm());
		entidade.setUltimoErro(origem.ultimoErro());
		return entidade;
	}

	private static Notificacao paraDominio(NotificacaoJpaEntity entidade) {
		return Notificacao.reconstituir(
				entidade.getId(),
				entidade.getAgendamentoId(),
				entidade.getTipo(),
				entidade.getPapel(),
				new Mensagem(entidade.getDestinatarioEmail(), entidade.getAssunto(), entidade.getCorpo()),
				entidade.getEnviarEm(),
				entidade.getStatus(),
				entidade.getTentativas(),
				entidade.getEnviadaEm(),
				entidade.getUltimoErro());
	}

}
