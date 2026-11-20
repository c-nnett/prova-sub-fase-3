package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EstabelecimentoJpaRepository extends JpaRepository<EstabelecimentoJpaEntity, UUID> {
}
