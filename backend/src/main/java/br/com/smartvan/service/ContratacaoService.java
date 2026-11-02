package br.com.smartvan.service;

import br.com.smartvan.dto.AtendimentoBuscaResponseDTO;
import br.com.smartvan.dto.AtendimentoLotacaoResponseDTO;
import br.com.smartvan.dto.ContratoResponseDTO;
import br.com.smartvan.dto.MotoristaBuscaResponseDTO;
import br.com.smartvan.dto.MotoristaPerfilPublicoResponseDTO;
import br.com.smartvan.dto.PropostaRequestDTO;
import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.StatusMotorista;
import br.com.smartvan.enums.StatusVinculo;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.event.ContratoAtivadoEvent;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.Veiculo;
import br.com.smartvan.repository.AlunoRepository;
import br.com.smartvan.repository.AtendimentoRepository;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.repository.MotoristaRepository;
import br.com.smartvan.repository.ResponsavelAlunoRepository;
import br.com.smartvan.repository.ResponsavelRepository;
import br.com.smartvan.repository.VeiculoRepository;
import br.com.smartvan.security.UsuarioLogado;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

//busca de motoristas e ciclo do contrato: proposta, aceite, recusa e consultas
@Service
public class ContratacaoService {

    private static final int TOLERANCIA_HORARIO_MINUTOS = 30;
    private static final LocalTime ULTIMO_HORARIO = LocalTime.of(23, 59, 59);

    private final AtendimentoRepository atendimentos;
    private final ContratoRepository contratos;
    private final VeiculoRepository veiculos;
    private final MotoristaRepository motoristas;
    private final AlunoRepository alunos;
    private final ResponsavelRepository responsaveis;
    private final ResponsavelAlunoRepository vinculos;
    private final NotificacaoService notificacoes;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public ContratacaoService(AtendimentoRepository atendimentos, ContratoRepository contratos,
                              VeiculoRepository veiculos, MotoristaRepository motoristas, AlunoRepository alunos,
                              ResponsavelRepository responsaveis, ResponsavelAlunoRepository vinculos,
                              NotificacaoService notificacoes, ApplicationEventPublisher eventos, Clock clock) {
        this.atendimentos = atendimentos;
        this.contratos = contratos;
        this.veiculos = veiculos;
        this.motoristas = motoristas;
        this.alunos = alunos;
        this.responsaveis = responsaveis;
        this.vinculos = vinculos;
        this.notificacoes = notificacoes;
        this.eventos = eventos;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- busca e perfil

    @Transactional(readOnly = true)
    public List<MotoristaBuscaResponseDTO> buscar(Long responsavelId, Long escolaId, LocalTime horarioAula,
                                                  String cidade, String nome) {
        //sem escola, só a pesquisa de um motorista pelo nome: a lista geral sai pelos filtros
        boolean soPeloNome = escolaId == null;
        if (soPeloNome && (nome == null || nome.isBlank())) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "VALIDACAO",
                    "Informe a escola ou o nome do motorista.");
        }
        exigirAluno(responsavelId);
        LocalTime inicioDe = inicioDaJanela(horarioAula);
        LocalTime inicioAte = fimDaJanela(horarioAula);
        List<Atendimento> encontrados = soPeloNome
                ? atendimentos.buscarEmTodasAsEscolas(inicioDe, inicioAte, padrao(cidade), padrao(nome))
                : atendimentos.buscar(escolaId, inicioDe, inicioAte, padrao(cidade), padrao(nome));
        if (encontrados.isEmpty()) {
            return List.of();
        }

        //veículos e contagens de todos em duas consultas, em vez de duas por atendimento
        Map<Long, Veiculo> maiores = maioresVeiculos(encontrados.stream().map(a -> a.getMotorista().getId()).toList());
        Map<Long, Long> ativos = ativosPorAtendimento(encontrados.stream().map(Atendimento::getId).toList());

