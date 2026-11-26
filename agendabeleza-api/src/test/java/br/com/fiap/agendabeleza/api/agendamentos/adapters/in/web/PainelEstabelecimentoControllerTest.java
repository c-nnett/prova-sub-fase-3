package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarAgendaDoEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.DadosDoPeriodo;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.PeriodoDoPainel;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ResumirAgendaDoEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.ResumoDaAgenda;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture;

@WebMvcTest(PainelEstabelecimentoController.class)
class PainelEstabelecimentoControllerTest {

	private static final String USUARIO = "X-Usuario-Id";
	private static final UUID GESTOR_ID = EstabelecimentoFixture.GESTOR_ID;
	private static final UUID ESTABELECIMENTO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
	private static final String URL_DA_AGENDA = "/api/v1/estabelecimentos/" + ESTABELECIMENTO_ID + "/agendamentos";
	private static final String URL_DO_RESUMO = "/api/v1/estabelecimentos/" + ESTABELECIMENTO_ID
			+ "/painel/resumo";
	private static final LocalDate SEGUNDA = LocalDate.of(2030, 3, 4);
	private static final LocalDate SEXTA = LocalDate.of(2030, 3, 8);
	private static final Instant INICIO = Instant.parse("2030-03-04T13:00:00Z");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultarAgendaDoEstabelecimentoUseCase consultarAgenda;
	@MockitoBean
	private ResumirAgendaDoEstabelecimentoUseCase resumirAgenda;

	private static PeriodoDoPainel periodo(LocalDate de, LocalDate ate) {
		return new PeriodoDoPainel(EstabelecimentoFixture.estabelecimento(), de, ate);
	}

	private static Agendamento agendamentoCom(StatusAgendamento status) {
		return Agendamento.reconstituir(UUID.randomUUID(), UUID.randomUUID(), ESTABELECIMENTO_ID, PROFISSIONAL_ID,
				UUID.randomUUID(), INICIO, INICIO.plusSeconds(3600), status, new BigDecimal("95.00"), null, 1L);
	}

	private static ConsultarAgendaDoEstabelecimentoUseCase.Consulta consultaDaAgenda(LocalDate de, LocalDate ate,
			Set<StatusAgendamento> status, UUID profissionalId) {
		return new ConsultarAgendaDoEstabelecimentoUseCase.Consulta(ESTABELECIMENTO_ID, GESTOR_ID, de, ate, status,
				profissionalId);
	}

	// Agenda

	@Test
	void agendaSemFiltrosRepassaTudoVazioEDevolveALista() throws Exception {
		Agendamento agendado = agendamentoCom(StatusAgendamento.AGENDADO);
		given(consultarAgenda.executar(consultaDaAgenda(null, null, Set.of(), null)))
				.willReturn(new DadosDoPeriodo<>(periodo(SEGUNDA, SEGUNDA), List.of(agendado)));

		mvc.perform(get(URL_DA_AGENDA).header(USUARIO, GESTOR_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(agendado.id().toString()))
				.andExpect(jsonPath("$[0].status").value("AGENDADO"));
	}

	@Test
	void agendaRepassaPeriodoStatusRepetidoEProfissional() throws Exception {
		given(consultarAgenda.executar(consultaDaAgenda(SEGUNDA, SEXTA,
				Set.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO), PROFISSIONAL_ID)))
				.willReturn(new DadosDoPeriodo<>(periodo(SEGUNDA, SEXTA),
						List.of(agendamentoCom(StatusAgendamento.CONFIRMADO))));

		mvc.perform(get(URL_DA_AGENDA).header(USUARIO, GESTOR_ID)
				.param("de", "2030-03-04")
				.param("ate", "2030-03-08")
				.param("status", "AGENDADO", "CONFIRMADO")
				.param("profissionalId", PROFISSIONAL_ID.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("CONFIRMADO"));
	}

	@Test
	void agendaComStatusInexistenteDevolve400() throws Exception {
		mvc.perform(get(URL_DA_AGENDA).header(USUARIO, GESTOR_ID).param("status", "PENDENTE"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(consultarAgenda);
	}

	@Test
	void agendaComDataMalformadaDevolve400() throws Exception {
		mvc.perform(get(URL_DA_AGENDA).header(USUARIO, GESTOR_ID).param("de", "04/03/2030"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(consultarAgenda);
	}

	@Test
	void agendaSemCabecalhoDoUsuarioDevolve400() throws Exception {
		mvc.perform(get(URL_DA_AGENDA))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(consultarAgenda);
	}

	@Test
	void agendaDeQuemNaoEGestorDevolve403() throws Exception {
		given(consultarAgenda.executar(any())).willThrow(new AcessoNegadoException("negado"));

		mvc.perform(get(URL_DA_AGENDA).header(USUARIO, GESTOR_ID))
				.andExpect(status().isForbidden());
	}

	@Test
	void agendaDeEstabelecimentoInexistenteDevolve404() throws Exception {
		given(consultarAgenda.executar(any())).willThrow(new RecursoNaoEncontradoException("inexistente"));

		mvc.perform(get(URL_DA_AGENDA).header(USUARIO, GESTOR_ID))
				.andExpect(status().isNotFound());
	}

	@Test
	void agendaComPeriodoInvalidoDevolve422() throws Exception {
		given(consultarAgenda.executar(any()))
				.willThrow(new RegraNegocioException(PeriodoDoPainel.PERIODO_LONGO_DEMAIS));

		mvc.perform(get(URL_DA_AGENDA).header(USUARIO, GESTOR_ID).param("de", "2030-03-04")
				.param("ate", "2030-06-04"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(PeriodoDoPainel.PERIODO_LONGO_DEMAIS));
	}

	// Resumo

	@Test
	void resumoDevolveOPeriodoOsTotaisPorStatusEAsReceitas() throws Exception {
		ResumoDaAgenda resumo = ResumoDaAgenda.de(List.of(
				agendamentoCom(StatusAgendamento.AGENDADO),
				agendamentoCom(StatusAgendamento.CONCLUIDO)));
		given(resumirAgenda.executar(new ResumirAgendaDoEstabelecimentoUseCase.Consulta(ESTABELECIMENTO_ID,
				GESTOR_ID, SEGUNDA, null, PROFISSIONAL_ID)))
				.willReturn(new DadosDoPeriodo<>(periodo(SEGUNDA, SEGUNDA), resumo));

		mvc.perform(get(URL_DO_RESUMO).header(USUARIO, GESTOR_ID)
				.param("de", "2030-03-04")
				.param("profissionalId", PROFISSIONAL_ID.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.de").value("2030-03-04"))
				.andExpect(jsonPath("$.ate").value("2030-03-04"))
				.andExpect(jsonPath("$.fuso").value("America/Sao_Paulo"))
				.andExpect(jsonPath("$.total").value(2))
				.andExpect(jsonPath("$.porStatus.AGENDADO").value(1))
				.andExpect(jsonPath("$.porStatus.CONCLUIDO").value(1))
				.andExpect(jsonPath("$.porStatus.NAO_COMPARECEU").value(0))
				.andExpect(jsonPath("$.receitaRealizada").value(95.00))
				.andExpect(jsonPath("$.receitaPrevista").value(95.00));
	}

	@Test
	void resumoDeQuemNaoEGestorDevolve403() throws Exception {
		given(resumirAgenda.executar(any())).willThrow(new AcessoNegadoException("negado"));

		mvc.perform(get(URL_DO_RESUMO).header(USUARIO, GESTOR_ID))
				.andExpect(status().isForbidden());
	}

}
