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
 * Implementa {@link AvisosDoAgendamento} gravando os e-mails na outbox, um para o cliente e outro para o
 * profissional, para envio imediato pelo job. Nada é enviado aqui: a gravação acompanha a transação de quem chama, e
 * um servidor de e-mail fora do ar não desfaz a reserva.
 * <p>
 * O e-mail do cliente só chega na reserva. Nos avisos seguintes ele vem da outbox; se não houver, como em
 * agendamentos anteriores às notificações, só o profissional é avisado.
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
		registrar(TipoNotificacao.CONFIRMACAO, aviso, List.of(emailCliente));
	}

	@Override
	public void cancelado(AvisoDoAgendamento aviso) {
		registrar(TipoNotificacao.CANCELAMENTO, aviso, emailDoClienteRegistrado(aviso));
	}

	@Override
	public void reagendado(AvisoDoAgendamento aviso) {
		registrar(TipoNotificacao.REAGENDAMENTO, aviso, emailDoClienteRegistrado(aviso));
	}

	/** Vazio quando a reserva não deixou e-mail do cliente na outbox. */
	private List<String> emailDoClienteRegistrado(AvisoDoAgendamento aviso) {
		return notificacoes.emailDoCliente(aviso.agendamentoId()).stream().toList();
	}

	/**
	 * Fluxo comum dos três avisos. As mensagens são montadas antes de gravar, então um e-mail inválido não deixa
	 * nada pela metade.
	 *
	 * @param emailsDoCliente zero ou um endereço
	 */
	private void registrar(TipoNotificacao tipo, AvisoDoAgendamento aviso, List<String> emailsDoCliente) {
		HorarioNotificado horario = new HorarioNotificado(aviso.nomeEstabelecimento(), aviso.nomeProfissional(),
				aviso.inicio(), aviso.fuso());
		Instant agora = relogio.instant();
		List<Notificacao> novas = new ArrayList<>();
		emailsDoCliente.forEach(email -> novas.add(Notificacao.pendente(aviso.agendamentoId(), tipo,
				PapelDestinatario.CLIENTE, tipo.mensagemPara(email, horario), agora)));
		novas.add(Notificacao.pendente(aviso.agendamentoId(), tipo, PapelDestinatario.PROFISSIONAL,
				tipo.mensagemPara(aviso.emailProfissional(), horario), agora));
		notificacoes.salvarTodas(novas);
	}

}
