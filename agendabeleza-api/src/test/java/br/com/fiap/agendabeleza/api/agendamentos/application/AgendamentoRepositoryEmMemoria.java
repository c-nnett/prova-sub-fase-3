package br.com.fiap.agendabeleza.api.agendamentos.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.FiltroDaAgenda;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.AlteracaoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;

/**
 * Imita o adaptador JPA: recusa sobreposição ativa do mesmo profissional, como a restrição
 * {@code agendamento_sem_conflito}, e devolve o agregado reconstituído, com a versão incrementada.
 */
public class AgendamentoRepositoryEmMemoria implements AgendamentoRepository {

	private final Map<UUID, Agendamento> dados = new LinkedHashMap<>();
	private final List<AlteracaoAgendamento> historico = new ArrayList<>();

	@Override
	public Agendamento salvar(Agendamento agendamento) {
		if (agendamento.ocupaHorario() && dados.values().stream().anyMatch(outro -> conflita(outro, agendamento))) {
			throw new HorarioIndisponivelException();
		}
		long versao = agendamento.versao() == null ? 0 : agendamento.versao() + 1;
		Agendamento gravado = Agendamento.reconstituir(agendamento.id(), agendamento.clienteId(),
				agendamento.estabelecimentoId(), agendamento.profissionalId(), agendamento.servicoId(),
				agendamento.inicio(), agendamento.fim(), agendamento.status(), agendamento.preco(),
				agendamento.observacoes(), versao);
		dados.put(gravado.id(), gravado);
		historico.addAll(agendamento.alteracoesPendentes());
		return gravado;
	}

	@Override
	public Optional<Agendamento> buscarPorId(UUID id) {
		return Optional.ofNullable(dados.get(id)).map(this::copia);
	}

	@Override
	public List<Intervalo> ocupacoesDoProfissional(UUID profissionalId, Intervalo periodo) {
		return dados.values().stream()
				.filter(agendamento -> agendamento.profissionalId().equals(profissionalId))
				.filter(Agendamento::ocupaHorario)
				.map(Agendamento::intervalo)
				.filter(intervalo -> intervalo.sobrepoe(periodo))
				.sorted(Comparator.comparing(Intervalo::inicio))
				.toList();
	}

	@Override
	public List<Agendamento> listarDoCliente(UUID clienteId) {
		return dados.values().stream()
				.filter(agendamento -> agendamento.pertenceAoCliente(clienteId))
				.sorted(Comparator.comparing(Agendamento::inicio).reversed())
				.map(this::copia)
				.toList();
	}

	/** Mesmos critérios das especificações do adaptador JPA: período pelo início, status e profissional opcionais. */
	@Override
	public List<Agendamento> listarDoEstabelecimento(FiltroDaAgenda filtro) {
		return dados.values().stream()
				.filter(agendamento -> agendamento.estabelecimentoId().equals(filtro.estabelecimentoId()))
				.filter(agendamento -> iniciaNoPeriodo(agendamento, filtro.periodo()))
				.filter(agendamento -> filtro.status().isEmpty() || filtro.status().contains(agendamento.status()))
				.filter(agendamento -> filtro.profissionalId() == null
						|| filtro.profissionalId().equals(agendamento.profissionalId()))
				.sorted(Comparator.comparing(Agendamento::inicio))
				.map(this::copia)
				.toList();
	}

	public List<AlteracaoAgendamento> historico() {
		return List.copyOf(historico);
	}

	public int total() {
		return dados.size();
	}

	private static boolean iniciaNoPeriodo(Agendamento agendamento, Intervalo periodo) {
		return !agendamento.inicio().isBefore(periodo.inicio()) && agendamento.inicio().isBefore(periodo.fim());
	}

	private static boolean conflita(Agendamento gravado, Agendamento novo) {
		return !gravado.id().equals(novo.id())
				&& gravado.ocupaHorario()
				&& gravado.profissionalId().equals(novo.profissionalId())
				&& gravado.intervalo().sobrepoe(novo.intervalo());
	}

	/** Uma instância nova a cada leitura, como o adaptador JPA. */
	private Agendamento copia(Agendamento origem) {
		return Agendamento.reconstituir(origem.id(), origem.clienteId(), origem.estabelecimentoId(),
				origem.profissionalId(), origem.servicoId(), origem.inicio(), origem.fim(), origem.status(),
				origem.preco(), origem.observacoes(), origem.versao());
	}

}
