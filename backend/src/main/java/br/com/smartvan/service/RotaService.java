package br.com.smartvan.service;

import br.com.smartvan.dto.GerarRotaRequestDTO;
import br.com.smartvan.dto.RotaResponseDTO;
import br.com.smartvan.enums.StatusRota;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.enums.TipoParada;
import br.com.smartvan.enums.TipoRota;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.maps.Coordenada;
import br.com.smartvan.maps.MapsProvider;
import br.com.smartvan.maps.OrdenadorPorProximidade;
import br.com.smartvan.maps.RotaOtimizada;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.AlunoEndereco;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Endereco;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.ParadaRota;
import br.com.smartvan.model.ResponsavelAluno;
import br.com.smartvan.model.Rota;
import br.com.smartvan.model.Veiculo;
import br.com.smartvan.repository.AlunoEnderecoRepository;
import br.com.smartvan.repository.AtendimentoRepository;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.repository.FaltaRepository;
import br.com.smartvan.repository.MotoristaRepository;
import br.com.smartvan.repository.ResponsavelAlunoRepository;
import br.com.smartvan.repository.RotaRepository;
import br.com.smartvan.repository.VeiculoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

//UC15: gera a rota do dia (IDA ou VOLTA) com os alunos de um atendimento, menos as faltas
@Slf4j
@Service
public class RotaService {

    //o atendimento não tem horário de volta
    private static final LocalTime SAIDA_VOLTA = LocalTime.of(12, 30);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    /** @param nova true se foi criada agora (201), false se reaproveitada (200) */
    public record Resultado(RotaResponseDTO rota, boolean nova) {
    }

    private final AtendimentoRepository atendimentos;
    private final MotoristaRepository motoristas;
    private final VeiculoRepository veiculos;
    private final ContratoRepository contratos;
    private final FaltaRepository faltas;
    private final AlunoEnderecoRepository enderecosDosAlunos;
    private final ResponsavelAlunoRepository vinculos;
    private final RotaRepository rotas;
    private final EnderecoService enderecoService;
    private final MapsProvider maps;
    private final NotificacaoService notificacoes;
    private final Clock clock;
    private final int margemChegadaMinutos;

    public RotaService(AtendimentoRepository atendimentos, MotoristaRepository motoristas, VeiculoRepository veiculos,
                       ContratoRepository contratos, FaltaRepository faltas,
                       AlunoEnderecoRepository enderecosDosAlunos, ResponsavelAlunoRepository vinculos,
                       RotaRepository rotas, EnderecoService enderecoService, MapsProvider maps,
                       NotificacaoService notificacoes, Clock clock,
                       @Value("${smartvan.rota.margem-chegada-minutos:15}") int margemChegadaMinutos) {
        this.atendimentos = atendimentos;
        this.motoristas = motoristas;
        this.veiculos = veiculos;
        this.contratos = contratos;
        this.faltas = faltas;
        this.enderecosDosAlunos = enderecosDosAlunos;
        this.vinculos = vinculos;
        this.rotas = rotas;
        this.enderecoService = enderecoService;
        this.maps = maps;
        this.notificacoes = notificacoes;
        this.clock = clock;
        this.margemChegadaMinutos = margemChegadaMinutos;
    }

