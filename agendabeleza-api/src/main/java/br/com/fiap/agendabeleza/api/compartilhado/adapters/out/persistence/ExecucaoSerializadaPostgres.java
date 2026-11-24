package br.com.fiap.agendabeleza.api.compartilhado.adapters.out.persistence;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.fiap.agendabeleza.api.compartilhado.application.port.ExecucaoSerializada;

/**
 * Implementa {@link ExecucaoSerializada} com uma trava consultiva de transação do PostgreSQL
 * ({@code pg_advisory_xact_lock}). A trava é obtida no início da transação e liberada no commit ou no rollback.
 * <p>
 * A chave é convertida para {@code bigint} com {@code hashtextextended}. Duas chaves diferentes podem, raramente,
 * cair no mesmo número: nesse caso as operações só esperam uma pela outra, sem erro.
 */
@Component
class ExecucaoSerializadaPostgres implements ExecucaoSerializada {

	private static final String TRAVAR = "select count(*) from (select pg_advisory_xact_lock(hashtextextended(?, 0))) t";

	private final TransactionTemplate transacao;
	private final JdbcTemplate jdbc;

	ExecucaoSerializadaPostgres(PlatformTransactionManager gerenciador, JdbcTemplate jdbc) {
		this.transacao = new TransactionTemplate(gerenciador);
		this.jdbc = jdbc;
	}

	@Override
	public <T> T executar(UUID chave, Supplier<T> operacao) {
		return transacao.execute(status -> {
			jdbc.queryForObject(TRAVAR, Long.class, chave.toString());
			return operacao.get();
		});
	}

}
