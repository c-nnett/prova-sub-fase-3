package br.com.fiap.agendabeleza.api.compartilhado.application.port;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Executa uma operação em uma única transação, uma de cada vez por chave. Duas operações com a mesma chave nunca
 * rodam ao mesmo tempo: a segunda espera a primeira terminar e só então faz as próprias verificações.
 * <p>
 * Usada para que reservas e bloqueios da agenda de um mesmo profissional não passem juntos pelas verificações de
 * horário livre, já que nenhuma restrição do banco compara as duas tabelas.
 */
public interface ExecucaoSerializada {

	/**
	 * @param chave    identifica o recurso disputado, como o id do profissional
	 * @param operacao verificações e gravações que precisam rodar juntas
	 * @return o resultado da operação, depois do commit
	 */
	<T> T executar(UUID chave, Supplier<T> operacao);

}
