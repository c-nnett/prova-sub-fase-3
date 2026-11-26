package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProfissionalJpaRepository extends JpaRepository<ProfissionalJpaEntity, UUID> {

	List<ProfissionalJpaEntity> findByEstabelecimentoIdOrderByNome(UUID estabelecimentoId);

	/** SQL nativo: as colunas são {@code updatable = false} na entidade e só mudam por aqui. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			update profissional
			   set nota_media = :notaMedia, total_avaliacoes = :totalAvaliacoes
			 where id = :id
			""", nativeQuery = true)
	int atualizarAvaliacoes(@Param("id") UUID id, @Param("notaMedia") BigDecimal notaMedia,
			@Param("totalAvaliacoes") int totalAvaliacoes);

}
