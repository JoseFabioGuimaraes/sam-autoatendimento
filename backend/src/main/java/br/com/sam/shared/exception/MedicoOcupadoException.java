package br.com.sam.shared.exception;

public class MedicoOcupadoException extends RuntimeException {

    public MedicoOcupadoException() {
        super("O médico já possui um paciente em atendimento. Finalize a consulta atual antes de autorizar a entrada de um novo paciente.");
    }

    public MedicoOcupadoException(String message) {
        super(message);
    }
}
