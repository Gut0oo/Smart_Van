package br.com.smartvan.service;

import br.com.smartvan.dto.EscolaResponseDTO;
import br.com.smartvan.repository.EscolaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class EscolaService {

    private final EscolaRepository escolas;

    public EscolaService(EscolaRepository escolas) {
        this.escolas = escolas;
    }

    @Transactional(readOnly = true)
    public List<EscolaResponseDTO> listar(String cidade, String nome) {
        return escolas.buscar(padrao(cidade), padrao(nome)).stream().map(EscolaResponseDTO::de).toList();
    }

    private static String padrao(String texto) {
        return "%" + (texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT)) + "%";
    }
}
