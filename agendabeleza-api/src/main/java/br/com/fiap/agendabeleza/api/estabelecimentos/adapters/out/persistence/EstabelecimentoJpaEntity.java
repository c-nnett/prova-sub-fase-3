package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estabelecimento")
@Getter
@Setter
@NoArgsConstructor
class EstabelecimentoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "gestor_id", nullable = false)
	private UUID gestorId;

	@Column(nullable = false, length = 150)
	private String nome;

	@Column(columnDefinition = "text")
	private String descricao;

	@Column(nullable = false, length = 150)
	private String logradouro;

	@Column(nullable = false, length = 10)
	private String numero;

	@Column(length = 80)
	private String complemento;

	@Column(nullable = false, length = 80)
	private String bairro;

	@Column(nullable = false, length = 80)
	private String cidade;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 2)
	private String uf;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 8)
	private String cep;

	@Column(nullable = false, length = 50)
	private String timezone;

	@Column(nullable = false)
	private boolean ativo;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@UpdateTimestamp
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	@OneToMany(mappedBy = "estabelecimento", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordem")
	private List<FotoEstabelecimentoJpaEntity> fotos = new ArrayList<>();

	@OneToMany(mappedBy = "estabelecimento", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("diaSemana")
	private List<HorarioFuncionamentoJpaEntity> horarios = new ArrayList<>();

}
