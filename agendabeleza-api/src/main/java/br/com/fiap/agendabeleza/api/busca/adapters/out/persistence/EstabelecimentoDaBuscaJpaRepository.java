package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

interface EstabelecimentoDaBuscaJpaRepository extends Repository<EstabelecimentoDaBuscaJpaEntity, UUID>,
		JpaSpecificationExecutor<EstabelecimentoDaBuscaJpaEntity> {
}
