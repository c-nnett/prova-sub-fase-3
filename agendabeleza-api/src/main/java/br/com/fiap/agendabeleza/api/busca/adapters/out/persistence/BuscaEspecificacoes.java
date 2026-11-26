package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.JanelaDeDisponibilidade;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Critérios combináveis da busca, no mesmo padrão de {@code AgendamentoEspecificacoes}: critério ausente vira
 * {@link Specification#unrestricted()}. Serviço, categoria e preço entram num único {@code EXISTS} sobre os serviços
 * do estabelecimento, para valerem para o mesmo serviço e não repetirem o estabelecimento no resultado. A
 * disponibilidade é outro {@code EXISTS}, sobre os profissionais, e com filtro de serviço exige que o profissional
 * disponível execute um serviço que atenda ao filtro.
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
				comServico(filtro),
				disponivel(filtro));
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
			List<Predicate> condicoes = condicoesDoServico(servico, criterios, filtro);
			condicoes.add(criterios.equal(servico.get("estabelecimentoId"), raiz.get("id")));
			if (filtro.precoMinimo() != null || filtro.precoMaximo() != null) {
				condicoes.add(criterios.exists(tarifaNaFaixa(servicos, servico, criterios, filtro)));
			}
			return criterios.exists(selecionarSe(servicos, criterios, condicoes));
		};
	}

	/**
	 * Algum profissional ativo do estabelecimento cuja jornada no dia da semana da janela, o funcionamento do
	 * estabelecimento nesse dia e a janela tenham um instante em comum. Os três intervalos são semiabertos, então a
	 * interseção existe quando todo início vem antes de todo fim. Sem fim na janela, ela vai até o fim do dia.
	 */
	static Specification<EstabelecimentoDaBuscaJpaEntity> disponivel(FiltroDaBusca filtro) {
		JanelaDeDisponibilidade janela = filtro.disponibilidade();
		if (janela == null) {
			return Specification.unrestricted();
		}
		short dia = (short) janela.diaDaSemana();
		return (raiz, consulta, criterios) -> {
			Subquery<Integer> disponiveis = consulta.subquery(Integer.class);
			Root<ProfissionalDaBuscaJpaEntity> profissional = disponiveis.from(ProfissionalDaBuscaJpaEntity.class);
			Root<DisponibilidadeDaBuscaJpaEntity> jornada = disponiveis.from(DisponibilidadeDaBuscaJpaEntity.class);
			Root<HorarioFuncionamentoDaBuscaJpaEntity> funcionamento =
					disponiveis.from(HorarioFuncionamentoDaBuscaJpaEntity.class);
			Path<LocalTime> inicioDaJornada = jornada.get("horaInicio");
			Path<LocalTime> fimDaJornada = jornada.get("horaFim");
			Path<LocalTime> abertura = funcionamento.get("abreAs");
			Path<LocalTime> fechamento = funcionamento.get("fechaAs");

			List<Predicate> condicoes = new ArrayList<>();
			condicoes.add(criterios.equal(profissional.get("estabelecimentoId"), raiz.get("id")));
			condicoes.add(criterios.isTrue(profissional.get("ativo")));
			condicoes.add(criterios.equal(jornada.get("profissionalId"), profissional.get("id")));
			condicoes.add(criterios.equal(funcionamento.get("estabelecimentoId"), raiz.get("id")));
			condicoes.add(criterios.equal(jornada.get("diaSemana"), dia));
			condicoes.add(criterios.equal(funcionamento.get("diaSemana"), dia));
			condicoes.add(criterios.lessThan(inicioDaJornada, fechamento));
			condicoes.add(criterios.lessThan(abertura, fimDaJornada));
			condicoes.add(criterios.greaterThan(fimDaJornada, janela.inicio()));
			condicoes.add(criterios.greaterThan(fechamento, janela.inicio()));
			if (janela.fim() != null) {
				condicoes.add(criterios.lessThan(inicioDaJornada, janela.fim()));
				condicoes.add(criterios.lessThan(abertura, janela.fim()));
			}
			if (filtro.filtraPorServico()) {
				condicoes.add(criterios.exists(servicoDoProfissional(disponiveis, profissional, criterios, filtro)));
			}
			return criterios.exists(selecionarSe(disponiveis, criterios, condicoes));
		};
	}

	/** Alguma tarifa de profissional ativo para o serviço, dentro da faixa. */
	private static Subquery<Integer> tarifaNaFaixa(CommonAbstractCriteria consulta,
			Root<ServicoDaBuscaJpaEntity> servico, CriteriaBuilder criterios, FiltroDaBusca filtro) {
		Subquery<Integer> tarifas = consulta.subquery(Integer.class);
		Root<TarifaDaBuscaJpaEntity> tarifa = tarifas.from(TarifaDaBuscaJpaEntity.class);
		Root<ProfissionalDaBuscaJpaEntity> profissional = tarifas.from(ProfissionalDaBuscaJpaEntity.class);
		List<Predicate> condicoes = condicoesDaFaixa(tarifa.get("tarifa"), criterios, filtro);
		condicoes.add(criterios.equal(tarifa.get("servicoId"), servico.get("id")));
		condicoes.add(criterios.equal(profissional.get("id"), tarifa.get("profissionalId")));
		condicoes.add(criterios.isTrue(profissional.get("ativo")));
		return selecionarSe(tarifas, criterios, condicoes);
	}

	/** Algum serviço que o profissional executa e que atende aos filtros de serviço, categoria e preço. */
	private static Subquery<Integer> servicoDoProfissional(CommonAbstractCriteria consulta,
			Root<ProfissionalDaBuscaJpaEntity> profissional, CriteriaBuilder criterios, FiltroDaBusca filtro) {
		Subquery<Integer> executados = consulta.subquery(Integer.class);
		Root<TarifaDaBuscaJpaEntity> tarifa = executados.from(TarifaDaBuscaJpaEntity.class);
		Root<ServicoDaBuscaJpaEntity> servico = executados.from(ServicoDaBuscaJpaEntity.class);
		List<Predicate> condicoes = condicoesDoServico(servico, criterios, filtro);
		condicoes.addAll(condicoesDaFaixa(tarifa.get("tarifa"), criterios, filtro));
		condicoes.add(criterios.equal(tarifa.get("profissionalId"), profissional.get("id")));
		condicoes.add(criterios.equal(servico.get("id"), tarifa.get("servicoId")));
		return selecionarSe(executados, criterios, condicoes);
	}

	/** Serviço ativo, com o trecho do nome e a categoria pedidos. */
	private static List<Predicate> condicoesDoServico(Root<ServicoDaBuscaJpaEntity> servico,
			CriteriaBuilder criterios, FiltroDaBusca filtro) {
		List<Predicate> condicoes = new ArrayList<>();
		condicoes.add(criterios.isTrue(servico.get("ativo")));
		if (filtro.servico() != null) {
			condicoes.add(contem(criterios, servico.get("nome"), filtro.servico()));
		}
		if (filtro.categoria() != null) {
			condicoes.add(criterios.equal(servico.get("categoria"), filtro.categoria()));
		}
		return condicoes;
	}

	private static List<Predicate> condicoesDaFaixa(Expression<BigDecimal> tarifa, CriteriaBuilder criterios,
			FiltroDaBusca filtro) {
		List<Predicate> condicoes = new ArrayList<>();
		if (filtro.precoMinimo() != null) {
			condicoes.add(criterios.greaterThanOrEqualTo(tarifa, filtro.precoMinimo()));
		}
		if (filtro.precoMaximo() != null) {
			condicoes.add(criterios.lessThanOrEqualTo(tarifa, filtro.precoMaximo()));
		}
		return condicoes;
	}

	private static Subquery<Integer> selecionarSe(Subquery<Integer> subconsulta, CriteriaBuilder criterios,
			List<Predicate> condicoes) {
		return subconsulta.select(criterios.literal(1)).where(condicoes.toArray(Predicate[]::new));
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
