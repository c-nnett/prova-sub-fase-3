package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServicoJpaRepository extends JpaRepository<ServicoJpaEntity, UUID> {

	List<ServicoJpaEntity> findByEstabelecimentoIdOrderByNome(UUID estabelecimentoId);

}
