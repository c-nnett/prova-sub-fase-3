package br.com.fiap.agendabeleza.api.calendario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class AssinaturaDoCalendarioTest {

	private static final Instant AGORA = Instant.parse("2030-03-04T12:00:00Z");

	@Test
	void emiteAssinaturaGuardandoSomenteOHashDoToken() {
		UUID profissionalId = UUID.randomUUID();

		AssinaturaEmitida emitida = AssinaturaDoCalendario.emitir(PapelNoCalendario.PROFISSIONAL, profissionalId,
				AGORA);

		AssinaturaDoCalendario assinatura = emitida.assinatura();
		assertThat(assinatura.id()).isNotNull();
		assertThat(assinatura.papel()).isEqualTo(PapelNoCalendario.PROFISSIONAL);
		assertThat(assinatura.titularId()).isEqualTo(profissionalId);
		assertThat(assinatura.criadaEm()).isEqualTo(AGORA);
		assertThat(assinatura.tokenHash()).isEqualTo(emitida.token().hash());
	}

	@Test
	void cadaEmissaoTemTokenProprio() {
		UUID clienteId = UUID.randomUUID();

		AssinaturaEmitida primeira = AssinaturaDoCalendario.emitir(PapelNoCalendario.CLIENTE, clienteId, AGORA);
		AssinaturaEmitida segunda = AssinaturaDoCalendario.emitir(PapelNoCalendario.CLIENTE, clienteId, AGORA);

		assertThat(primeira.token()).isNotEqualTo(segunda.token());
		assertThat(primeira.assinatura().tokenHash()).isNotEqualTo(segunda.assinatura().tokenHash());
	}

	@Test
	void recusaCamposObrigatoriosAusentes() {
		UUID titularId = UUID.randomUUID();

		assertThatThrownBy(() -> AssinaturaDoCalendario.emitir(null, titularId, AGORA))
				.isInstanceOf(RegraNegocioException.class).hasMessage("papel é obrigatório");
		assertThatThrownBy(() -> AssinaturaDoCalendario.emitir(PapelNoCalendario.CLIENTE, null, AGORA))
				.hasMessage("titularId é obrigatório");
		assertThatThrownBy(() -> AssinaturaDoCalendario.emitir(PapelNoCalendario.CLIENTE, titularId, null))
				.hasMessage("criadaEm é obrigatório");
	}

	@Test
	void reconstituiOsDadosGravados() {
		UUID id = UUID.randomUUID();
		UUID titularId = UUID.randomUUID();
		String tokenHash = TokenDoCalendario.gerar().hash();

		AssinaturaDoCalendario assinatura = AssinaturaDoCalendario.reconstituir(id, PapelNoCalendario.CLIENTE,
				titularId, tokenHash, AGORA);

		assertThat(assinatura.id()).isEqualTo(id);
		assertThat(assinatura.papel()).isEqualTo(PapelNoCalendario.CLIENTE);
		assertThat(assinatura.titularId()).isEqualTo(titularId);
		assertThat(assinatura.tokenHash()).isEqualTo(tokenHash);
		assertThat(assinatura.criadaEm()).isEqualTo(AGORA);
	}

}
