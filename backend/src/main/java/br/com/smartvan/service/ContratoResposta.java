package br.com.smartvan.service;

import br.com.smartvan.dto.ContratoResponseDTO;
import br.com.smartvan.model.AlunoEndereco;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Endereco;
import br.com.smartvan.repository.AlunoEnderecoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//monta a resposta do contrato com o endereço principal do aluno (o motorista precisa dele para avaliar a proposta)
@Component
public class ContratoResposta {

    private final AlunoEnderecoRepository enderecos;

    public ContratoResposta(AlunoEnderecoRepository enderecos) {
        this.enderecos = enderecos;
    }

    /** Chame dentro de uma transação, como {@link ContratoResponseDTO#de}. */
    public ContratoResponseDTO de(Contrato c) {
        return de(List.of(c)).getFirst();
    }

    /** Busca os endereços de todos os alunos numa consulta só. */
    public List<ContratoResponseDTO> de(List<Contrato> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> alunoIds = lista.stream().map(c -> c.getAluno().getId()).distinct().toList();
        Map<Long, Endereco> principais = enderecos.principaisDosAlunos(alunoIds).stream()
                .collect(Collectors.toMap(ae -> ae.getAluno().getId(), AlunoEndereco::getEndereco, (a, b) -> a));
        return lista.stream().map(c -> ContratoResponseDTO.de(c, principais.get(c.getAluno().getId()))).toList();
    }
}
