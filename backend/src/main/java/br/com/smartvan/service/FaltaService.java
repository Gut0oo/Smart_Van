package br.com.smartvan.service;

import br.com.smartvan.dto.FaltaRequestDTO;
import br.com.smartvan.dto.FaltaResponseDTO;
import br.com.smartvan.enums.StatusVinculo;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.Falta;
import br.com.smartvan.repository.AlunoRepository;
import br.com.smartvan.repository.FaltaRepository;
import br.com.smartvan.repository.ResponsavelAlunoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

//faltas do aluno: o responsável avisa que o aluno não vai num dia, e a parada sai da rota gerada para esse dia
@Service
public class FaltaService {

    private final FaltaRepository faltas;
    private final AlunoRepository alunos;
    private final ResponsavelAlunoRepository vinculos;
    private final Clock clock;

    public FaltaService(FaltaRepository faltas, AlunoRepository alunos, ResponsavelAlunoRepository vinculos,
                        Clock clock) {
        this.faltas = faltas;
        this.alunos = alunos;
        this.vinculos = vinculos;
        this.clock = clock;
    }

    @Transactional
    public FaltaResponseDTO registrar(Long responsavelId, Long alunoId, FaltaRequestDTO req) {
        //trava o aluno: dois pedidos iguais ao mesmo tempo dão 409 no segundo, e não erro na restrição única
        Aluno aluno = alunos.travar(alunoId).orElseThrow(() -> RegraNegocioException.naoEncontrado("Aluno"));
        conferirResponsavel(responsavelId, alunoId);
        if (req.data().isBefore(LocalDate.now(clock))) {
            throw RegraNegocioException.regra("DATA_PASSADA", "A data da falta não pode ser passada.");
        }
        if (faltas.existsByAlunoIdAndDataFalta(alunoId, req.data())) {
            throw RegraNegocioException.conflito("FALTA_JA_REGISTRADA",
                    "Já existe falta registrada para este aluno nesta data.");
        }
        Falta falta = new Falta();
        falta.setAluno(aluno);
        falta.setDataFalta(req.data());
        falta.setMotivo(req.motivo() == null || req.motivo().isBlank() ? null : req.motivo().trim());
        return FaltaResponseDTO.de(faltas.save(falta));
    }

    //sem mês: as faltas de hoje em diante
    @Transactional(readOnly = true)
    public List<FaltaResponseDTO> listar(Long responsavelId, Long alunoId, YearMonth mes) {
        conferirAluno(responsavelId, alunoId);
        List<Falta> lista = mes == null
                ? faltas.findByAlunoIdAndDataFaltaGreaterThanEqualOrderByDataFalta(alunoId, LocalDate.now(clock))
                : faltas.findByAlunoIdAndDataFaltaBetweenOrderByDataFalta(alunoId, mes.atDay(1), mes.atEndOfMonth());
        return lista.stream().map(FaltaResponseDTO::de).toList();
    }

    @Transactional
    public void remover(Long responsavelId, Long alunoId, LocalDate data) {
        conferirAluno(responsavelId, alunoId);
        Falta falta = faltas.findByAlunoIdAndDataFalta(alunoId, data)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Falta"));
        faltas.delete(falta);
    }

    private void conferirAluno(Long responsavelId, Long alunoId) {
        if (!alunos.existsById(alunoId)) {
            throw RegraNegocioException.naoEncontrado("Aluno");
        }
        conferirResponsavel(responsavelId, alunoId);
    }

    private void conferirResponsavel(Long responsavelId, Long alunoId) {
        if (!vinculos.existsByResponsavelIdAndAlunoIdAndStatusVinculo(responsavelId, alunoId, StatusVinculo.ATIVO)) {
            throw RegraNegocioException.semPermissao();
        }
    }
}
