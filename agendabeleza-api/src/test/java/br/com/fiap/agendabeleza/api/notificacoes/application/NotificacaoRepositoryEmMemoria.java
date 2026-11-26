package br.com.fiap.agendabeleza.api.notificacoes.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoRepository;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Mensagem;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Notificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.StatusNotificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.TipoNotificacao;

/** Imita o adaptador JPA: pendentes vencidas em ordem de envio, e o e-mail do cliente pela primeira notificação. */
public class NotificacaoRepositoryEmMemoria implements NotificacaoRepository {

	private static final Comparator<Notificacao> ORDEM_DE_ENVIO = Comparator.comparing(Notificacao::enviarEm);

	private final Map<UUID, Notificacao> dados = new LinkedHashMap<>();

	@Override
	public void salvarTodas(List<Notificacao> notificacoes) {
		notificacoes.forEach(this::atualizar);
	}

	@Override
	public void atualizar(Notificacao notificacao) {
		dados.put(notificacao.id(), notificacao);
	}

	@Override
	public List<Notificacao> pendentesAte(Instant momento, int limite) {
		return dados.values().stream()
				.filter(notificacao -> notificacao.status() == StatusNotificacao.PENDENTE)
				.filter(notificacao -> !notificacao.enviarEm().isAfter(momento))
				.sorted(ORDEM_DE_ENVIO)
				.limit(limite)
				.toList();
	}

	@Override
	public List<Notificacao> pendentesDoAgendamento(UUID agendamentoId, TipoNotificacao tipo) {
		return dados.values().stream()
				.filter(notificacao -> notificacao.agendamentoId().equals(agendamentoId))
				.filter(notificacao -> notificacao.tipo() == tipo)
				.filter(notificacao -> notificacao.status() == StatusNotificacao.PENDENTE)
				.toList();
	}

	@Override
	public Optional<String> emailDoCliente(UUID agendamentoId) {
		return dados.values().stream()
				.filter(notificacao -> notificacao.agendamentoId().equals(agendamentoId))
				.filter(notificacao -> notificacao.papel() == PapelDestinatario.CLIENTE)
				.min(ORDEM_DE_ENVIO)
				.map(Notificacao::mensagem)
				.map(Mensagem::destinatario);
	}

	public List<Notificacao> todas() {
		return List.copyOf(dados.values());
	}

}
