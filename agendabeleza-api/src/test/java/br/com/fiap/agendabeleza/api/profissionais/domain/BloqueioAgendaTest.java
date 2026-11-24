package br.com.fiap.agendabeleza.api.profissionais.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class BloqueioAgendaTest {

	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final Instant AGORA = Instant.parse("2026-11-29T12:00:00Z");
	private static final Instant INICIO = Instant.parse("2026-11-30T13:00:00Z");
	private static final Instant FIM = Instant.parse("2026-11-30T14:00:00Z");

	@Test
	void criaBloqueioFuturo() {
		BloqueioAgenda bloqueio = BloqueioAgenda.criar(PROFISSIONAL_ID, INICIO, FIM, "  Consulta médica  ", AGORA);

		assertThat(bloqueio.id()).isNotNull();
		assertThat(bloqueio.profissionalId()).isEqualTo(PROFISSIONAL_ID);
		assertThat(bloqueio.periodo()).isEqualTo(new Intervalo(INICIO, FIM));
		assertThat(bloqueio.motivo()).isEqualTo("Consulta médica");
		assertThat(bloqueio.pertenceA(PROFISSIONAL_ID)).isTrue();
		assertThat(bloqueio.pertenceA(UUID.randomUUID())).isFalse();
	}

	@Test
	void aceitaBloqueioEmAndamentoESemMotivo() {
		BloqueioAgenda bloqueio = BloqueioAgenda.criar(PROFISSIONAL_ID, AGORA.minusSeconds(3600),
				AGORA.plusSeconds(3600), "  ", AGORA);

		assertThat(bloqueio.motivo()).isNull();
	}

	@Test
	void recusaBloqueioJaEncerrado() {
		assertThatThrownBy(() -> BloqueioAgenda.criar(PROFISSIONAL_ID, AGORA.minusSeconds(3600), AGORA, null, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("O bloqueio já terminou");
	}

	@Test
	void recusaFimAntesDoInicio() {
		assertThatThrownBy(() -> BloqueioAgenda.criar(PROFISSIONAL_ID, FIM, INICIO, null, AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("fim deve ser posterior a inicio");
	}

	@Test
	void recusaMotivoLongo() {
		assertThatThrownBy(() -> BloqueioAgenda.criar(PROFISSIONAL_ID, INICIO, FIM, "x".repeat(151), AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("motivo deve ter no máximo 150 caracteres");
	}

	@Test
	void recusaCamposAusentes() {
		assertThatThrownBy(() -> BloqueioAgenda.criar(null, INICIO, FIM, null, AGORA))
				.hasMessage("profissionalId é obrigatório");
		assertThatThrownBy(() -> BloqueioAgenda.criar(PROFISSIONAL_ID, null, FIM, null, AGORA))
				.hasMessage("inicio é obrigatório");
		assertThatThrownBy(() -> BloqueioAgenda.criar(PROFISSIONAL_ID, INICIO, FIM, null, null))
				.hasMessage("agora é obrigatório");
	}

	@Test
	void reconstituiSemValidarSeJaTerminou() {
		UUID id = UUID.randomUUID();
		Instant passado = Instant.parse("2020-01-01T10:00:00Z");

		BloqueioAgenda bloqueio = BloqueioAgenda.reconstituir(id, PROFISSIONAL_ID, passado, passado.plusSeconds(60),
				"Férias");

		assertThat(bloqueio.id()).isEqualTo(id);
		assertThat(bloqueio.periodo().inicio()).isEqualTo(passado);
		assertThat(bloqueio.motivo()).isEqualTo("Férias");
	}

}
