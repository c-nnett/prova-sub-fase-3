package br.com.fiap.agendabeleza.api.calendario.adapters.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;

interface AssinaturaCalendarioJpaRepository extends JpaRepository<AssinaturaCalendarioJpaEntity, UUID> {

	/** Atendida pela restrição única {@code assinatura_calendario_token_uk}. */
	Optional<AssinaturaCalendarioJpaEntity> findByTokenHash(String tokenHash);

	/**
	 * Delete em JPQL, executado na hora. O delete derivado do Spring Data só agenda a remoção, e o Hibernate grava os
	 * inserts antes dos deletes no flush: a linha nova esbarraria na restrição única do titular.
	 */
	@Modifying
	@Query("delete from AssinaturaCalendarioJpaEntity a where a.papel = :papel and a.titularId = :titularId")
	void apagarDoTitular(@Param("papel") PapelNoCalendario papel, @Param("titularId") UUID titularId);

}
