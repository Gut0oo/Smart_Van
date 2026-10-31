package br.com.smartvan.service;

import br.com.smartvan.dto.AlunoRequestDTO;
import br.com.smartvan.dto.AlunoResponseDTO;
import br.com.smartvan.enums.StatusVinculo;
import br.com.smartvan.enums.TipoEndereco;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.AlunoEndereco;
import br.com.smartvan.model.Endereco;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.ResponsavelAluno;
import br.com.smartvan.repository.AlunoEnderecoRepository;
import br.com.smartvan.repository.AlunoRepository;
import br.com.smartvan.repository.EscolaRepository;
import br.com.smartvan.repository.ResponsavelAlunoRepository;
import br.com.smartvan.repository.ResponsavelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlunoService {

    private final AlunoRepository alunos;
    private final EscolaRepository escolas;
    private final ResponsavelRepository responsaveis;
    private final AlunoEnderecoRepository alunosEnderecos;
    private final ResponsavelAlunoRepository vinculos;
    private final EnderecoService enderecoService;

    public AlunoService(AlunoRepository alunos, EscolaRepository escolas, ResponsavelRepository responsaveis,
                        AlunoEnderecoRepository alunosEnderecos, ResponsavelAlunoRepository vinculos,
                        EnderecoService enderecoService) {
        this.alunos = alunos;
        this.escolas = escolas;
        this.responsaveis = responsaveis;
        this.alunosEnderecos = alunosEnderecos;
        this.vinculos = vinculos;
        this.enderecoService = enderecoService;
    }

    //grava alunos, alunos_enderecos (RESIDENCIAL, principal) e responsaveis_alunos (ATIVO, acompanha rota)
    @Transactional
    public AlunoResponseDTO cadastrar(Long responsavelId, AlunoRequestDTO req) {
        Responsavel responsavel = responsaveis.findById(responsavelId)
                .orElseThrow(RegraNegocioException::semPermissao);
        Escola escola = escolas.findById(req.escolaId())
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Escola"));

        Endereco endereco = enderecoService.criar(req.endereco());

        Aluno aluno = new Aluno();
        aluno.setNome(req.nome().trim());
        aluno.setDataNascimento(req.dataNascimento());
        aluno.setSerie(req.serie());
        aluno.setEscola(escola);
        aluno = alunos.save(aluno);

        AlunoEndereco alunoEndereco = new AlunoEndereco();
        alunoEndereco.setAluno(aluno);
        alunoEndereco.setEndereco(endereco);
        alunoEndereco.setTipo(TipoEndereco.RESIDENCIAL);
        alunoEndereco.setPrincipal(true);
        alunosEnderecos.save(alunoEndereco);

        ResponsavelAluno vinculo = new ResponsavelAluno();
        vinculo.setResponsavel(responsavel);
        vinculo.setAluno(aluno);
        vinculo.setParentesco(req.parentesco());
        vinculo.setStatusVinculo(StatusVinculo.ATIVO);
        vinculo.setAcompanhaRota(true);
        vinculos.save(vinculo);

        return AlunoResponseDTO.de(aluno, req.parentesco(), endereco);
    }

    @Transactional(readOnly = true)
    public List<AlunoResponseDTO> listar(Long responsavelId) {
        List<ResponsavelAluno> lista = vinculos.ativosDoResponsavel(responsavelId);
        if (lista.isEmpty()) {
            return List.of();
        }
        List<Long> ids = lista.stream().map(v -> v.getAluno().getId()).toList();
        Map<Long, Endereco> principais = alunosEnderecos.principaisDosAlunos(ids).stream()
                .collect(Collectors.toMap(ae -> ae.getAluno().getId(), AlunoEndereco::getEndereco, (a, b) -> a));
        return lista.stream()
                .map(v -> AlunoResponseDTO.de(v.getAluno(), v.getParentesco(), principais.get(v.getAluno().getId())))
                .toList();
    }
}
