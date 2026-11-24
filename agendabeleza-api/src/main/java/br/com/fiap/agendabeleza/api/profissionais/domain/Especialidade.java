package br.com.fiap.agendabeleza.api.profissionais.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoObrigatorio;

import java.util.UUID;

/**
 * Item do catálogo de especialidades, como colorimetria ou manicure. O catálogo é comum a todos os
 * estabelecimentos.
 */
public class Especialidade {

	private final UUID id;
	private final String nome;

	private Especialidade(UUID id, String nome) {
		this.id = obrigatorio(id, "id");
		this.nome = textoObrigatorio(nome, "nome", 80);
	}

	public static Especialidade cadastrar(String nome) {
		return new Especialidade(UUID.randomUUID(), nome);
	}

	public static Especialidade reconstituir(UUID id, String nome) {
		return new Especialidade(id, nome);
	}

	public UUID id() {
		return id;
	}

	public String nome() {
		return nome;
	}

}
