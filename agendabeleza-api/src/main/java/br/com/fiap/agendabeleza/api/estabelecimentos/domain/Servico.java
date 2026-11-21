package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoObrigatorio;
import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.textoOpcional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Serviço oferecido por um estabelecimento. A duração define o fim do agendamento, e o preço base pode ser
 * sobrescrito pela tarifa do profissional.
 */
public class Servico {

	private final UUID id;
	private final UUID estabelecimentoId;
	private String nome;
	private String descricao;
	private CategoriaServico categoria;
	private int duracaoMinutos;
	private BigDecimal precoBase;
	private boolean ativo;

	private Servico(UUID id, UUID estabelecimentoId) {
		this.id = obrigatorio(id, "id");
		this.estabelecimentoId = obrigatorio(estabelecimentoId, "estabelecimentoId");
	}

	public static Servico cadastrar(UUID estabelecimentoId, String nome, String descricao, CategoriaServico categoria,
			int duracaoMinutos, BigDecimal precoBase) {
		Servico servico = new Servico(UUID.randomUUID(), estabelecimentoId);
		servico.atualizar(nome, descricao, categoria, duracaoMinutos, precoBase, true);
		return servico;
	}

	/** Recria o serviço a partir dos dados gravados, aplicando as mesmas validações do cadastro. */
	public static Servico reconstituir(UUID id, UUID estabelecimentoId, String nome, String descricao,
			CategoriaServico categoria, int duracaoMinutos, BigDecimal precoBase, boolean ativo) {
		Servico servico = new Servico(id, estabelecimentoId);
		servico.atualizar(nome, descricao, categoria, duracaoMinutos, precoBase, ativo);
		return servico;
	}

	public void atualizar(String nome, String descricao, CategoriaServico categoria, int duracaoMinutos,
			BigDecimal precoBase, boolean ativo) {
		String novoNome = textoObrigatorio(nome, "nome", 120);
		String novaDescricao = textoOpcional(descricao, "descricao", 2000);
		CategoriaServico novaCategoria = obrigatorio(categoria, "categoria");
		if (duracaoMinutos <= 0) {
			throw new RegraNegocioException("duracaoMinutos deve ser maior que zero");
		}
		BigDecimal novoPreco = validarPreco(precoBase);

		this.nome = novoNome;
		this.descricao = novaDescricao;
		this.categoria = novaCategoria;
		this.duracaoMinutos = duracaoMinutos;
		this.precoBase = novoPreco;
		this.ativo = ativo;
	}

	public boolean pertenceA(UUID estabelecimentoId) {
		return this.estabelecimentoId.equals(estabelecimentoId);
	}

	public UUID id() {
		return id;
	}

	public UUID estabelecimentoId() {
		return estabelecimentoId;
	}

	public String nome() {
		return nome;
	}

	public String descricao() {
		return descricao;
	}

	public CategoriaServico categoria() {
		return categoria;
	}

	public int duracaoMinutos() {
		return duracaoMinutos;
	}

	public BigDecimal precoBase() {
		return precoBase;
	}

	public boolean ativo() {
		return ativo;
	}

	private static BigDecimal validarPreco(BigDecimal preco) {
		obrigatorio(preco, "precoBase");
		if (preco.signum() < 0) {
			throw new RegraNegocioException("precoBase não pode ser negativo");
		}
		if (preco.stripTrailingZeros().scale() > 2) {
			throw new RegraNegocioException("precoBase deve ter no máximo duas casas decimais");
		}
		return preco.setScale(2, RoundingMode.UNNECESSARY);
	}

}
