package br.com.fiap.agendabeleza.api.calendario.application.usecase;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;

/**
 * Escolhe a estratégia de cada papel. Exige uma para todo papel já na montagem, para que um papel novo sem
 * estratégia quebre a subida da aplicação, e não a primeira assinatura.
 */
public class TitularesDoCalendario {

	private final Map<PapelNoCalendario, TitularDoCalendario> porPapel = new EnumMap<>(PapelNoCalendario.class);

	public TitularesDoCalendario(Collection<TitularDoCalendario> titulares) {
		titulares.forEach(titular -> porPapel.put(titular.papel(), titular));
		for (PapelNoCalendario papel : PapelNoCalendario.values()) {
			if (!porPapel.containsKey(papel)) {
				throw new IllegalArgumentException("Sem estratégia de calendário para o papel " + papel);
			}
		}
	}

	public TitularDoCalendario doPapel(PapelNoCalendario papel) {
		return porPapel.get(papel);
	}

}
