package br.com.fiap.agendabeleza.api.profissionais.domain;

import java.util.UUID;

/**
 * Dados válidos reutilizados pelos testes do módulo de profissionais.
 */
public final class ProfissionalFixture {

	public static final UUID ESTABELECIMENTO_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	public static final UUID USUARIO_PROFISSIONAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

	private ProfissionalFixture() {
	}

	public static Profissional profissional(UUID estabelecimentoId) {
		return Profissional.cadastrar(estabelecimentoId, USUARIO_PROFISSIONAL_ID, "Ana Souza", "ana@salao.com",
				"Colorista há 10 anos");
	}

	public static Profissional profissional() {
		return profissional(ESTABELECIMENTO_ID);
	}

}
