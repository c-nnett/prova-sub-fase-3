package br.com.fiap.agendabeleza.api.calendario.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import br.com.fiap.agendabeleza.api.TestcontainersConfiguration;
import br.com.fiap.agendabeleza.api.calendario.application.port.AssinaturaCalendarioRepository;
import br.com.fiap.agendabeleza.api.calendario.domain.AssinaturaDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.AssinaturaEmitida;
import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;

/**
 * Persistência real no PostgreSQL do Testcontainers, com as restrições únicas de titular e de hash.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AssinaturaCalendarioPersistenciaTest {

	private static final Instant AGORA = Instant.parse("2030-03-04T12:00:00Z");

	@Autowired
	private AssinaturaCalendarioRepository assinaturas;

	private static AssinaturaEmitida emitir(PapelNoCalendario papel, UUID titularId, Instant momento) {
		return AssinaturaDoCalendario.emitir(papel, titularId, momento);
	}

	@Test
	void gravaEEncontraPeloHashDoToken() {
		UUID profissionalId = UUID.randomUUID();
		AssinaturaEmitida emitida = emitir(PapelNoCalendario.PROFISSIONAL, profissionalId, AGORA);

		assinaturas.salvar(emitida.assinatura());

		AssinaturaDoCalendario lida = assinaturas.buscarPorTokenHash(emitida.token().hash()).orElseThrow();
		assertThat(lida.id()).isEqualTo(emitida.assinatura().id());
		assertThat(lida.papel()).isEqualTo(PapelNoCalendario.PROFISSIONAL);
		assertThat(lida.titularId()).isEqualTo(profissionalId);
		assertThat(lida.tokenHash()).isEqualTo(emitida.token().hash());
		assertThat(lida.criadaEm()).isEqualTo(AGORA);
	}

	@Test
	void hashDesconhecidoRetornaVazio() {
		assertThat(assinaturas.buscarPorTokenHash("0".repeat(64))).isEmpty();
	}

	@Test
	void novaAssinaturaDoMesmoTitularSubstituiAAnterior() {
		UUID clienteId = UUID.randomUUID();
		AssinaturaEmitida anterior = emitir(PapelNoCalendario.CLIENTE, clienteId, AGORA);
		AssinaturaEmitida nova = emitir(PapelNoCalendario.CLIENTE, clienteId, AGORA.plus(1, ChronoUnit.DAYS));
		assinaturas.salvar(anterior.assinatura());

		assinaturas.salvar(nova.assinatura());

		assertThat(assinaturas.buscarPorTokenHash(anterior.token().hash())).isEmpty();
		AssinaturaDoCalendario lida = assinaturas.buscarPorTokenHash(nova.token().hash()).orElseThrow();
		assertThat(lida.criadaEm()).isEqualTo(AGORA.plus(1, ChronoUnit.DAYS));
	}

	@Test
	void mesmoIdEmPapeisDiferentesTemAssinaturasSeparadas() {
		UUID titularId = UUID.randomUUID();
		AssinaturaEmitida doCliente = emitir(PapelNoCalendario.CLIENTE, titularId, AGORA);
		AssinaturaEmitida doProfissional = emitir(PapelNoCalendario.PROFISSIONAL, titularId, AGORA);

		assinaturas.salvar(doCliente.assinatura());
		assinaturas.salvar(doProfissional.assinatura());

		assertThat(assinaturas.buscarPorTokenHash(doCliente.token().hash())).isPresent();
		assertThat(assinaturas.buscarPorTokenHash(doProfissional.token().hash())).isPresent();
	}

}
