package br.com.fiap.agendabeleza.api.calendario.application.usecase;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.calendario.domain.DetalhesDoEvento;
import br.com.fiap.agendabeleza.api.calendario.domain.EventoDoCalendario;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Endereco;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Transforma os agendamentos de um feed em eventos. Vive uma única geração: guarda serviço, estabelecimento e
 * profissional por id, porque os agendamentos de um titular repetem os mesmos cadastros, e assim cada um é lido do
 * banco uma vez só.
 */
class MontagemDosEventos {

	private static final Locale PORTUGUES_DO_BRASIL = Locale.forLanguageTag("pt-BR");

	private final TitularDoCalendario titular;
	private final CadastroLidoUmaVez<Servico> servicos;
	private final CadastroLidoUmaVez<Estabelecimento> estabelecimentos;
	private final CadastroLidoUmaVez<Profissional> profissionais;

	MontagemDosEventos(TitularDoCalendario titular, ServicoRepository servicos,
			EstabelecimentoRepository estabelecimentos, ProfissionalRepository profissionais) {
		this.titular = titular;
		this.servicos = new CadastroLidoUmaVez<>("Serviço", servicos::buscarPorId);
		this.estabelecimentos = new CadastroLidoUmaVez<>("Estabelecimento", estabelecimentos::buscarPorId);
		this.profissionais = new CadastroLidoUmaVez<>("Profissional", profissionais::buscarPorId);
	}

	List<EventoDoCalendario> eventos(List<Agendamento> agendamentos) {
		return agendamentos.stream()
				.flatMap(agendamento -> EventoDoCalendario.doAgendamento(agendamento, detalhes(agendamento)).stream())
				.toList();
	}

	private DetalhesDoEvento detalhes(Agendamento agendamento) {
		Servico servico = servicos.obter(agendamento.servicoId());
		Estabelecimento estabelecimento = estabelecimentos.obter(agendamento.estabelecimentoId());
		Profissional profissional = profissionais.obter(agendamento.profissionalId());
		String descricao = String.join("\n",
				"Serviço: " + servico.nome(),
				"Profissional: " + profissional.nome(),
				"Estabelecimento: " + estabelecimento.nome(),
				"Preço: " + emReais(agendamento.preco()));
		return new DetalhesDoEvento(titular.resumoDoEvento(servico, estabelecimento),
				enderecoEmUmaLinha(estabelecimento.endereco()), descricao);
	}

	/** Ex.: {@code Rua Augusta, 100, sala 2 - Consolação, São Paulo/SP}. */
	private static String enderecoEmUmaLinha(Endereco endereco) {
		String complemento = endereco.complemento() == null ? "" : ", " + endereco.complemento();
		return endereco.logradouro() + ", " + endereco.numero() + complemento + " - " + endereco.bairro() + ", "
				+ endereco.cidade() + "/" + endereco.uf();
	}

	private static String emReais(BigDecimal valor) {
		return String.format(PORTUGUES_DO_BRASIL, "R$ %.2f", valor);
	}

	/**
	 * Memória de uma consulta por id. A ausência não deveria acontecer (as chaves estrangeiras de {@code agendamento}
	 * garantem os cadastros) e, se acontecer, é falha interna, não 404.
	 */
	private static final class CadastroLidoUmaVez<T> {

		private final String nomeDoCadastro;
		private final Function<UUID, Optional<T>> consulta;
		private final Map<UUID, T> lidos = new HashMap<>();

		private CadastroLidoUmaVez(String nomeDoCadastro, Function<UUID, Optional<T>> consulta) {
			this.nomeDoCadastro = nomeDoCadastro;
			this.consulta = consulta;
		}

		private T obter(UUID id) {
			return lidos.computeIfAbsent(id, idNaoLido -> consulta.apply(idNaoLido).orElseThrow(
					() -> new IllegalStateException(nomeDoCadastro + " " + idNaoLido + " do agendamento sumiu")));
		}

	}

}
