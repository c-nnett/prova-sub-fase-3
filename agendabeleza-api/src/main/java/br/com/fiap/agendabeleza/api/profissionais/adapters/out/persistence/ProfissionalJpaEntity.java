package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profissional")
@Getter
@Setter
@NoArgsConstructor
class ProfissionalJpaEntity {

	@Id
	private UUID id;

	@Column(name = "estabelecimento_id", nullable = false)
	private UUID estabelecimentoId;

	@Column(name = "usuario_id", unique = true)
	private UUID usuarioId;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false, length = 180)
	private String email;

	@Column(columnDefinition = "text")
	private String bio;

	@Column(name = "nota_media", precision = 3, scale = 2)
	private BigDecimal notaMedia;

	@Column(name = "total_avaliacoes", nullable = false)
	private int totalAvaliacoes;

	@Column(nullable = false)
	private boolean ativo;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@UpdateTimestamp
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	@ElementCollection
	@CollectionTable(name = "profissional_especialidade", joinColumns = @JoinColumn(name = "profissional_id"))
	@Column(name = "especialidade_id", nullable = false)
	private Set<UUID> especialidadeIds = new LinkedHashSet<>();

	@ElementCollection
	@CollectionTable(name = "profissional_servico", joinColumns = @JoinColumn(name = "profissional_id"))
	private List<ServicoProfissionalJpaEmbeddable> servicos = new ArrayList<>();

	@OneToMany(mappedBy = "profissional", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("diaSemana, horaInicio")
	private List<DisponibilidadeJpaEntity> disponibilidades = new ArrayList<>();

}
