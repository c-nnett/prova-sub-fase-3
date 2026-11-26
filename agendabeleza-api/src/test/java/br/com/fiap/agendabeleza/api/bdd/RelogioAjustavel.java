package br.com.fiap.agendabeleza.api.bdd;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Relógio dos cenários BDD. Segue o relógio real até um passo fixar o "agora", por exemplo para concluir um
 * atendimento que só pode ser concluído depois do início. Cada cenário volta ao relógio real no fim.
 */
public class RelogioAjustavel extends Clock {

	private volatile Instant agoraFixado;

	public void definirAgora(Instant agora) {
		this.agoraFixado = agora;
	}

	public void voltarAoRelogioReal() {
		this.agoraFixado = null;
	}

	@Override
	public Instant instant() {
		Instant fixado = agoraFixado;
		return fixado == null ? Instant.now() : fixado;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	/** Devolve um relógio no fuso pedido com o instante deste momento, suficiente para os usos de uma chamada. */
	@Override
	public Clock withZone(ZoneId fuso) {
		return Clock.fixed(instant(), fuso);
	}

}
