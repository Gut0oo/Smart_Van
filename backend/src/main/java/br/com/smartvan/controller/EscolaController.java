package br.com.smartvan.controller;

import br.com.smartvan.dto.EscolaResponseDTO;
import br.com.smartvan.service.EscolaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//seção 1 do contrato; rota pública (ver InterceptorUsuarioLogado)
@RestController
@RequestMapping("/escolas")
public class EscolaController {

    private final EscolaService service;

    public EscolaController(EscolaService service) {
        this.service = service;
    }

    @GetMapping
    public List<EscolaResponseDTO> listar(@RequestParam(required = false) String cidade,
                                          @RequestParam(required = false) String nome) {
        return service.listar(cidade, nome);
    }
}
