package br.com.fiap.agendabeleza.api.avaliacoes.adapters.out.persistence;

import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;

/** Projeção da soma e da contagem das notas, convertida em {@link NotaMedia} pelo domínio. */
record SomaDasNotas(Long soma, Long total) {

	NotaMedia paraNotaMedia() {
		return NotaMedia.de(soma, Math.toIntExact(total));
	}

}
