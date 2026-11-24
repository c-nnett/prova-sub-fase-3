package br.com.fiap.agendabeleza.api.compartilhado.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import br.com.fiap.agendabeleza.api.compartilhado.application.port.ExecucaoSerializada;

/**
 * Executa a operação na hora, sem transação nem trava, e guarda as chaves usadas. Serve aos testes em memória, que
 * rodam em uma única thread.
 */
public class ExecucaoSerializadaDireta implements ExecucaoSerializada {

	private final List<UUID> chaves = new ArrayList<>();

	@Override
	public <T> T executar(UUID chave, Supplier<T> operacao) {
		chaves.add(chave);
		return operacao.get();
	}

	public List<UUID> chaves() {
		return List.copyOf(chaves);
	}

}
