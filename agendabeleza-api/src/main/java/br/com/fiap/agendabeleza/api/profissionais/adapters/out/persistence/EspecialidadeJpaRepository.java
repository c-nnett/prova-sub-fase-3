package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EspecialidadeJpaRepository extends JpaRepository<EspecialidadeJpaEntity, UUID> {
}
