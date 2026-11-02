package br.com.smartvan.exception;

import org.springframework.http.HttpStatus;

/** 422 ROTA_SEM_ALUNOS. Tipo próprio para a transação da geração confirmar a remoção da rota antiga mesmo com o erro. */
public class RotaSemAlunosException extends RegraNegocioException {

    public RotaSemAlunosException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "ROTA_SEM_ALUNOS",
                "Nenhum aluno para esta rota (contratos ativos do atendimento, menos faltas).");
    }
}
