package br.com.fiap.agendabeleza.api.agendamentos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ResumoDaAgendaTest {

	private static final UUID CLIENTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID ESTABELECIMENTO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
	private static final Instant INICIO = Instant.parse("2030-01-07T13:00:00Z");

	private static Agendamento comStatusEPreco(StatusAgendamento status, String preco) {
		return Agendamento.reconstituir(UUID.randomUUID(), CLIENTE_ID, ESTABELECIMENTO_ID, PROFISSIONAL_ID,
				SERVICO_ID, INICIO, INICIO.plus(Duration.ofMinutes(60)), status, new BigDecimal(preco), null, 0L);
	}

	@Test
	void agendaVaziaTemTodosOsStatusZeradosEReceitasZeradas() {
		ResumoDaAgenda resumo = ResumoDaAgenda.de(List.of());

		assertThat(resumo.total()).isZero();
		assertThat(resumo.porStatus()).containsOnlyKeys(StatusAgendamento.values());
		assertThat(resumo.porStatus().values()).containsOnly(0L);
		assertThat(resumo.receitaRealizada()).isEqualByComparingTo("0");
		assertThat(resumo.receitaPrevista()).isEqualByComparingTo("0");
		assertThat(resumo.receitaRealizada().scale()).isEqualTo(2);
	}

	@Test
	void contaOsAgendamentosPorStatus() {
		ResumoDaAgenda resumo = ResumoDaAgenda.de(List.of(
				comStatusEPreco(StatusAgendamento.AGENDADO, "50.00"),
				comStatusEPreco(StatusAgendamento.AGENDADO, "50.00"),
				comStatusEPreco(StatusAgendamento.CONFIRMADO, "20.00"),
				comStatusEPreco(StatusAgendamento.CONCLUIDO, "100.00"),
				comStatusEPreco(StatusAgendamento.CONCLUIDO, "80.00"),
				comStatusEPreco(StatusAgendamento.CANCELADO, "70.00")));

		assertThat(resumo.total()).isEqualTo(6);
		assertThat(resumo.porStatus())
				.containsEntry(StatusAgendamento.AGENDADO, 2L)
				.containsEntry(StatusAgendamento.CONFIRMADO, 1L)
				.containsEntry(StatusAgendamento.CONCLUIDO, 2L)
				.containsEntry(StatusAgendamento.CANCELADO, 1L)
				.containsEntry(StatusAgendamento.NAO_COMPARECEU, 0L);
	}

	@Test
	void receitaRealizadaSomaSomenteOsConcluidos() {
		ResumoDaAgenda resumo = ResumoDaAgenda.de(List.of(
				comStatusEPreco(StatusAgendamento.CONCLUIDO, "100.00"),
				comStatusEPreco(StatusAgendamento.CONCLUIDO, "80.50"),
				comStatusEPreco(StatusAgendamento.CONFIRMADO, "40.00")));

		assertThat(resumo.receitaRealizada()).isEqualByComparingTo("180.50");
	}

	@Test
	void receitaPrevistaSomaOsAgendamentosQueAindaOcupamHorario() {
		ResumoDaAgenda resumo = ResumoDaAgenda.de(List.of(
				comStatusEPreco(StatusAgendamento.AGENDADO, "50.00"),
				comStatusEPreco(StatusAgendamento.CONFIRMADO, "70.00"),
				comStatusEPreco(StatusAgendamento.CONCLUIDO, "100.00")));

		assertThat(resumo.receitaPrevista()).isEqualByComparingTo("120.00");
	}

	@Test
	void canceladoENaoComparecimentoNaoEntramEmNenhumaReceita() {
		ResumoDaAgenda resumo = ResumoDaAgenda.de(List.of(
				comStatusEPreco(StatusAgendamento.CANCELADO, "50.00"),
				comStatusEPreco(StatusAgendamento.NAO_COMPARECEU, "70.00")));

		assertThat(resumo.total()).isEqualTo(2);
		assertThat(resumo.receitaRealizada()).isEqualByComparingTo("0");
		assertThat(resumo.receitaPrevista()).isEqualByComparingTo("0");
	}

	@Test
	void totaisPorStatusNaoPodemSerAlterados() {
		ResumoDaAgenda resumo = ResumoDaAgenda.de(List.of());

		assertThatThrownBy(() -> resumo.porStatus().put(StatusAgendamento.AGENDADO, 10L))
				.isInstanceOf(UnsupportedOperationException.class);
	}

}
