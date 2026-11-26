package br.com.fiap.agendabeleza.api.agendamentos.application.port;

/**
 * Avisa cliente e profissional sobre a reserva, o cancelamento e o reagendamento (funcionalidade 3). Implementada
 * pelo módulo {@code notificacoes}, que grava os e-mails na outbox: por isso deve ser chamada dentro da mesma
 * transação da alteração, e o envio acontece depois do commit (arquitetura, seção 6.2).
 */
public interface AvisosDoAgendamento {

	/** @param emailCliente destino dos avisos do cliente, informado na reserva e reaproveitado nos seguintes */
	void reservado(AvisoDoAgendamento aviso, String emailCliente);

	void cancelado(AvisoDoAgendamento aviso);

	/** @param aviso dados do agendamento já com o novo início */
	void reagendado(AvisoDoAgendamento aviso);

}
