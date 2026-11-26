package br.com.fiap.agendabeleza.api.calendario.application.usecase;

import static br.com.fiap.agendabeleza.api.calendario.application.CenarioDoCalendario.AGORA;
import static br.com.fiap.agendabeleza.api.calendario.application.CenarioDoCalendario.CLIENTE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.calendario.application.CenarioDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.AssinaturaDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.TokenDoCalendario;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture;
import br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture;

class EmitirAssinaturaDoCalendarioUseCaseTest {

	private final CenarioDoCalendario cenario = new CenarioDoCalendario();

	private TokenDoCalendario emitirParaOProfissional(UUID usuarioId) {
		return cenario.emitir.executar(new EmitirAssinaturaDoCalendarioUseCase.Comando(PapelNoCalendario.PROFISSIONAL,
				cenario.profissional.id(), usuarioId));
	}

	private TokenDoCalendario emitirParaOCliente() {
		return cenario.emitir.executar(
				new EmitirAssinaturaDoCalendarioUseCase.Comando(PapelNoCalendario.CLIENTE, CLIENTE_ID, CLIENTE_ID));
	}

	@Test
	void proprioProfissionalAssinaAAgendaDele() {
		TokenDoCalendario token = emitirParaOProfissional(ProfissionalFixture.USUARIO_PROFISSIONAL_ID);

		assertThat(cenario.assinaturas.todas()).singleElement().satisfies(assinatura -> {
			assertThat(assinatura.papel()).isEqualTo(PapelNoCalendario.PROFISSIONAL);
			assertThat(assinatura.titularId()).isEqualTo(cenario.profissional.id());
			assertThat(assinatura.tokenHash()).isEqualTo(token.hash());
			assertThat(assinatura.criadaEm()).isEqualTo(AGORA);
		});
	}

	@Test
	void gestorDoEstabelecimentoAssinaAAgendaDoProfissional() {
		TokenDoCalendario token = emitirParaOProfissional(EstabelecimentoFixture.GESTOR_ID);

		assertThat(cenario.assinaturas.buscarPorTokenHash(token.hash())).isPresent();
	}

	@Test
	void outroUsuarioNaoAssinaAAgendaDoProfissional() {
		assertThatThrownBy(() -> emitirParaOProfissional(EstabelecimentoFixture.OUTRO_USUARIO_ID))
				.isInstanceOf(AcessoNegadoException.class)
				.hasMessage(CalendarioDoProfissional.SOMENTE_GESTOR_OU_PROFISSIONAL);
		assertThat(cenario.assinaturas.todas()).isEmpty();
	}

	@Test
	void profissionalInexistenteResponde404() {
		UUID profissionalInexistente = UUID.randomUUID();

		assertThatThrownBy(() -> cenario.emitir.executar(new EmitirAssinaturaDoCalendarioUseCase.Comando(
				PapelNoCalendario.PROFISSIONAL, profissionalInexistente, EstabelecimentoFixture.GESTOR_ID)))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage("Profissional " + profissionalInexistente + " não encontrado");
	}

	@Test
	void clienteAssinaOsProprioHorarios() {
		TokenDoCalendario token = emitirParaOCliente();

		AssinaturaDoCalendario assinatura = cenario.assinaturas.buscarPorTokenHash(token.hash()).orElseThrow();
		assertThat(assinatura.papel()).isEqualTo(PapelNoCalendario.CLIENTE);
		assertThat(assinatura.titularId()).isEqualTo(CLIENTE_ID);
	}

	@Test
	void clienteNaoAssinaOsHorariosDeOutroCliente() {
		assertThatThrownBy(() -> cenario.emitir.executar(new EmitirAssinaturaDoCalendarioUseCase.Comando(
				PapelNoCalendario.CLIENTE, CLIENTE_ID, EstabelecimentoFixture.OUTRO_USUARIO_ID)))
				.isInstanceOf(AcessoNegadoException.class)
				.hasMessage(CalendarioDoCliente.SOMENTE_O_PROPRIO_CLIENTE);
	}

	@Test
	void novaEmissaoTrocaOTokenEAnteriorDeixaDeValer() {
		TokenDoCalendario anterior = emitirParaOCliente();

		TokenDoCalendario novo = emitirParaOCliente();

		assertThat(novo).isNotEqualTo(anterior);
		assertThat(cenario.assinaturas.todas()).hasSize(1);
		assertThat(cenario.assinaturas.buscarPorTokenHash(anterior.hash())).isEmpty();
		assertThat(cenario.assinaturas.buscarPorTokenHash(novo.hash())).isPresent();
	}

	@Test
	void assinaturasDoClienteEDoProfissionalSaoIndependentes() {
		emitirParaOCliente();
		emitirParaOProfissional(ProfissionalFixture.USUARIO_PROFISSIONAL_ID);

		assertThat(cenario.assinaturas.todas()).hasSize(2);
	}

}
