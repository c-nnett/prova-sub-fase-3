package br.com.fiap.agendabeleza.api.calendario.application.usecase;

import java.time.Clock;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.calendario.application.port.AssinaturaCalendarioRepository;
import br.com.fiap.agendabeleza.api.calendario.domain.AssinaturaDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.AssinaturaEmitida;
import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.TokenDoCalendario;

/**
 * Gera a URL secreta do feed de um profissional ou de um cliente. Cada chamada troca o token: a URL anterior deixa
 * de abrir, o que serve para revogar um link vazado.
 */
public class EmitirAssinaturaDoCalendarioUseCase {

	private final TitularesDoCalendario titulares;
	private final AssinaturaCalendarioRepository assinaturas;
	private final Clock relogio;

	public EmitirAssinaturaDoCalendarioUseCase(TitularesDoCalendario titulares,
			AssinaturaCalendarioRepository assinaturas, Clock relogio) {
		this.titulares = titulares;
		this.assinaturas = assinaturas;
		this.relogio = relogio;
	}

	/** @return o token em claro, que só aparece aqui; o banco guarda o hash */
	public TokenDoCalendario executar(Comando comando) {
		titulares.doPapel(comando.papel()).exigirAcesso(comando.titularId(), comando.usuarioId());
		AssinaturaEmitida emitida = AssinaturaDoCalendario.emitir(comando.papel(), comando.titularId(),
				relogio.instant());
		assinaturas.salvar(emitida.assinatura());
		return emitida.token();
	}

	/**
	 * @param titularId id do profissional ou, para o cliente, o próprio usuário
	 * @param usuarioId quem pede a assinatura ({@code X-Usuario-Id})
	 */
	public record Comando(PapelNoCalendario papel, UUID titularId, UUID usuarioId) {
	}

}
