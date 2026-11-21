package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "especialidade")
@Getter
@Setter
@NoArgsConstructor
class EspecialidadeJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 80)
	private String nome;

}
