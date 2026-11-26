package br.com.fiap.agendabeleza.api.busca.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import br.com.fiap.agendabeleza.api.busca.application.port.CatalogoDeEstabelecimentos;
import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import br.com.fiap.agendabeleza.api.busca.domain.FiltroDaBusca;
import br.com.fiap.agendabeleza.api.busca.domain.JanelaDeDisponibilidade;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Pagina;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Paginacao;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Busca pública e paginada de estabelecimentos com filtros combinados (funcionalidade 5). Filtro e paginação são
 * validados antes de consultar o banco. "Hoje", para recusar data no passado, é o dia no fuso padrão dos
 * estabelecimentos, já que a busca abrange estabelecimentos de vários lugares.
 */
public class BuscarEstabelecimentosUseCase {

	private final CatalogoDeEstabelecimentos catalogo;
	private final Clock relogio;

	public BuscarEstabelecimentosUseCase(CatalogoDeEstabelecimentos catalogo, Clock relogio) {
		this.catalogo = catalogo;
		this.relogio = relogio;
	}

	public Pagina<EstabelecimentoEncontrado> executar(Consulta consulta) {
		LocalDate hoje = LocalDate.now(relogio.withZone(Estabelecimento.TIMEZONE_PADRAO));
		JanelaDeDisponibilidade disponibilidade = JanelaDeDisponibilidade.de(consulta.data(), consulta.horaInicio(),
				consulta.horaFim(), hoje);
		FiltroDaBusca filtro = new FiltroDaBusca(consulta.nome(), consulta.cidade(), consulta.bairro(),
				consulta.servico(), consulta.categoria(), consulta.notaMinima(), consulta.precoMinimo(),
				consulta.precoMaximo(), disponibilidade);
		Paginacao paginacao = Paginacao.de(consulta.pagina(), consulta.tamanho());
		OrdemDaBusca ordem = Objects.requireNonNullElse(consulta.ordem(), OrdemDaBusca.PADRAO);
		return catalogo.buscar(filtro, paginacao, ordem);
	}

	/**
	 * Parâmetros da busca, todos opcionais.
	 *
	 * @param data    dia em que o cliente quer ser atendido; obrigatória com {@code horaInicio} ou {@code horaFim}
	 * @param pagina  a partir de zero; nula é a primeira
	 * @param tamanho nulo usa {@link Paginacao#TAMANHO_PADRAO}
	 * @param ordem   nula usa {@link OrdemDaBusca#PADRAO}
	 */
	@SuppressWarnings("java:S107") // Um parâmetro por filtro da rota.
	public record Consulta(String nome, String cidade, String bairro, String servico, CategoriaServico categoria,
			BigDecimal notaMinima, BigDecimal precoMinimo, BigDecimal precoMaximo, LocalDate data,
			LocalTime horaInicio, LocalTime horaFim, Integer pagina, Integer tamanho, OrdemDaBusca ordem) {
	}

}
