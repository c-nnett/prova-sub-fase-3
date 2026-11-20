package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoObrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoOpcional;

import java.util.Locale;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Endereço do estabelecimento. A UF fica em maiúsculas e o CEP só com dígitos.
 */
public record Endereco(
		String logradouro,
		String numero,
		String complemento,
		String bairro,
		String cidade,
		String uf,
		String cep) {

	public Endereco {
		logradouro = textoObrigatorio(logradouro, "logradouro", 150);
		numero = textoObrigatorio(numero, "numero", 10);
		complemento = textoOpcional(complemento, "complemento", 80);
		bairro = textoObrigatorio(bairro, "bairro", 80);
		cidade = textoObrigatorio(cidade, "cidade", 80);

		uf = textoObrigatorio(uf, "uf", 2).toUpperCase(Locale.ROOT);
		if (!uf.matches("[A-Z]{2}")) {
			throw new RegraNegocioException("uf deve ter duas letras");
		}

		cep = textoObrigatorio(cep, "cep", 9).replace("-", "");
		if (!cep.matches("\\d{8}")) {
			throw new RegraNegocioException("cep deve ter oito dígitos");
		}
	}

}