        //na pesquisa pelo nome, o atendimento lotado aparece com 0 vagas para o app mostrar "sem vagas"
        Map<Motorista, List<AtendimentoBuscaResponseDTO>> resultado = new LinkedHashMap<>();
        for (Atendimento a : encontrados) {
            int vagas = vagasRestantes(capacidade(maiores.get(a.getMotorista().getId())),
                    ativos.getOrDefault(a.getId(), 0L));
            if (vagas > 0 || soPeloNome) {
                resultado.computeIfAbsent(a.getMotorista(), m -> new ArrayList<>())
                        .add(AtendimentoBuscaResponseDTO.de(a, vagas));
            }
        }
        return resultado.entrySet().stream()
                .map(e -> MotoristaBuscaResponseDTO.de(e.getKey(), maiores.get(e.getKey().getId()), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MotoristaPerfilPublicoResponseDTO perfil(Long responsavelId, Long motoristaId) {
        exigirAluno(responsavelId);
        Motorista m = motoristas.findById(motoristaId)
                .filter(x -> x.getStatusValidacao() == StatusMotorista.VALIDADO)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Motorista"));

        Veiculo maior = maioresVeiculos(List.of(motoristaId)).get(motoristaId);
        List<Atendimento> doMotorista = atendimentos.doMotorista(motoristaId);
        Map<Long, Long> ativos = doMotorista.isEmpty() ? Map.of()
                : ativosPorAtendimento(doMotorista.stream().map(Atendimento::getId).toList());

        int capacidade = capacidade(maior);
        List<AtendimentoLotacaoResponseDTO> lotacao = doMotorista.stream()
                .map(a -> {
                    long contratosAtivos = ativos.getOrDefault(a.getId(), 0L);
                    return AtendimentoLotacaoResponseDTO.de(a, contratosAtivos, capacidade,
                            vagasRestantes(capacidade, contratosAtivos));
                })
                .toList();
        return MotoristaPerfilPublicoResponseDTO.de(m, maior, lotacao, contratos.contarClientes(motoristaId));
    }

    private void exigirAluno(Long responsavelId) {
        if (!vinculos.existsByResponsavelIdAndStatusVinculo(responsavelId, StatusVinculo.ATIVO)) {
            throw RegraNegocioException.regra("ALUNO_OBRIGATORIO",
                    "Cadastre ao menos um aluno antes de buscar motoristas.");
        }
    }

    //a janela de tolerância não atravessa a meia-noite
    private static LocalTime inicioDaJanela(LocalTime horarioAula) {
        if (horarioAula == null || horarioAula.isBefore(LocalTime.MIN.plusMinutes(TOLERANCIA_HORARIO_MINUTOS))) {
            return LocalTime.MIN;
        }
        return horarioAula.minusMinutes(TOLERANCIA_HORARIO_MINUTOS);
    }

    private static LocalTime fimDaJanela(LocalTime horarioAula) {
        if (horarioAula == null || horarioAula.isAfter(ULTIMO_HORARIO.minusMinutes(TOLERANCIA_HORARIO_MINUTOS))) {
            return ULTIMO_HORARIO;
        }
        return horarioAula.plusMinutes(TOLERANCIA_HORARIO_MINUTOS);
    }

    private static String padrao(String texto) {
        return "%" + (texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    private Map<Long, Veiculo> maioresVeiculos(Collection<Long> motoristaIds) {
        Map<Long, Veiculo> maiores = new HashMap<>();
        for (Veiculo v : veiculos.dosMotoristasPorCapacidade(motoristaIds)) {
            maiores.putIfAbsent(v.getMotorista().getId(), v);
        }
        return maiores;
    }

    private Map<Long, Long> ativosPorAtendimento(Collection<Long> atendimentoIds) {
        Map<Long, Long> ativos = new HashMap<>();
        for (Object[] linha : contratos.contarAtivosPorAtendimento(atendimentoIds)) {
            ativos.put((Long) linha[0], (Long) linha[1]);
        }
        return ativos;
    }

    private static int capacidade(Veiculo maior) {
        return maior == null ? 0 : maior.getCapacidade();
    }

    private static int vagasRestantes(int capacidade, long contratosAtivos) {
        return (int) Math.max(0, capacidade - contratosAtivos);
    }

    // ---------------------------------------------------------------- proposta, aceite e recusa

    @Transactional
    public ContratoResponseDTO propor(Long responsavelId, PropostaRequestDTO req) {
        Aluno aluno = alunos.findById(req.alunoId()).orElseThrow(() -> RegraNegocioException.naoEncontrado("Aluno"));
        if (!vinculos.existsByResponsavelIdAndAlunoIdAndStatusVinculo(responsavelId, aluno.getId(), StatusVinculo.ATIVO)) {
            throw RegraNegocioException.semPermissao();
        }
        Atendimento atendimento = atendimentos.travar(req.atendimentoId())
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Atendimento"));
        if (aluno.getEscola() == null || !aluno.getEscola().getId().equals(atendimento.getEscola().getId())) {
            throw RegraNegocioException.regra("ALUNO_DE_OUTRA_ESCOLA",
                    "O aluno não estuda na escola deste atendimento.");
        }
        Motorista motorista = atendimento.getMotorista();
        if (motorista.getStatusValidacao() != StatusMotorista.VALIDADO) {
            throw RegraNegocioException.regra("MOTORISTA_NAO_VALIDADO", "O motorista ainda não foi validado.");
        }
        exigirVaga(atendimento);
        //sempre depois do atendimento, na mesma ordem do aceite: duas travas nunca se esperam em círculo
        alunos.travar(aluno.getId());
        if (contratos.existsByAlunoIdAndMotoristaIdAndStatus(aluno.getId(), motorista.getId(), StatusContrato.PENDENTE)) {
            throw RegraNegocioException.conflito("PROPOSTA_DUPLICADA",
                    "Já existe uma proposta pendente para este aluno com este motorista.");
        }
        exigirAlunoSemContratoAtivo(aluno.getId());
        Responsavel responsavel = responsaveis.findById(responsavelId)
                .orElseThrow(RegraNegocioException::semPermissao);

        Contrato c = new Contrato();
        c.setResponsavel(responsavel);
        c.setMotorista(motorista);
        c.setAluno(aluno);
        c.setAtendimento(atendimento);
        c.setValorMensal(req.valorMensal());
        c.setDataInicio(LocalDate.now(clock));
        c.setStatus(StatusContrato.PENDENTE);
        c = contratos.save(c);

        notificacoes.notificar(motorista.getId(), TipoNotificacao.PROPOSTA_RECEBIDA, "Nova proposta de contrato",
                responsavel.getNome() + " enviou uma proposta para o aluno " + aluno.getNome() + ".", c.getId());
        return ContratoResponseDTO.de(c);
    }

    @Transactional
    public ContratoResponseDTO aceitar(Long motoristaId, Long contratoId) {
        Contrato c = travarDoMotorista(motoristaId, contratoId);
        exigirPendente(c);
        Atendimento atendimento = atendimentos.travar(c.getAtendimento().getId())
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Atendimento"));
        exigirVaga(atendimento);
        alunos.travar(c.getAluno().getId());
        exigirAlunoSemContratoAtivo(c.getAluno().getId());

        c.setStatus(StatusContrato.ATIVO);
        c.setDataInicio(LocalDate.now(clock));
        contratos.saveAndFlush(c);

        notificacoes.notificar(c.getResponsavel().getId(), TipoNotificacao.PROPOSTA_ACEITA, "Proposta aceita",
                c.getMotorista().getNome() + " aceitou a proposta para " + c.getAluno().getNome() + ".", c.getId());
        eventos.publishEvent(new ContratoAtivadoEvent(c.getId()));
        return ContratoResponseDTO.de(c);
    }

    @Transactional
    public ContratoResponseDTO recusar(Long motoristaId, Long contratoId) {
        Contrato c = travarDoMotorista(motoristaId, contratoId);
        exigirPendente(c);
        c.setStatus(StatusContrato.RECUSADO);
        notificacoes.notificar(c.getResponsavel().getId(), TipoNotificacao.PROPOSTA_RECUSADA, "Proposta recusada",
                c.getMotorista().getNome() + " recusou a proposta para " + c.getAluno().getNome() + ".", c.getId());
        return ContratoResponseDTO.de(c);
    }

    private Contrato travarDoMotorista(Long motoristaId, Long contratoId) {
        Contrato c = contratos.travar(contratoId).orElseThrow(() -> RegraNegocioException.naoEncontrado("Contrato"));
        if (!c.getMotorista().getId().equals(motoristaId)) {
            throw RegraNegocioException.semPermissao();
        }
        return c;
    }

    private static void exigirPendente(Contrato c) {
        if (c.getStatus() != StatusContrato.PENDENTE) {
            throw RegraNegocioException.conflito("CONTRATO_NAO_PENDENTE", "O contrato não está pendente.");
        }
    }

    //chamar com o atendimento já travado: a contagem só vale enquanto ninguém mais pode ativar um contrato nele
    private void exigirVaga(Atendimento atendimento) {
        int capacidade = veiculos.maiorCapacidade(atendimento.getMotorista().getId());
        long ativos = contratos.countByAtendimentoIdAndStatus(atendimento.getId(), StatusContrato.ATIVO);
        if (vagasRestantes(capacidade, ativos) <= 0) {
            throw RegraNegocioException.conflito("SEM_VAGAS", "Este atendimento não tem vagas disponíveis.");
        }
    }

    private void exigirAlunoSemContratoAtivo(Long alunoId) {
        if (contratos.existsByAlunoIdAndStatus(alunoId, StatusContrato.ATIVO)) {
            throw RegraNegocioException.conflito("ALUNO_JA_TEM_CONTRATO", "O aluno já possui um contrato ativo.");
        }
    }

    // ---------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> listar(Long usuarioId, StatusContrato status) {
        List<Contrato> lista = status == null
                ? contratos.doUsuario(usuarioId)
                : contratos.doUsuarioComStatus(usuarioId, status);
        return lista.stream().map(ContratoResponseDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public ContratoResponseDTO obter(UsuarioLogado eu, Long id) {
        Contrato c = contratos.carregar(id).orElseThrow(() -> RegraNegocioException.naoEncontrado("Contrato"));
        if (!c.getResponsavel().getId().equals(eu.id()) && !c.getMotorista().getId().equals(eu.id())) {
            throw RegraNegocioException.semPermissao();
        }
        return ContratoResponseDTO.de(c);
    }
}
