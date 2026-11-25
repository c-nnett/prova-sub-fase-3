package br.com.fiap.agendabeleza.api.notificacoes.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoObrigatorio;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes;

/**
 * E-mail em texto simples pronto para envio. O assunto segue o tamanho da coluna {@code notificacao.assunto}
 * (modelagem, seção 3.6).
 *
 * @param destinatario e-mail de quem recebe
 * @param assunto      linha de assunto
 * @param corpo        texto do e-mail
 */
public record Mensagem(String destinatario, String assunto, String corpo) {

	public static final int TAMANHO_MAXIMO_ASSUNTO = 200;

	/** A coluna {@code corpo} é {@code text}; o limite só evita mensagens descontroladas. */
	public static final int TAMANHO_MAXIMO_CORPO = 10_000;

	public Mensagem {
		destinatario = Validacoes.email(destinatario, "destinatario");
		assunto = textoObrigatorio(assunto, "assunto", TAMANHO_MAXIMO_ASSUNTO);
		corpo = textoObrigatorio(corpo, "corpo", TAMANHO_MAXIMO_CORPO);
	}

}
