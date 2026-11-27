package br.com.fiap.agendabeleza.api.calendario.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;

import java.time.Instant;
import java.util.UUID;

/**
 * Direito de ler o feed de um titular (profissional ou cliente). Cada titular tem no máximo uma assinatura: emitir
 * outra troca o token, e a URL antiga deixa de funcionar. É assim que um link vazado é revogado.
 */
public class AssinaturaDoCalendario {

	private final UUID id;
	private final PapelNoCalendario papel;
	private final UUID titularId;
	private final String tokenHash;
	private final Instant criadaEm;

	private AssinaturaDoCalendario(UUID id, PapelNoCalendario papel, UUID titularId, String tokenHash,
			Instant criadaEm) {
		this.id = obrigatorio(id, "id");
		this.papel = obrigatorio(papel, "papel");
		this.titularId = obrigatorio(titularId, "titularId");
		this.tokenHash = obrigatorio(tokenHash, "tokenHash");
		this.criadaEm = obrigatorio(criadaEm, "criadaEm");
	}

	/** Gera um token novo para o titular. O token em claro não fica no agregado, só o hash. */
	public static AssinaturaEmitida emitir(PapelNoCalendario papel, UUID titularId, Instant agora) {
		TokenDoCalendario token = TokenDoCalendario.gerar();
		AssinaturaDoCalendario assinatura = new AssinaturaDoCalendario(UUID.randomUUID(), papel, titularId,
				token.hash(), agora);
		return new AssinaturaEmitida(assinatura, token);
	}

	public static AssinaturaDoCalendario reconstituir(UUID id, PapelNoCalendario papel, UUID titularId,
			String tokenHash, Instant criadaEm) {
		return new AssinaturaDoCalendario(id, papel, titularId, tokenHash, criadaEm);
	}

	public UUID id() {
		return id;
	}

	public PapelNoCalendario papel() {
		return papel;
	}

	public UUID titularId() {
		return titularId;
	}

	public String tokenHash() {
		return tokenHash;
	}

	public Instant criadaEm() {
		return criadaEm;
	}

}
