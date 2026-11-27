package br.com.fiap.agendabeleza.api.calendario.application.port;

import br.com.fiap.agendabeleza.api.calendario.domain.FeedDoCalendario;

/**
 * Porta de saída que transforma a agenda em um formato que os calendários externos assinam (funcionalidade 7).
 * <p>
 * Substitui a sincronização por OAuth2 com a API do Google (ADR-07, revisto no planejamento, seção 2.1): o Google
 * Calendar e o Outlook buscam o feed sozinhos pela URL secreta. O núcleo não conhece o formato; o adaptador gera o
 * iCalendar (RFC 5545).
 */
public interface CalendarioGateway {

	/** @return o documento pronto para servir como resposta do feed */
	String gerar(FeedDoCalendario feed);

}
