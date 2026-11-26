package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Critérios combináveis da busca, no mesmo padrão de {@code AgendamentoEspecificacoes}: critério ausente vira
 * {@link Specification#unrestricted()}. Serviço, categoria e preço entram num único {@code EXISTS} sobre os serviços
 * do estabelecimento, para valerem para o mesmo serviço e não repetirem o estabelecimento no resultado.
 */
final class BuscaEspecificacoes {

	private static final char ESCAPE = '\\';

	private BuscaEspecificacoes() {
	}

	static Specification<EstabelecimentoDaBuscaJpaEntity> de(FiltroDaBusca filtro) {
		return Specification.allOf(
				ativo(),
				nomeContendo(filtro.nome()),
				textoIgual("cidade", filtro.cidade()),
				textoIgual("bairro", filtro.bairro()),
				notaMinima(filtro.notaMinima()),
				comServico(filtro));
	}

	static Specification<EstabelecimentoDaBuscaJpaEntity> ativo() {
		return (raiz, consulta, criterios) -> criterios.isTrue(raiz.get("ativo"));
	}

	static Specification<EstabelecimentoDaBuscaJpaEntity> nomeContendo(String trecho) {
		if (trecho == null) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, criterios) -> contem(criterios, raiz.get("nome"), trecho);
	}

	/** Igualdade sem diferenciar maiúsculas, com a expressão do índice {@code idx_estabelecimento_cidade_bairro}. */
	static Specification<EstabelecimentoDaBuscaJpaEntity> textoIgual(String campo, String valor) {
		if (valor == null) {
			return Specification.unrestricted();
		}
		String valorMinusculo = minusculo(valor);
		return (raiz, consulta, criterios) -> criterios.equal(criterios.lower(raiz.get(campo)), valorMinusculo);
	}

	/** Nota média nula não passa no {@code >=}: sem avaliação fica de fora quando há nota mínima. */
	static Specification<EstabelecimentoDaBuscaJpaEntity> notaMinima(BigDecimal nota) {
		if (nota == null) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, criterios) -> criterios.greaterThanOrEqualTo(raiz.get("notaMedia"), nota);
	}

	static Specification<EstabelecimentoDaBuscaJpaEntity> comServico(FiltroDaBusca filtro) {
		if (!filtro.filtraPorServico()) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, criterios) -> {
			Subquery<Integer> servicos = consulta.subquery(Integer.class);
			Root<ServicoDaBuscaJpaEntity> servico = servicos.from(ServicoDaBuscaJpaEntity.class);
			List<Predicate> condicoes = new ArrayList<>();
			condicoes.add(criterios.equal(servico.get("estabelecimentoId"), raiz.get("id")));
			condicoes.add(criterios.isTrue(servico.get("ativo")));
			if (filtro.servico() != null) {
				condicoes.add(contem(criterios, servico.get("nome"), filtro.servico()));
			}
			if (filtro.categoria() != null) {
				condicoes.add(criterios.equal(servico.get("categoria"), filtro.categoria()));
			}
			if (filtro.precoMinimo() != null || filtro.precoMaximo() != null) {
				condicoes.add(criterios.exists(tarifaNaFaixa(servicos, servico, criterios, filtro)));
			}
			servicos.select(criterios.literal(1)).where(condicoes.toArray(Predicate[]::new));
			return criterios.exists(servicos);
		};
	}

	/** Alguma tarifa de profissional ativo para o serviço, dentro da faixa. */
	private static Subquery<Integer> tarifaNaFaixa(Subquery<Integer> servicos, Root<ServicoDaBuscaJpaEntity> servico,
			CriteriaBuilder criterios, FiltroDaBusca filtro) {
		Subquery<Integer> tarifas = servicos.subquery(Integer.class);
		Root<TarifaDaBuscaJpaEntity> tarifa = tarifas.from(TarifaDaBuscaJpaEntity.class);
		Root<ProfissionalDaBuscaJpaEntity> profissional = tarifas.from(ProfissionalDaBuscaJpaEntity.class);
		List<Predicate> condicoes = new ArrayList<>();
		condicoes.add(criterios.equal(tarifa.get("servicoId"), servico.get("id")));
		condicoes.add(criterios.equal(profissional.get("id"), tarifa.get("profissionalId")));
		condicoes.add(criterios.isTrue(profissional.get("ativo")));
		Expression<BigDecimal> valor = tarifa.get("tarifa");
		if (filtro.precoMinimo() != null) {
			condicoes.add(criterios.greaterThanOrEqualTo(valor, filtro.precoMinimo()));
		}
		if (filtro.precoMaximo() != null) {
			condicoes.add(criterios.lessThanOrEqualTo(valor, filtro.precoMaximo()));
		}
		return tarifas.select(criterios.literal(1)).where(condicoes.toArray(Predicate[]::new));
	}

	/** {@code lower(campo) like '%trecho%'}, com {@code %} e {@code _} do trecho tratados como texto. */
	private static Predicate contem(CriteriaBuilder criterios, Expression<String> campo, String trecho) {
		String padrao = "%" + escaparCuringas(minusculo(trecho)) + "%";
		return criterios.like(criterios.lower(campo), padrao, ESCAPE);
	}

	/** Os valores vão como parâmetro da consulta, já em minúsculas, e não como literal no SQL. */
	private static String minusculo(String texto) {
		return texto.toLowerCase(Locale.ROOT);
	}

	private static String escaparCuringas(String trecho) {
		return trecho.replace(String.valueOf(ESCAPE), "" + ESCAPE + ESCAPE)
				.replace("%", ESCAPE + "%")
				.replace("_", ESCAPE + "_");
	}

}
