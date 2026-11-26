package br.com.fiap.agendabeleza.api.notificacoes.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisoDoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisosDoAgendamento;
import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoRepository;
import br.com.fiap.agendabeleza.api.notificacoes.domain.HorarioNotificado;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Notificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.TipoNotificacao;

/**
 * Implementa {@link AvisosDoAgendamento} gravando os e-mails na outbox, sempre um para o cliente e outro para o
 * profissional. Nada é enviado aqui: a gravação acompanha a transação de quem chama, e o job envia depois.
 * <ul>
 * <li>Reserva: confirmação imediata e lembrete {@link Notificacao#ANTECEDENCIA_DO_LEMBRETE} antes do início.</li>
 * <li>Cancelamento: os lembretes pendentes são cancelados, e o aviso de cancelamento sai imediatamente.</li>
 * <li>Reagendamento: os lembretes pendentes do horário antigo são cancelados, e o aviso e os lembretes do novo
 * horário são gravados.</li>
 * </ul>
 * O e-mail do cliente só chega na reserva. Nos avisos seguintes ele vem da outbox; se não houver, como em
 * agendamentos anteriores às notificações, só o profissional é avisado. As mensagens novas são montadas antes de
 * qualquer gravação, então um e-mail inválido não deixa nada pela metade.
 */
public class AvisosPorEmail implements AvisosDoAgendamento {

	private final NotificacaoRepository notificacoes;
	private final Clock relogio;

	public AvisosPorEmail(NotificacaoRepository notificacoes, Clock relogio) {
		this.notificacoes = notificacoes;
		this.relogio = relogio;
	}

	@Override
	public void reservado(AvisoDoAgendamento aviso, String emailCliente) {
		Instant agora = relogio.instant();
		List<String> emailsDoCliente = List.of(emailCliente);
		List<Notificacao> novas = new ArrayList<>(
				paraOsDestinatarios(TipoNotificacao.CONFIRMACAO, aviso, emailsDoCliente, agora));
		novas.addAll(lembretes(aviso, emailsDoCliente, agora));
		notificacoes.salvarTodas(novas);
	}

	@Override
	public void cancelado(AvisoDoAgendamento aviso) {
		List<Notificacao> novas = paraOsDestinatarios(TipoNotificacao.CANCELAMENTO, aviso,
				emailDoClienteRegistrado(aviso), relogio.instant());
		cancelarLembretesPendentes(aviso);
		notificacoes.salvarTodas(novas);
	}

	@Override
	public void reagendado(AvisoDoAgendamento aviso) {
		Instant agora = relogio.instant();
		List<String> emailsDoCliente = emailDoClienteRegistrado(aviso);
		List<Notificacao> novas = new ArrayList<>(
				paraOsDestinatarios(TipoNotificacao.REAGENDAMENTO, aviso, emailsDoCliente, agora));
		novas.addAll(lembretes(aviso, emailsDoCliente, agora));
		cancelarLembretesPendentes(aviso);
		notificacoes.salvarTodas(novas);
	}

	/** Vazio quando a reserva não deixou e-mail do cliente na outbox. */
	private List<String> emailDoClienteRegistrado(AvisoDoAgendamento aviso) {
		return notificacoes.emailDoCliente(aviso.agendamentoId()).stream().toList();
	}

	/** Nenhum lembrete se a antecedência já passou: a confirmação acabou de sair. */
	private List<Notificacao> lembretes(AvisoDoAgendamento aviso, List<String> emailsDoCliente, Instant agora) {
		Instant enviarEm = aviso.inicio().minus(Notificacao.ANTECEDENCIA_DO_LEMBRETE);
		if (!enviarEm.isAfter(agora)) {
			return List.of();
		}
		return paraOsDestinatarios(TipoNotificacao.LEMBRETE, aviso, emailsDoCliente, enviarEm);
	}

	/** O lembrete que já saiu fica como está; só os pendentes deixam de valer. */
	private void cancelarLembretesPendentes(AvisoDoAgendamento aviso) {
		for (Notificacao lembrete : notificacoes.pendentesDoAgendamento(aviso.agendamentoId(),
				TipoNotificacao.LEMBRETE)) {
			lembrete.cancelar();
			notificacoes.atualizar(lembrete);
		}
	}

	/**
	 * Fluxo comum de todos os avisos: uma notificação por destinatário, com o mesmo texto.
	 *
	 * @param emailsDoCliente zero ou um endereço
	 */
	private static List<Notificacao> paraOsDestinatarios(TipoNotificacao tipo, AvisoDoAgendamento aviso,
			List<String> emailsDoCliente, Instant enviarEm) {
		HorarioNotificado horario = new HorarioNotificado(aviso.nomeEstabelecimento(), aviso.nomeProfissional(),
				aviso.inicio(), aviso.fuso());
		List<Notificacao> novas = new ArrayList<>();
		emailsDoCliente.forEach(email -> novas.add(Notificacao.pendente(aviso.agendamentoId(), tipo,
				PapelDestinatario.CLIENTE, tipo.mensagemPara(email, horario), enviarEm)));
		novas.add(Notificacao.pendente(aviso.agendamentoId(), tipo, PapelDestinatario.PROFISSIONAL,
				tipo.mensagemPara(aviso.emailProfissional(), horario), enviarEm));
		return novas;
	}

}
