package br.com.fiap.agendabeleza.api.busca.adapters.in.web;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.agendabeleza.api.busca.adapters.in.web.dto.EstabelecimentoEncontradoResponse;
import br.com.fiap.agendabeleza.api.busca.adapters.in.web.dto.PaginaResponse;
import br.com.fiap.agendabeleza.api.busca.application.usecase.BuscarEstabelecimentosUseCase;
import br.com.fiap.agendabeleza.api.busca.domain.OrdemDaBusca;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Paginacao;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Busca pública de estabelecimentos (funcionalidade 5). Os limites de cada parâmetro respondem 400 aqui; a regra
 * entre parâmetros (faixa de preço invertida) é do domínio e responde 422.
 */
@RestController
@RequestMapping("/api/v1/busca")
@Tag(name = "Busca", description = "Busca de estabelecimentos com filtros combinados e paginação")
class BuscaController {

	private final BuscarEstabelecimentosUseCase buscar;

	BuscaController(BuscarEstabelecimentosUseCase buscar) {
		this.buscar = buscar;
	}

	@GetMapping("/estabelecimentos")
	@Operation(summary = "Buscar estabelecimentos", description = "Estabelecimentos ativos que atendem a todos os "
			+ "filtros informados; filtro ausente não restringe. Nome e serviço são buscados por trecho, cidade e "
			+ "bairro pelo valor exato, sem diferenciar maiúsculas. Serviço, categoria e faixa de preço valem para o "
			+ "mesmo serviço ativo, e o preço é a tarifa de um profissional ativo. Com nota mínima, quem ainda não "
			+ "foi avaliado fica de fora. A ordem MELHOR_AVALIADOS (padrão) põe os sem avaliação no fim. Responde 400 "
			+ "para número, categoria ou ordem malformados, página negativa, tamanho fora de 1 a 50, nota fora de 0 a "
			+ "5 e preço negativo, e 422 se o preço mínimo for maior que o máximo.")
	@SuppressWarnings("java:S107") // Um parâmetro por filtro da rota.
	PaginaResponse<EstabelecimentoEncontradoResponse> buscarEstabelecimentos(
			@Parameter(description = "Trecho do nome do estabelecimento") @RequestParam(required = false)
			String nome,
			@Parameter(description = "Cidade") @RequestParam(required = false) String cidade,
			@Parameter(description = "Bairro") @RequestParam(required = false) String bairro,
			@Parameter(description = "Trecho do nome de um serviço") @RequestParam(required = false)
			String servico,
			@Parameter(description = "Categoria de um serviço") @RequestParam(required = false)
			CategoriaServico categoria,
			@Parameter(description = "Nota média mínima, de 0 a 5") @RequestParam(required = false)
			@DecimalMin("0") @DecimalMax("5") BigDecimal notaMinima,
			@Parameter(description = "Tarifa mínima de um serviço") @RequestParam(required = false)
			@DecimalMin("0") BigDecimal precoMinimo,
			@Parameter(description = "Tarifa máxima de um serviço") @RequestParam(required = false)
			@DecimalMin("0") BigDecimal precoMaximo,
			@Parameter(description = "Página, a partir de zero") @RequestParam(required = false)
			@Min(0) Integer pagina,
			@Parameter(description = "Itens por página, de 1 a 50; padrão 20") @RequestParam(required = false)
			@Min(1) @Max(Paginacao.TAMANHO_MAXIMO) Integer tamanho,
			@Parameter(description = "Ordem do resultado; padrão MELHOR_AVALIADOS") @RequestParam(required = false)
			OrdemDaBusca ordem) {
		return PaginaResponse.de(buscar.executar(new BuscarEstabelecimentosUseCase.Consulta(nome, cidade, bairro,
				servico, categoria, notaMinima, precoMinimo, precoMaximo, null, null, null, pagina, tamanho, ordem)),
				EstabelecimentoEncontradoResponse::de);
	}

}
