package br.com.sam.medico.service;

import br.com.sam.medico.dto.MedicoResponse;
import br.com.sam.medico.repository.MedicoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private final MedicoRepository medicoRepository;

    @Transactional(readOnly = true)
    public Page<MedicoResponse> listarAtivos(Pageable pageable) {
        return medicoRepository.findAllAtivos(pageable)
                .map(MedicoResponse::from);
    }

    @Transactional(readOnly = true)
    public MedicoResponse buscarPorId(String id) {
        return medicoRepository.findById(id)
                .map(MedicoResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado: " + id));
    }
}
