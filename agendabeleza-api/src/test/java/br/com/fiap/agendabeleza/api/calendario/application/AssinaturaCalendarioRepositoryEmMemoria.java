package br.com.fiap.agendabeleza.api.calendario.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.fiap.agendabeleza.api.calendario.application.port.AssinaturaCalendarioRepository;
import br.com.fiap.agendabeleza.api.calendario.domain.AssinaturaDoCalendario;

/**
 * Imita o adaptador JPA: uma assinatura por papel e titular, e a nova substitui a anterior.
 */
public class AssinaturaCalendarioRepositoryEmMemoria implements AssinaturaCalendarioRepository {

	private final List<AssinaturaDoCalendario> dados = new ArrayList<>();

	@Override
	public void salvar(AssinaturaDoCalendario assinatura) {
		dados.removeIf(gravada -> gravada.papel() == assinatura.papel()
				&& gravada.titularId().equals(assinatura.titularId()));
		dados.add(assinatura);
	}

	@Override
	public Optional<AssinaturaDoCalendario> buscarPorTokenHash(String tokenHash) {
		return dados.stream().filter(gravada -> gravada.tokenHash().equals(tokenHash)).findFirst();
	}

	public List<AssinaturaDoCalendario> todas() {
		return List.copyOf(dados);
	}

}
