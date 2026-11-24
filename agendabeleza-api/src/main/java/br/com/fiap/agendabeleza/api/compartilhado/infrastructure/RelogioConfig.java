package br.com.fiap.agendabeleza.api.compartilhado.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio usado pelos casos de uso que dependem do instante atual. Os testes trocam por {@link Clock#fixed}.
 */
@Configuration(proxyBeanMethods = false)
class RelogioConfig {

	@Bean
	Clock relogio() {
		return Clock.systemUTC();
	}

}
