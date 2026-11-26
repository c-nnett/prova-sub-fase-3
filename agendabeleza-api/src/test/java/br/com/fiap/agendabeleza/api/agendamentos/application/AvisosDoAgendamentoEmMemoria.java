package br.com.fiap.agendabeleza.api.agendamentos.application;

import java.util.ArrayList;
import java.util.List;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisoDoAgendamento;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisosDoAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.application.ExecucaoSerializadaDireta;

/**
 * Guarda os avisos pedidos e se cada um veio de dentro da execução serializada, que no adaptador real é a mesma
 * transação da gravação do agendamento.
 */
public class AvisosDoAgendamentoEmMemoria implements AvisosDoAgendamento {

	public enum Ocorrencia {
		RESERVADO, CANCELADO, REAGENDADO
	}

	/** @param emailCliente só na reserva; nulo nos demais */
	public record AvisoRegistrado(Ocorrencia ocorrencia, AvisoDoAgendamento aviso, String emailCliente,
			boolean dentroDaExecucao) {
	}

	private final ExecucaoSerializadaDireta execucao;
	private final List<AvisoRegistrado> registrados = new ArrayList<>();

	public AvisosDoAgendamentoEmMemoria(ExecucaoSerializadaDireta execucao) {
		this.execucao = execucao;
	}

	@Override
	public void reservado(AvisoDoAgendamento aviso, String emailCliente) {
		registrar(Ocorrencia.RESERVADO, aviso, emailCliente);
	}

	@Override
	public void cancelado(AvisoDoAgendamento aviso) {
		registrar(Ocorrencia.CANCELADO, aviso, null);
	}

	@Override
	public void reagendado(AvisoDoAgendamento aviso) {
		registrar(Ocorrencia.REAGENDADO, aviso, null);
	}

	public List<AvisoRegistrado> registrados() {
		return List.copyOf(registrados);
	}

	public List<AvisoRegistrado> registrados(Ocorrencia ocorrencia) {
		return registrados.stream().filter(registrado -> registrado.ocorrencia() == ocorrencia).toList();
	}

	private void registrar(Ocorrencia ocorrencia, AvisoDoAgendamento aviso, String emailCliente) {
		registrados.add(new AvisoRegistrado(ocorrencia, aviso, emailCliente, execucao.emExecucao()));
	}

}
