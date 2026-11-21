package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ListarEspecialidadesUseCase;
import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;

@WebMvcTest(EspecialidadeController.class)
class EspecialidadeControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ListarEspecialidadesUseCase listar;

	@Test
	void listaCatalogo() throws Exception {
		Especialidade barba = Especialidade.reconstituir(UUID.fromString("e5000000-0000-4000-8000-00000000000a"), "Barba");
		Especialidade corte = Especialidade.reconstituir(UUID.fromString("e5000000-0000-4000-8000-000000000001"), "Corte");
		given(listar.executar()).willReturn(List.of(barba, corte));

		mvc.perform(get("/api/v1/especialidades"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(barba.id().toString()))
				.andExpect(jsonPath("$[0].nome").value("Barba"))
				.andExpect(jsonPath("$[1].nome").value("Corte"));
	}

}
