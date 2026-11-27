package br.com.fiap.agendabeleza.api.calendario.application.usecase;

import static br.com.fiap.agendabeleza.api.calendario.application.CenarioDoCalendario.AGORA;
import static br.com.fiap.agendabeleza.api.calendario.application.CenarioDoCalendario.CLIENTE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.calendario.application.CalendarioGatewayEmMemoria;
import br.com.fiap.agendabeleza.api.calendario.application.CenarioDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.DetalhesDoEvento;
import br.com.fiap.agendabeleza.api.calendario.domain.EventoDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.FeedDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.SituacaoDoEvento;
import br.com.fiap.agendabeleza.api.calendario.domain.TokenDoCalendario;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture;

class GerarFeedDoCalendarioUseCaseTest {

	private static final String LOCAL = "Rua Augusta, 100 - Consolação, São Paulo/SP";
	private static final String DESCRICAO = "Serviço: Corte feminino\nProfissional: Ana Souza\n"
			+ "Estabelecimento: Salão Bela Vista\nPreço: R$ 80,00";

	private final CenarioDoCalendario cenario = new CenarioDoCalendario();

	private String tokenDoProfissional() {
		return cenario.emitir.executar(new EmitirAssinaturaDoCalendarioUseCase.Comando(PapelNoCalendario.PROFISSIONAL,
				cenario.profissional.id(), ProfissionalFixture.USUARIO_PROFISSIONAL_ID)).valor();
	}

	private String tokenDoCliente() {
		return cenario.emitir.executar(new EmitirAssinaturaDoCalendarioUseCase.Comando(PapelNoCalendario.CLIENTE,
				CLIENTE_ID, CLIENTE_ID)).valor();
	}

	private FeedDoCalendario feedGerado(String token) {
		assertThat(cenario.gerarFeed.executar(token)).isEqualTo(CalendarioGatewayEmMemoria.CONTEUDO_GERADO);
		return cenario.calendario.ultimoFeed();
	}

	@Test
	void feedDoProfissionalTrazOsAtendimentosEmOrdemDeInicio() {
		Agendamento depois = cenario.agendado(Duration.ofDays(2), StatusAgendamento.CONFIRMADO);
		Agendamento antes = cenario.agendado(Duration.ofDays(1), StatusAgendamento.AGENDADO);

		FeedDoCalendario feed = feedGerado(tokenDoProfissional());

		assertThat(feed.nome()).isEqualTo("AgendaBeleza - Ana Souza");
		assertThat(feed.geradoEm()).isEqualTo(AGORA);
		assertThat(feed.eventos()).extracting(EventoDoCalendario::agendamentoId)
				.containsExactly(antes.id(), depois.id());
		assertThat(feed.eventos()).extracting(EventoDoCalendario::situacao)
				.containsExactly(SituacaoDoEvento.PROVISORIO, SituacaoDoEvento.CONFIRMADO);
		assertThat(feed.eventos().getFirst().detalhes())
				.isEqualTo(new DetalhesDoEvento("Corte feminino", LOCAL, DESCRICAO));
	}

	@Test
	void feedDoClienteMostraOEstabelecimentoNoTitulo() {
		cenario.agendado(Duration.ofDays(1), StatusAgendamento.AGENDADO);
		cenario.agendado(UUID.randomUUID(), Duration.ofDays(3), StatusAgendamento.AGENDADO);

		FeedDoCalendario feed = feedGerado(tokenDoCliente());

		assertThat(feed.nome()).isEqualTo(CalendarioDoCliente.NOME_DO_CALENDARIO);
		assertThat(feed.eventos()).singleElement().extracting(EventoDoCalendario::detalhes)
				.isEqualTo(new DetalhesDoEvento("Corte feminino em Salão Bela Vista", LOCAL, DESCRICAO));
	}

	@Test
	void canceladoSaiDoFeed() {
		cenario.agendado(Duration.ofDays(1), StatusAgendamento.CANCELADO);

		assertThat(feedGerado(tokenDoProfissional()).eventos()).isEmpty();
	}

	@Test
	void feedVaiAteJanelaDoPassado() {
		Duration dentroDaJanela = GerarFeedDoCalendarioUseCase.JANELA_DO_PASSADO.negated().plusHours(1);
		Duration foraDaJanela = GerarFeedDoCalendarioUseCase.JANELA_DO_PASSADO.negated().minusHours(1);
		Agendamento recente = cenario.agendado(dentroDaJanela, StatusAgendamento.CONCLUIDO);
		cenario.agendado(foraDaJanela, StatusAgendamento.CONCLUIDO);

		assertThat(feedGerado(tokenDoCliente()).eventos()).extracting(EventoDoCalendario::agendamentoId)
				.containsExactly(recente.id());
	}

	@Test
	void tokenMalformadoResponde404() {
		assertThatThrownBy(() -> cenario.gerarFeed.executar("nao-e-um-token"))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage(TokenDoCalendario.CALENDARIO_NAO_ENCONTRADO);
	}

	@Test
	void tokenSemAssinaturaResponde404() {
		String tokenDesconhecido = TokenDoCalendario.gerar().valor();

		assertThatThrownBy(() -> cenario.gerarFeed.executar(tokenDesconhecido))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage(TokenDoCalendario.CALENDARIO_NAO_ENCONTRADO);
	}

	@Test
	void tokenTrocadoDeixaDeAbrirOFeed() {
		String anterior = tokenDoCliente();
		String novo = tokenDoCliente();

		assertThatThrownBy(() -> cenario.gerarFeed.executar(anterior))
				.isInstanceOf(RecursoNaoEncontradoException.class);
		assertThat(feedGerado(novo).eventos()).isEmpty();
	}

}
