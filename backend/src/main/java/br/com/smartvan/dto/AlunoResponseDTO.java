package br.com.smartvan.dto;

import br.com.smartvan.enums.Parentesco;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.Endereco;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/** Objeto {@code Aluno} do contrato. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlunoResponseDTO(Long id, String nome, LocalDate dataNascimento, String serie, Parentesco parentesco,
                               EscolaResumoResponseDTO escola, EnderecoResponseDTO endereco) {

    public static AlunoResponseDTO de(Aluno a, Parentesco parentesco, Endereco endereco) {
        return new AlunoResponseDTO(a.getId(), a.getNome(), a.getDataNascimento(), a.getSerie(), parentesco,
                a.getEscola() == null ? null : EscolaResumoResponseDTO.de(a.getEscola()),
                endereco == null ? null : EnderecoResponseDTO.de(endereco));
    }
}
