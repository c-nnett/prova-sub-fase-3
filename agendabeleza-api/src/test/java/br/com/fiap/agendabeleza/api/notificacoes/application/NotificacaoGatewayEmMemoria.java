package br.com.fiap.agendabeleza.api.notificacoes.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoGateway;
import br.com.fiap.agendabeleza.api.notificacoes.domain.FalhaNoEnvioException;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Mensagem;

/** Guarda as mensagens enviadas e recusa os destinatários marcados, como um servidor de e-mail fora do ar. */
public class NotificacaoGatewayEmMemoria implements NotificacaoGateway {

	public static final String ERRO_DO_SERVIDOR = "Servidor de e-mail indisponível";

	private final List<Mensagem> enviadas = new ArrayList<>();
	private final Set<String> destinatariosRecusados = new HashSet<>();

	@Override
	public void enviar(Mensagem mensagem) {
		if (destinatariosRecusados.contains(mensagem.destinatario())) {
			throw new FalhaNoEnvioException(mensagem.destinatario(), new IllegalStateException(ERRO_DO_SERVIDOR));
		}
		enviadas.add(mensagem);
	}

	public void recusar(String destinatario) {
		destinatariosRecusados.add(destinatario);
	}

	public List<Mensagem> enviadas() {
		return List.copyOf(enviadas);
	}

}
