package br.com.fiap.agendabeleza.api.avaliacoes.adapters.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AvaliacaoJpaRepository extends JpaRepository<AvaliacaoJpaEntity, UUID> {

	boolean existsByAgendamentoId(UUID agendamentoId);

	List<AvaliacaoJpaEntity> findByEstabelecimentoIdOrderByCriadaEmDesc(UUID estabelecimentoId);

	List<AvaliacaoJpaEntity> findByProfissionalIdOrderByCriadaEmDesc(UUID profissionalId);

	@Query("""
			select new br.com.fiap.agendabeleza.api.avaliacoes.adapters.out.persistence.SomaDasNotas(
			       coalesce(sum(a.notaEstabelecimento), 0L), count(a))
			  from AvaliacaoJpaEntity a
			 where a.estabelecimentoId = :estabelecimentoId
			""")
	SomaDasNotas somarNotasDoEstabelecimento(@Param("estabelecimentoId") UUID estabelecimentoId);

	@Query("""
			select new br.com.fiap.agendabeleza.api.avaliacoes.adapters.out.persistence.SomaDasNotas(
			       coalesce(sum(a.notaProfissional), 0L), count(a))
			  from AvaliacaoJpaEntity a
			 where a.profissionalId = :profissionalId
			""")
	SomaDasNotas somarNotasDoProfissional(@Param("profissionalId") UUID profissionalId);

}
