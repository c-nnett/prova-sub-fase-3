package br.com.fiap.agendabeleza.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AgendaBelezaApplicationTests {

	@Test
	void contextLoads() {
	}

}
