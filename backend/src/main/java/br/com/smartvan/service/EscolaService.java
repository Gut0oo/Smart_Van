package br.com.smartvan.service;

import br.com.smartvan.dto.EscolaRequestDTO;
import br.com.smartvan.dto.EscolaResponseDTO;
import br.com.smartvan.model.Escola;
import br.com.smartvan.repository.EscolaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class EscolaService {

    private final EscolaRepository escolas;
    private final EnderecoService enderecoService;

    public EscolaService(EscolaRepository escolas, EnderecoService enderecoService) {
        this.escolas = escolas;
        this.enderecoService = enderecoService;
    }

    @Transactional
    public EscolaResponseDTO cadastrar(EscolaRequestDTO req) {
        Escola escola = new Escola();
        escola.setNome(req.nome().trim());
        escola.setTelefone(req.telefone());
        escola.setEndereco(enderecoService.criar(req.endereco()));
        return EscolaResponseDTO.de(escolas.save(escola));
    }

    @Transactional(readOnly = true)
    public List<EscolaResponseDTO> listar(String cidade, String nome) {
        return escolas.buscar(padrao(cidade), padrao(nome)).stream().map(EscolaResponseDTO::de).toList();
    }

    private static String padrao(String texto) {
        return "%" + (texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT)) + "%";
    }
}
