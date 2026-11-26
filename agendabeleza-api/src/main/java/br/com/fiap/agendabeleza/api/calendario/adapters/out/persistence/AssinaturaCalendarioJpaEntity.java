package br.com.fiap.agendabeleza.api.calendario.adapters.out.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Linha imutável: renovar o token apaga a linha anterior e grava outra. */
@Entity
@Table(name = "assinatura_calendario")
@Getter
@Setter
@NoArgsConstructor
class AssinaturaCalendarioJpaEntity {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20, updatable = false)
	private PapelNoCalendario papel;

	@Column(name = "titular_id", nullable = false, updatable = false)
	private UUID titularId;

	/** {@code char(64)}: sem o tipo explícito, o {@code ddl-auto=validate} espera {@code varchar}. */
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "token_hash", nullable = false, length = 64, updatable = false)
	private String tokenHash;

	@Column(name = "criada_em", nullable = false, updatable = false)
	private Instant criadaEm;

}
