package br.com.fiap.agendabeleza.carga;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feeder das reservas em horários distintos: cada registro é um par profissional e início que nenhum outro usa. A
 * ordem alterna os profissionais a cada reserva, para que a trava por profissional ({@code ExecucaoSerializada}) não
 * serialize a carga inteira num único profissional.
 */
public final class HorariosDistintos {

	/** Fuso dos estabelecimentos (America/Sao_Paulo), fixo em UTC-3 desde 2019. */
	private static final ZoneOffset FUSO_DO_SALAO = ZoneOffset.ofHours(-3);

	/**
	 * Sempre com segundos: o {@code toString()} do {@code OffsetDateTime} os omite quando são zero
	 * ({@code 10:00-03:00}), e a API recusa esse formato com 400.
	 */
	private static final DateTimeFormatter DATA_HORA_COM_OFFSET =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

	private HorariosDistintos() {
	}

	/** Início no formato que a API aceita, no fuso do salão. */
	public static String inicio(LocalDate dia, LocalTime horario) {
		return dia.atTime(horario).atOffset(FUSO_DO_SALAO).format(DATA_HORA_COM_OFFSET);
	}

	/** @param quantidade reservas necessárias; dias são acrescentados até haver horários para todas */
	public static Iterator<Map<String, Object>> para(SalaoPreparado salao, int quantidade) {
		List<LocalTime> inicios = iniciosDoDia();
		List<Map<String, Object>> registros = new ArrayList<>(quantidade);
		for (LocalDate dia = salao.primeiroDia(); registros.size() < quantidade; dia = dia.plusDays(1)) {
			for (LocalTime inicio : inicios) {
				for (UUID profissionalId : salao.profissionais()) {
					if (registros.size() == quantidade) {
						return registros.iterator();
					}
					registros.add(Map.of("profissionalId", profissionalId.toString(),
							"inicio", inicio(dia, inicio)));
				}
			}
		}
		return registros.iterator();
	}

	/** Inícios em passos da duração do serviço, todos terminando até o fechamento. */
	private static List<LocalTime> iniciosDoDia() {
		Duration passo = Duration.ofMinutes(PreparacaoDaCarga.DURACAO_DO_SERVICO_EM_MINUTOS);
		List<LocalTime> inicios = new ArrayList<>();
		for (LocalTime inicio = PreparacaoDaCarga.ABERTURA; !inicio.plus(passo)
				.isAfter(PreparacaoDaCarga.FECHAMENTO); inicio = inicio.plus(passo)) {
			inicios.add(inicio);
		}
		return inicios;
	}

}
