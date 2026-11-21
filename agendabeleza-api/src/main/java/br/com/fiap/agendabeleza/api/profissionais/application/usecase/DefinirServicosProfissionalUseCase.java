package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;

/**
 * Substitui os serviços que o profissional executa e as tarifas. Cada serviço precisa ser do mesmo
 * estabelecimento e estar ativo. Tarifa não informada assume o preço base do serviço. Gestor ou o próprio
 * profissional.
 */
public class DefinirServicosProfissionalUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;
	private final ServicoRepository servicos;

	public DefinirServicosProfissionalUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais, ServicoRepository servicos) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
		this.servicos = servicos;
	}

	public Profissional executar(Comando comando) {
		Profissional profissional = ProfissionalAlteravel.obter(profissionais, estabelecimentos,
				comando.profissionalId(), comando.usuarioId()).profissional();
		List<Item> itens = comando.servicos() == null ? List.of() : comando.servicos();
		List<ServicoProfissional> novos = itens.stream()
				.map(item -> paraServicoProfissional(profissional, item))
				.toList();
		profissional.definirServicos(novos);
		return profissionais.salvar(profissional);
	}

	private ServicoProfissional paraServicoProfissional(Profissional profissional, Item item) {
		if (item.servicoId() == null) {
			throw new RegraNegocioException("servicoId é obrigatório");
		}
		Servico servico = servicos.buscarPorId(item.servicoId())
				.filter(encontrado -> encontrado.pertenceA(profissional.estabelecimentoId()))
				.orElseThrow(() -> new RegraNegocioException(
						"Serviço " + item.servicoId() + " não encontrado no estabelecimento"));
		if (!servico.ativo()) {
			throw new RegraNegocioException("Serviço " + servico.id() + " está inativo");
		}
		BigDecimal tarifa = item.tarifa() == null ? servico.precoBase() : item.tarifa();
		return new ServicoProfissional(servico.id(), tarifa);
	}

	public record Comando(UUID profissionalId, UUID usuarioId, List<Item> servicos) {
	}

	/** Serviço a executar e tarifa opcional. */
	public record Item(UUID servicoId, BigDecimal tarifa) {
	}

}
