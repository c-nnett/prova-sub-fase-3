package br.com.fiap.agendabeleza.api.profissionais.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;

public class BloqueioAgendaRepositoryEmMemoria implements BloqueioAgendaRepository {

	private static final Comparator<BloqueioAgenda> POR_INICIO = Comparator.comparing(
			bloqueio -> bloqueio.periodo().inicio());

	private final Map<UUID, BloqueioAgenda> dados = new LinkedHashMap<>();

	@Override
	public BloqueioAgenda salvar(BloqueioAgenda bloqueio) {
		dados.put(bloqueio.id(), bloqueio);
		return bloqueio;
	}

	@Override
	public Optional<BloqueioAgenda> buscarPorId(UUID id) {
		return Optional.ofNullable(dados.get(id));
	}

	@Override
	public void remover(UUID id) {
		dados.remove(id);
	}

	@Override
	public List<BloqueioAgenda> listarNoPeriodo(UUID profissionalId, Intervalo periodo) {
		return dados.values().stream()
				.filter(bloqueio -> bloqueio.pertenceA(profissionalId))
				.filter(bloqueio -> bloqueio.periodo().sobrepoe(periodo))
				.sorted(POR_INICIO)
				.toList();
	}

	@Override
	public List<BloqueioAgenda> listarVigentes(UUID profissionalId, Instant agora) {
		return dados.values().stream()
				.filter(bloqueio -> bloqueio.pertenceA(profissionalId))
				.filter(bloqueio -> bloqueio.periodo().fim().isAfter(agora))
				.sorted(POR_INICIO)
				.toList();
	}

}
