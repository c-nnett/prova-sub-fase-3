package br.com.fiap.agendabeleza.api.avaliacoes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class AvaliacaoTest {

	private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID ESTABELECIMENTO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
	private static final Instant INICIO = Instant.parse("2030-03-04T13:00:00Z");
	private static final Instant AGORA = INICIO.plus(Duration.ofHours(3));

	private static Agendamento agendamentoCom(StatusAgendamento status) {
		return Agendamento.reconstituir(UUID.randomUUID(), CLIENTE_ID, ESTABELECIMENTO_ID, PROFISSIONAL_ID,
				SERVICO_ID, INICIO, INICIO.plus(Duration.ofMinutes(60)), status, new BigDecimal("95.00"), null, 2L);
	}

	private static Avaliacao avaliar(Agendamento agendamento, Integer notaEstabelecimento, Integer notaProfissional,
			String comentario) {
		return Avaliacao.registrar(agendamento, notaEstabelecimento, notaProfissional, comentario, AGORA);
	}

	@Test
	void registraAAvaliacaoDeUmAtendimentoConcluidoComOsDadosDoAgendamento() {
		Agendamento concluido = agendamentoCom(StatusAgendamento.CONCLUIDO);

		Avaliacao avaliacao = avaliar(concluido, 5, 4, "  Ótimo atendimento  ");

		assertThat(avaliacao.id()).isNotNull();
		assertThat(avaliacao.agendamentoId()).isEqualTo(concluido.id());
		assertThat(avaliacao.clienteId()).isEqualTo(CLIENTE_ID);
		assertThat(avaliacao.estabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
		assertThat(avaliacao.profissionalId()).isEqualTo(PROFISSIONAL_ID);
		assertThat(avaliacao.notaEstabelecimento()).isEqualTo(5);
		assertThat(avaliacao.notaProfissional()).isEqualTo(4);
		assertThat(avaliacao.comentario()).isEqualTo("Ótimo atendimento");
		assertThat(avaliacao.criadaEm()).isEqualTo(AGORA);
	}

	@Test
	void comentarioEmBrancoViraNulo() {
		assertThat(avaliar(agendamentoCom(StatusAgendamento.CONCLUIDO), 3, 3, "   ").comentario()).isNull();
	}

	@Test
	void recusaComentarioLongoDemais() {
		String comentarioLongo = "a".repeat(Avaliacao.TAMANHO_MAXIMO_COMENTARIO + 1);

		assertThatThrownBy(() -> avaliar(agendamentoCom(StatusAgendamento.CONCLUIDO), 3, 3, comentarioLongo))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("comentario deve ter no máximo 1000 caracteres");
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 6, -1 })
	void recusaNotaDoEstabelecimentoForaDaEscala(int nota) {
		assertThatThrownBy(() -> avaliar(agendamentoCom(StatusAgendamento.CONCLUIDO), nota, 3, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("notaEstabelecimento deve ser de 1 a 5");
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 6 })
	void recusaNotaDoProfissionalForaDaEscala(int nota) {
		assertThatThrownBy(() -> avaliar(agendamentoCom(StatusAgendamento.CONCLUIDO), 3, nota, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("notaProfissional deve ser de 1 a 5");
	}

	@Test
	void aceitaAsNotasDosExtremosDaEscala() {
		Avaliacao avaliacao = avaliar(agendamentoCom(StatusAgendamento.CONCLUIDO), 1, 5, null);

		assertThat(avaliacao.notaEstabelecimento()).isEqualTo(1);
		assertThat(avaliacao.notaProfissional()).isEqualTo(5);
	}

	@Test
	void exigeAsDuasNotas() {
		Agendamento concluido = agendamentoCom(StatusAgendamento.CONCLUIDO);

		assertThatThrownBy(() -> avaliar(concluido, null, 3, null))
				.hasMessage("notaEstabelecimento é obrigatório");
		assertThatThrownBy(() -> avaliar(concluido, 3, null, null))
				.hasMessage("notaProfissional é obrigatório");
	}

	@ParameterizedTest
	@EnumSource(value = StatusAgendamento.class, names = "CONCLUIDO", mode = EnumSource.Mode.EXCLUDE)
	void recusaAgendamentoQueNaoFoiConcluido(StatusAgendamento status) {
		assertThatThrownBy(() -> avaliar(agendamentoCom(status), 5, 5, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(Avaliacao.AGENDAMENTO_NAO_CONCLUIDO);
	}

	@Test
	void reconstituiComOsDadosGravados() {
		UUID id = UUID.randomUUID();
		UUID agendamentoId = UUID.randomUUID();

		Avaliacao avaliacao = Avaliacao.reconstituir(id, agendamentoId, CLIENTE_ID, ESTABELECIMENTO_ID,
				PROFISSIONAL_ID, 2, 3, "Demorou", AGORA);

		assertThat(avaliacao.id()).isEqualTo(id);
		assertThat(avaliacao.agendamentoId()).isEqualTo(agendamentoId);
		assertThat(avaliacao.notaEstabelecimento()).isEqualTo(2);
		assertThat(avaliacao.notaProfissional()).isEqualTo(3);
		assertThat(avaliacao.comentario()).isEqualTo("Demorou");
		assertThat(avaliacao.criadaEm()).isEqualTo(AGORA);
	}

}
