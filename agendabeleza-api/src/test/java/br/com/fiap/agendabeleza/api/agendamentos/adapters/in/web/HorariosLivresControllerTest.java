package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarHorariosLivresUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.HorariosLivres;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

@WebMvcTest(HorariosLivresController.class)
class HorariosLivresControllerTest {

	private static final UUID PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
	private static final String URL = "/api/v1/profissionais/" + PROFISSIONAL_ID + "/horarios-livres";
	private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 30);
	private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultarHorariosLivresUseCase consultar;

	private static Intervalo intervalo(String inicio, String fim) {
		return new Intervalo(SEGUNDA.atTime(LocalTime.parse(inicio)).atZone(FUSO).toInstant(),
				SEGUNDA.atTime(LocalTime.parse(fim)).atZone(FUSO).toInstant());
	}

	@Test
	void devolveOsHorariosNoFusoDoEstabelecimento() throws Exception {
		given(consultar.executar(new ConsultarHorariosLivresUseCase.Consulta(PROFISSIONAL_ID, SERVICO_ID, SEGUNDA)))
				.willReturn(new HorariosLivres(SEGUNDA, FUSO, 60,
						List.of(intervalo("09:00", "10:00"), intervalo("11:00", "12:00"))));

		mvc.perform(get(URL).param("servicoId", SERVICO_ID.toString()).param("data", "2026-11-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value("2026-11-30"))
				.andExpect(jsonPath("$.fuso").value("America/Sao_Paulo"))
				.andExpect(jsonPath("$.duracaoMinutos").value(60))
				.andExpect(jsonPath("$.horarios.length()").value(2))
				.andExpect(jsonPath("$.horarios[0].inicio").value("2026-11-30T09:00:00-03:00"))
				.andExpect(jsonPath("$.horarios[0].fim").value("2026-11-30T10:00:00-03:00"))
				.andExpect(jsonPath("$.horarios[1].inicio").value("2026-11-30T11:00:00-03:00"));
	}

	@Test
	void diaSemHorariosDevolveListaVazia() throws Exception {
		given(consultar.executar(any())).willReturn(new HorariosLivres(SEGUNDA, FUSO, 60, List.of()));

		mvc.perform(get(URL).param("servicoId", SERVICO_ID.toString()).param("data", "2026-11-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horarios").isEmpty());
	}

	@Test
	void recusaConsultaSemParametros() throws Exception {
		mvc.perform(get(URL).param("data", "2026-11-30"))
				.andExpect(status().isBadRequest());
		mvc.perform(get(URL).param("servicoId", SERVICO_ID.toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void recusaDataMalformada() throws Exception {
		mvc.perform(get(URL).param("servicoId", SERVICO_ID.toString()).param("data", "30/11/2026"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void profissionalInexistenteDevolve404() throws Exception {
		given(consultar.executar(any())).willThrow(new RecursoNaoEncontradoException("Profissional não encontrado"));

		mvc.perform(get(URL).param("servicoId", SERVICO_ID.toString()).param("data", "2026-11-30"))
				.andExpect(status().isNotFound());
	}

	@Test
	void servicoQueOProfissionalNaoExecutaDevolve422() throws Exception {
		given(consultar.executar(any())).willThrow(new RegraNegocioException("O profissional não executa o serviço"));

		mvc.perform(get(URL).param("servicoId", SERVICO_ID.toString()).param("data", "2026-11-30"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("O profissional não executa o serviço"));
	}

}
