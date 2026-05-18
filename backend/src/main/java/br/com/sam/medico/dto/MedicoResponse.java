package br.com.sam.medico.dto;

import br.com.sam.medico.model.Medico;

public record MedicoResponse(
        String id,
        String nomeCompleto,
        String crm,
        String especialidade,
        String email
) {
    public static MedicoResponse from(Medico medico) {
        return new MedicoResponse(
                medico.getId(),
                medico.getNomeCompleto(),
                medico.getCrm(),
                medico.getEspecialidade(),
                medico.getUsuario().getEmail()
        );
    }
}
