package br.com.fiap.agendabeleza.api.busca.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.agendabeleza.api.busca.application.port.CatalogoDeEstabelecimentos;
import br.com.fiap.agendabeleza.api.busca.application.usecase.BuscarEstabelecimentosUseCase;

/** Registra os casos de uso do módulo de busca como beans (arquitetura, seção 4.2). */
@Configuration(proxyBeanMethods = false)
class BuscaConfig {

	@Bean
	BuscarEstabelecimentosUseCase buscarEstabelecimentosUseCase(CatalogoDeEstabelecimentos catalogo,
			Clock relogio) {
		return new BuscarEstabelecimentosUseCase(catalogo, relogio);
	}

}
