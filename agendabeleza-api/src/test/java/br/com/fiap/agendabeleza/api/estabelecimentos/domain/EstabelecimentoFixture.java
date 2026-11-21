package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import java.util.UUID;

/**
 * Dados válidos reutilizados pelos testes do módulo de estabelecimentos.
 */
public final class EstabelecimentoFixture {

	public static final UUID GESTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	public static final UUID OUTRO_USUARIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	private EstabelecimentoFixture() {
	}

	public static Endereco endereco() {
		return new Endereco("Rua Augusta", "100", null, "Consolação", "São Paulo", "SP", "01305000");
	}

	public static Estabelecimento estabelecimento() {
		return Estabelecimento.cadastrar(GESTOR_ID, "Salão Bela Vista", "Cabelo e unhas", endereco(), null);
	}

}
