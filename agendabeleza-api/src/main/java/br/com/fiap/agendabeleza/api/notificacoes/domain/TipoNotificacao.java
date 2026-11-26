package br.com.fiap.agendabeleza.api.notificacoes.domain;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Motivo do e-mail. Cada tipo é a estratégia do próprio texto (Strategy): o assunto e o modelo do corpo, que
 * recebe, nesta ordem, o estabelecimento, o profissional, a data e a hora. O texto serve para o cliente e para o
 * profissional.
 */
public enum TipoNotificacao {

	CONFIRMACAO("Agendamento reservado", "O agendamento no %s com %s está reservado para %s às %s."),
	CANCELAMENTO("Agendamento cancelado", "O agendamento no %s com %s em %s às %s foi cancelado."),
	REAGENDAMENTO("Agendamento reagendado", "O agendamento no %s com %s foi reagendado para %s às %s."),
	/** Enviado {@link Notificacao#ANTECEDENCIA_DO_LEMBRETE} antes do início. */
	LEMBRETE("Lembrete de agendamento", "Lembrete: o agendamento no %s com %s é em %s às %s.");

	private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

	private final String assunto;
	private final String modeloDoCorpo;

	TipoNotificacao(String assunto, String modeloDoCorpo) {
		this.assunto = assunto;
		this.modeloDoCorpo = modeloDoCorpo;
	}

	/** Monta o e-mail com o início no fuso do estabelecimento. */
	public Mensagem mensagemPara(String destinatario, HorarioNotificado horario) {
		ZonedDateTime inicioLocal = horario.inicio().atZone(horario.fuso());
		String corpo = modeloDoCorpo.formatted(horario.nomeEstabelecimento(), horario.nomeProfissional(),
				DATA.format(inicioLocal), HORA.format(inicioLocal));
		return new Mensagem(destinatario, assunto, corpo);
	}

}