    @Transactional
    public Resultado gerar(Long motoristaId, GerarRotaRequestDTO req) {
        if (req.data().isBefore(LocalDate.now(clock))) {
            throw RegraNegocioException.regra("DATA_PASSADA", "A data da rota não pode ser passada.");
        }
        //trava o atendimento: gerações simultâneas esperam uma pela outra e não nascem duas rotas iguais
        Atendimento atendimento = atendimentos.travar(req.atendimentoId())
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Atendimento"));
        if (!atendimento.getMotorista().getId().equals(motoristaId)) {
            throw RegraNegocioException.semPermissao();
        }
        Motorista motorista = motoristas.findById(motoristaId).orElseThrow(RegraNegocioException::semPermissao);

        List<Aluno> alunos = alunosDaRota(atendimento.getId(), req.data());
        if (alunos.isEmpty()) {
            throw RegraNegocioException.regra("ROTA_SEM_ALUNOS",
                    "Nenhum aluno para esta rota (contratos ativos do atendimento, menos faltas).");
        }
        Endereco partida = motorista.getEnderecoPartida();
        if (partida == null) {
            throw RegraNegocioException.regra("ENDERECO_PARTIDA_OBRIGATORIO",
                    "Cadastre o endereço de partida antes de gerar a rota.");
        }
        Veiculo veiculo = escolherVeiculo(motoristaId, req.veiculoId());

        //reaproveita a rota planejada só com o mesmo conjunto de alunos e sem pedido de outro horário ou veículo
        Set<Long> idsAlunos = alunos.stream().map(Aluno::getId).collect(Collectors.toSet());
        List<Rota> existentes = rotas.findByMotoristaIdAndDataRotaAndTipoAndAtendimentoIdAndStatus(
                motoristaId, req.data(), req.tipo(), atendimento.getId(), StatusRota.PLANEJADA);
        for (Rota existente : existentes) {
            if (idsDosAlunos(existente).equals(idsAlunos) && atendeAoPedido(existente, req)) {
                return new Resultado(RotaResponseDTO.de(existente), false);
            }
        }
        rotas.deleteAll(existentes);
        rotas.flush();

        Map<Long, Endereco> casas = enderecosDosAlunos.principaisDosAlunos(idsAlunos).stream()
                .collect(Collectors.toMap(ae -> ae.getAluno().getId(), AlunoEndereco::getEndereco, (a, b) -> a));
        List<Coordenada> coordenadasDasCasas = new ArrayList<>();
        for (Aluno aluno : alunos) {
            Endereco casa = casas.get(aluno.getId());
            if (casa == null) {
                throw semCoordenadas();
            }
            coordenadasDasCasas.add(coordenada(casa));
        }
        Escola escola = atendimento.getEscola();
        Endereco enderecoEscola = escola.getEndereco();
        boolean ida = req.tipo() == TipoRota.IDA;
        Coordenada origem = coordenada(ida ? partida : enderecoEscola);
        Coordenada destino = coordenada(ida ? enderecoEscola : partida);

        LocalTime saida = horarioDeSaida(req, atendimento);
        Instant instanteSaida = LocalDateTime.of(req.data(), saida).atZone(clock.getZone()).toInstant();
        RotaOtimizada otimizacao = otimizar(origem, coordenadasDasCasas, destino, instanteSaida);
        boolean otimizada = otimizacao != null;
        RotaOtimizada percurso = otimizada ? otimizacao
                : OrdenadorPorProximidade.ordenar(origem, coordenadasDasCasas, destino);

        Rota rota = new Rota();
        rota.setMotorista(motorista);
        rota.setVeiculo(veiculo);
        rota.setEscola(escola);
        rota.setAtendimento(atendimento);
        rota.setDataRota(req.data());
        rota.setTipo(req.tipo());
        rota.setStatus(StatusRota.PLANEJADA);
        rota.setOtimizada(otimizada);
        rota.setPolyline(percurso.polyline());
        rota.setHorarioInicio(saida.truncatedTo(ChronoUnit.MINUTES));
        rota.setHorarioFim(saida.plusSeconds(percurso.segundosTotal()).truncatedTo(ChronoUnit.MINUTES));
        rota.setTempoEstimadoMinutos((int) Math.round(percurso.segundosTotal() / 60.0));
        rota.setDistanciaKm(BigDecimal.valueOf(percurso.metrosTotal())
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
        rota.setMinutosAcimaTeto(ida ? minutosAcimaDoTeto(atendimento, req.data(), saida, percurso.segundosTotal()) : null);

        int ordem = 1;
        if (!ida) {
            rota.adicionarParada(parada(ordem++, TipoParada.EMBARQUE, null, enderecoEscola, saida));
        }
        Map<Long, LocalTime> previstoPorAluno = new HashMap<>();
        TipoParada tipoDoAluno = ida ? TipoParada.EMBARQUE : TipoParada.DESEMBARQUE;
        for (int i = 0; i < percurso.ordemParadas().size(); i++) {
            Aluno aluno = alunos.get(percurso.ordemParadas().get(i));
            LocalTime previsto = saida.plusSeconds(percurso.segundosAteCadaParada().get(i));
            previstoPorAluno.put(aluno.getId(), previsto);
            rota.adicionarParada(parada(ordem++, tipoDoAluno, aluno, casas.get(aluno.getId()), previsto));
        }
        if (ida) {
            rota.adicionarParada(parada(ordem, TipoParada.DESEMBARQUE, null, enderecoEscola,
                    saida.plusSeconds(percurso.segundosTotal())));
        }
        rota = rotas.save(rota);

        notificarResponsaveis(rota, alunos, previstoPorAluno, escola, ida);
        if (rota.getMinutosAcimaTeto() != null) {
            notificarMotoristaForaDoTeto(rota, atendimento, escola);
        }
        return new Resultado(RotaResponseDTO.de(rota), true);
    }

    @Transactional(readOnly = true)
    public List<RotaResponseDTO> listar(Long motoristaId, LocalDate data) {
        LocalDate dia = data != null ? data : LocalDate.now(clock);
        return rotas.findByMotoristaIdAndDataRotaOrderByHorarioInicioAscIdAsc(motoristaId, dia).stream()
                .map(RotaResponseDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public RotaResponseDTO buscar(Long motoristaId, Long rotaId) {
        Rota rota = rotas.carregar(rotaId).orElseThrow(() -> RegraNegocioException.naoEncontrado("Rota"));
        if (!rota.getMotorista().getId().equals(motoristaId)) {
            throw RegraNegocioException.semPermissao();
        }
        return RotaResponseDTO.de(rota);
    }

    private List<Aluno> alunosDaRota(Long atendimentoId, LocalDate data) {
        List<Aluno> todos = contratos.alunosAtivosDoAtendimento(atendimentoId);
        if (todos.isEmpty()) {
            return todos;
        }
        Set<Long> comFalta = new HashSet<>(faltas.alunosComFalta(data, todos.stream().map(Aluno::getId).toList()));
        return todos.stream().filter(a -> !comFalta.contains(a.getId())).toList();
    }

    private static Set<Long> idsDosAlunos(Rota rota) {
        return rota.getParadas().stream().map(ParadaRota::getAluno).filter(Objects::nonNull)
                .map(Aluno::getId).collect(Collectors.toSet());
    }

    //o que o pedido não informa não impede o reaproveitamento; o motorista adianta a saída mandando o novo horário
    private static boolean atendeAoPedido(Rota rota, GerarRotaRequestDTO req) {
        boolean mesmoHorario = req.horarioSaida() == null
                || req.horarioSaida().truncatedTo(ChronoUnit.MINUTES).equals(rota.getHorarioInicio());
        boolean mesmoVeiculo = req.veiculoId() == null || req.veiculoId().equals(rota.getVeiculo().getId());
        return mesmoHorario && mesmoVeiculo;
    }

    private Veiculo escolherVeiculo(Long motoristaId, Long veiculoId) {
        if (veiculoId != null) {
            Veiculo veiculo = veiculos.findById(veiculoId)
                    .orElseThrow(() -> RegraNegocioException.naoEncontrado("Veículo"));
            if (!veiculo.getMotorista().getId().equals(motoristaId)) {
                throw RegraNegocioException.semPermissao();
            }
            return veiculo;
        }
        List<Veiculo> doMotorista = veiculos.findByMotoristaIdOrderByIdAsc(motoristaId);
        if (doMotorista.isEmpty()) {
            throw RegraNegocioException.regra("VEICULO_OBRIGATORIO", "Cadastre um veículo para gerar a rota.");
        }
        if (doMotorista.size() > 1) {
            throw RegraNegocioException.regra("VEICULO_AMBIGUO", "Você tem mais de um veículo: informe o veiculoId.");
        }
        return doMotorista.getFirst();
    }

    private Coordenada coordenada(Endereco endereco) {
        return enderecoService.coordenadaDe(endereco).orElseThrow(RotaService::semCoordenadas);
    }

    private static RegraNegocioException semCoordenadas() {
        return RegraNegocioException.regra("ENDERECO_SEM_COORDENADAS",
                "Um endereço da rota está sem coordenadas e não foi possível obtê-las.");
    }

    private static LocalTime horarioDeSaida(GerarRotaRequestDTO req, Atendimento atendimento) {
        if (req.horarioSaida() != null) {
            return req.horarioSaida();
        }
        return req.tipo() == TipoRota.IDA ? atendimento.getHorarioSaidaMotorista() : SAIDA_VOLTA;
    }

    //nulo quando o serviço de mapas falhou ou devolveu algo sem as paradas pedidas: quem chama usa o plano B
    private RotaOtimizada otimizar(Coordenada origem, List<Coordenada> paradas, Coordenada destino, Instant saida) {
        try {
            RotaOtimizada resultado = maps.otimizarRota(origem, paradas, destino, saida);
            return valido(resultado, paradas.size()) ? resultado : null;
        } catch (RuntimeException e) {
            //só o tipo da exceção: a mensagem não vai para o log
            log.warn("Otimização de rota indisponível, usando proximidade: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private static boolean valido(RotaOtimizada r, int totalParadas) {
        if (r == null || r.ordemParadas() == null || r.segundosAteCadaParada() == null) {
            return false;
        }
        if (r.ordemParadas().size() != totalParadas || r.segundosAteCadaParada().size() != totalParadas) {
            return false;
        }
        Set<Integer> vistos = new HashSet<>(r.ordemParadas());
        return vistos.size() == totalParadas && vistos.stream().allMatch(i -> i != null && i >= 0 && i < totalParadas);
    }

    //arredonda para cima: adiantar esse número de minutos faz a chegada caber no teto
    private Integer minutosAcimaDoTeto(Atendimento atendimento, LocalDate data, LocalTime saida, int segundosTotal) {
        LocalDateTime chegada = data.atTime(saida).plusSeconds(segundosTotal);
        LocalDateTime teto = data.atTime(atendimento.tetoChegada(margemChegadaMinutos));
        long segundosAcima = Duration.between(teto, chegada).getSeconds();
        return segundosAcima > 0 ? (int) Math.ceilDiv(segundosAcima, 60L) : null;
    }

    private static ParadaRota parada(int ordem, TipoParada tipo, Aluno aluno, Endereco endereco, LocalTime previsto) {
        ParadaRota parada = new ParadaRota();
        parada.setOrdem(ordem);
        parada.setTipoParada(tipo);
        parada.setAluno(aluno);
        parada.setEndereco(endereco);
        parada.setHorarioPrevisto(previsto.truncatedTo(ChronoUnit.MINUTES));
        return parada;
    }

    private void notificarResponsaveis(Rota rota, List<Aluno> alunos, Map<Long, LocalTime> previstos, Escola escola,
                                       boolean ida) {
        Map<Long, Aluno> porId = alunos.stream().collect(Collectors.toMap(Aluno::getId, Function.identity()));
        String data = rota.getDataRota().format(DIA_MES);
        for (ResponsavelAluno vinculo : vinculos.ativosDosAlunos(porId.keySet())) {
            Aluno aluno = porId.get(vinculo.getId().getAlunoId());
            String hora = previstos.get(aluno.getId()).format(HORA);
            String mensagem = ida
                    ? "Rota de ida de %s para %s: %s será buscado(a) por volta das %s."
                            .formatted(data, escola.getNome(), aluno.getNome(), hora)
                    : "Rota de volta de %s de %s: %s chega em casa por volta das %s."
                            .formatted(data, escola.getNome(), aluno.getNome(), hora);
            notificacoes.notificar(vinculo.getId().getResponsavelId(), TipoNotificacao.ROTA_GERADA,
                    "Rota do dia gerada", mensagem, rota.getId());
        }
    }

    private void notificarMotoristaForaDoTeto(Rota rota, Atendimento atendimento, Escola escola) {
        String mensagem = "A rota de ida de %s para %s chega às %s, depois do teto de %s. Adiante a saída em %d minuto(s)."
                .formatted(rota.getDataRota().format(DIA_MES), escola.getNome(), rota.getHorarioFim().format(HORA),
                        atendimento.tetoChegada(margemChegadaMinutos).format(HORA), rota.getMinutosAcimaTeto());
        notificacoes.notificar(rota.getMotorista().getId(), TipoNotificacao.ROTA_GERADA,
                "Rota fora do teto de chegada", mensagem, rota.getId());
    }
}
