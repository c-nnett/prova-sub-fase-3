package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.fiap.agendabeleza.api.profissionais.application.usecase.PerfilProfissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProfissionalResponse(
		UUID id,
		UUID estabelecimentoId,
		UUID usuarioId,
		String nome,
		@JsonInclude(JsonInclude.Include.NON_NULL)
		@Schema(description = "Somente nas respostas de cadastro e alteração. As consultas públicas não trazem o e-mail")
		String email,
		String bio,
		@Schema(description = "Nula até a primeira avaliação") BigDecimal notaMedia,
		int totalAvaliacoes,
		boolean ativo,
		List<EspecialidadeResponse> especialidades,
		List<ServicoProfissionalDto> servicos,
		@Schema(description = "Ordenada de segunda a domingo e pelo início") List<PeriodoDto> jornada) {

	/** Resposta para o gestor ou o próprio profissional, com o e-mail. */
	public static ProfissionalResponse completo(PerfilProfissional perfil) {
		return de(perfil, perfil.profissional().email());
	}

	/** Resposta das consultas públicas, sem o e-mail. */
	public static ProfissionalResponse publico(PerfilProfissional perfil) {
		return de(perfil, null);
	}

	private static ProfissionalResponse de(PerfilProfissional perfil, String email) {
		Profissional profissional = perfil.profissional();
		return new ProfissionalResponse(
				profissional.id(),
				profissional.estabelecimentoId(),
				profissional.usuarioId(),
				profissional.nome(),
				email,
				profissional.bio(),
				profissional.notaMedia(),
				profissional.totalAvaliacoes(),
				profissional.ativo(),
				perfil.especialidades().stream().map(EspecialidadeResponse::de).toList(),
				profissional.servicos().stream().map(ServicoProfissionalDto::de).toList(),
				profissional.jornada().stream().map(PeriodoDto::de).toList());
	}

}
