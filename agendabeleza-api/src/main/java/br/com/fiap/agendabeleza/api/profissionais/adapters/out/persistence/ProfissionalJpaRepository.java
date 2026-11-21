package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProfissionalJpaRepository extends JpaRepository<ProfissionalJpaEntity, UUID> {

	List<ProfissionalJpaEntity> findByEstabelecimentoIdOrderByNome(UUID estabelecimentoId);

}
