package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class ServicoTest {

	private static final UUID ESTABELECIMENTO_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");

	private Servico corte() {
		return Servico.cadastrar(ESTABELECIMENTO_ID, "Corte feminino", "Lavagem inclusa",
				CategoriaServico.CABELO, 60, new BigDecimal("80"));
	}

	@Test
	void cadastraAtivoComPrecoEmDuasCasas() {
		Servico servico = corte();

		assertThat(servico.id()).isNotNull();
		assertThat(servico.estabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
		assertThat(servico.nome()).isEqualTo("Corte feminino");
		assertThat(servico.descricao()).isEqualTo("Lavagem inclusa");
		assertThat(servico.categoria()).isEqualTo(CategoriaServico.CABELO);
		assertThat(servico.duracaoMinutos()).isEqualTo(60);
		assertThat(servico.precoBase()).isEqualTo(new BigDecimal("80.00"));
		assertThat(servico.ativo()).isTrue();
		assertThat(servico.pertenceA(ESTABELECIMENTO_ID)).isTrue();
		assertThat(servico.pertenceA(UUID.randomUUID())).isFalse();
	}

	@Test
	void atualizaDadosEStatus() {
		Servico servico = corte();

		servico.atualizar("Corte e escova", null, CategoriaServico.CABELO, 90, new BigDecimal("120.50"), false);

		assertThat(servico.nome()).isEqualTo("Corte e escova");
		assertThat(servico.descricao()).isNull();
		assertThat(servico.duracaoMinutos()).isEqualTo(90);
		assertThat(servico.precoBase()).isEqualTo(new BigDecimal("120.50"));
		assertThat(servico.ativo()).isFalse();
	}

	@Test
	void recusaDuracaoNaoPositiva() {
		assertThatThrownBy(() -> Servico.cadastrar(ESTABELECIMENTO_ID, "Corte", null,
				CategoriaServico.CABELO, 0, BigDecimal.TEN))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("duracaoMinutos deve ser maior que zero");
	}

	@Test
	void recusaPrecoNegativoOuAusente() {
		assertThatThrownBy(() -> Servico.cadastrar(ESTABELECIMENTO_ID, "Corte", null,
				CategoriaServico.CABELO, 30, new BigDecimal("-0.01")))
				.hasMessage("precoBase não pode ser negativo");
		assertThatThrownBy(() -> Servico.cadastrar(ESTABELECIMENTO_ID, "Corte", null,
				CategoriaServico.CABELO, 30, null))
				.hasMessage("precoBase é obrigatório");
	}

	@Test
	void recusaPrecoComMaisDeDuasCasasDecimais() {
		assertThatThrownBy(() -> Servico.cadastrar(ESTABELECIMENTO_ID, "Corte", null,
				CategoriaServico.CABELO, 30, new BigDecimal("10.555")))
				.hasMessage("precoBase deve ter no máximo duas casas decimais");
	}

	@Test
	void recusaSemEstabelecimentoNomeOuCategoria() {
		assertThatThrownBy(() -> Servico.cadastrar(null, "Corte", null, CategoriaServico.CABELO, 30, BigDecimal.TEN))
				.hasMessage("estabelecimentoId é obrigatório");
		assertThatThrownBy(() -> Servico.cadastrar(ESTABELECIMENTO_ID, null, null, CategoriaServico.CABELO, 30, BigDecimal.TEN))
				.hasMessage("nome é obrigatório");
		assertThatThrownBy(() -> Servico.cadastrar(ESTABELECIMENTO_ID, "Corte", null, null, 30, BigDecimal.TEN))
				.hasMessage("categoria é obrigatório");
	}

	@Test
	void recusaNomeAcimaDe120Caracteres() {
		assertThatThrownBy(() -> Servico.cadastrar(ESTABELECIMENTO_ID, "a".repeat(121), null,
				CategoriaServico.CABELO, 30, BigDecimal.TEN))
				.hasMessage("nome deve ter no máximo 120 caracteres");
	}

}
