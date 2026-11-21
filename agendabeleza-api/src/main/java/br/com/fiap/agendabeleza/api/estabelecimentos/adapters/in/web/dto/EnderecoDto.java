package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Endereco;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnderecoDto(
		@NotBlank @Size(max = 150) String logradouro,
		@NotBlank @Size(max = 10) String numero,
		@Size(max = 80) String complemento,
		@NotBlank @Size(max = 80) String bairro,
		@NotBlank @Size(max = 80) String cidade,
		@NotBlank @Schema(example = "SP") String uf,
		@NotBlank @Schema(example = "01305000", description = "Oito dígitos, com ou sem hífen") String cep) {

	public Endereco paraDominio() {
		return new Endereco(logradouro, numero, complemento, bairro, cidade, uf, cep);
	}

	public static EnderecoDto de(Endereco endereco) {
		return new EnderecoDto(endereco.logradouro(), endereco.numero(), endereco.complemento(), endereco.bairro(),
				endereco.cidade(), endereco.uf(), endereco.cep());
	}

}
