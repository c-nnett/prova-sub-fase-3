package br.com.fiap.agendabeleza.api.calendario.application.usecase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import br.com.fiap.agendabeleza.api.calendario.application.port.AssinaturaCalendarioRepository;
import br.com.fiap.agendabeleza.api.calendario.application.port.CalendarioGateway;
import br.com.fiap.agendabeleza.api.calendario.domain.AssinaturaDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.EventoDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.FeedDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.TokenDoCalendario;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * Monta o feed que o Google Calendar ou o Outlook buscam pela URL secreta. O token é a única credencial: formato
 * inválido, token desconhecido e token trocado respondem igual (404), sem dizer qual foi o caso.
 * <p>
 * Entram os agendamentos que terminam a partir de {@link #JANELA_DO_PASSADO} atrás, sem limite à frente. O passado
 * curto mantém o histórico recente na agenda sem deixar o feed crescer para sempre.
 */
public class GerarFeedDoCalendarioUseCase {

	public static final Duration JANELA_DO_PASSADO = Duration.ofDays(30);

	private final TitularesDoCalendario titulares;
	private final AssinaturaCalendarioRepository assinaturas;
	private final EstabelecimentoRepository estabelecimentos;
	private final ServicoRepository servicos;
	private final ProfissionalRepository profissionais;
	private final CalendarioGateway calendario;
	private final Clock relogio;

	public GerarFeedDoCalendarioUseCase(TitularesDoCalendario titulares, AssinaturaCalendarioRepository assinaturas,
			EstabelecimentoRepository estabelecimentos, ServicoRepository servicos,
			ProfissionalRepository profissionais, CalendarioGateway calendario, Clock relogio) {
		this.titulares = titulares;
		this.assinaturas = assinaturas;
		this.estabelecimentos = estabelecimentos;
		this.servicos = servicos;
		this.profissionais = profissionais;
		this.calendario = calendario;
		this.relogio = relogio;
	}

	/** @return o documento do feed, no formato do {@link CalendarioGateway} */
	public String executar(String token) {
		AssinaturaDoCalendario assinatura = assinaturas.buscarPorTokenHash(TokenDoCalendario.de(token).hash())
				.orElseThrow(() -> new RecursoNaoEncontradoException(TokenDoCalendario.CALENDARIO_NAO_ENCONTRADO));
		TitularDoCalendario titular = titulares.doPapel(assinatura.papel());
		Instant agora = relogio.instant();
		List<EventoDoCalendario> eventos = new MontagemDosEventos(titular, servicos, estabelecimentos, profissionais)
				.eventos(titular.agendamentosDesde(assinatura.titularId(), agora.minus(JANELA_DO_PASSADO)));
		return calendario.gerar(new FeedDoCalendario(titular.nomeDoCalendario(assinatura.titularId()), agora,
				eventos));
	}

}
