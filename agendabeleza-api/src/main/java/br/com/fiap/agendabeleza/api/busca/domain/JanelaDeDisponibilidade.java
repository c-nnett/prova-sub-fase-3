package br.com.fiap.agendabeleza.api.busca.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Dia e horário em que o cliente quer ser atendido, no fuso do estabelecimento, como as colunas {@code time} de
 * funcionamento e jornada. A busca exige um profissional ativo cuja jornada, o funcionamento do estabelecimento e esta
 * janela tenham algum instante em comum naquele dia da semana. Agendamentos e bloqueios não são descontados: é uma
 * simplificação aceita (planejamento, seção 8), e o horário exato sai de {@code /profissionais/{id}/horarios-livres}.
 *
 * @param inicio início da janela, inclusivo
 * @param fim    fim da janela, exclusivo; nulo vai até o fim do dia
 */
public record JanelaDeDisponibilidade(LocalDate data, LocalTime inicio, LocalTime fim) {

	public static final String HORA_SEM_DATA = "Informe a data para buscar por horário";
	public static final String DATA_NO_PASSADO = "A data da busca não pode estar no passado";
	public static final String JANELA_INVERTIDA = "A hora inicial deve ser anterior à hora final";

	public JanelaDeDisponibilidade {
		if (data == null) {
			throw new RegraNegocioException(HORA_SEM_DATA);
		}
		if (inicio == null) {
			inicio = LocalTime.MIDNIGHT;
		}
		if (fim != null && !inicio.isBefore(fim)) {
			throw new RegraNegocioException(JANELA_INVERTIDA);
		}
	}

	/**
	 * Monta a janela a partir dos parâmetros opcionais da busca.
	 *
	 * @param hoje data atual no fuso dos estabelecimentos
	 * @return {@code null} quando não há data nem horas, ou seja, sem filtro de disponibilidade
	 */
	public static JanelaDeDisponibilidade de(LocalDate data, LocalTime inicio, LocalTime fim, LocalDate hoje) {
		if (data == null && inicio == null && fim == null) {
			return null;
		}
		if (data != null && data.isBefore(hoje)) {
			throw new RegraNegocioException(DATA_NO_PASSADO);
		}
		return new JanelaDeDisponibilidade(data, inicio, fim);
	}

	/** De 1 (segunda) a 7 (domingo), ISO-8601, como as colunas {@code dia_semana}. */
	public int diaDaSemana() {
		return data.getDayOfWeek().getValue();
	}

}
