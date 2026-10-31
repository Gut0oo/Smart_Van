package br.com.smartvan.service;

import br.com.smartvan.dto.AtendimentoRequestDTO;
import br.com.smartvan.dto.AtendimentoResponseDTO;
import br.com.smartvan.dto.EnderecoRequestDTO;
import br.com.smartvan.dto.EnderecoResponseDTO;
import br.com.smartvan.dto.EscolaResumoResponseDTO;
import br.com.smartvan.dto.PerfilMotoristaResponseDTO;
import br.com.smartvan.dto.UsuarioDetalheResponseDTO;
import br.com.smartvan.dto.VeiculoRequestDTO;
import br.com.smartvan.dto.VeiculoResponseDTO;
import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Endereco;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Veiculo;
import br.com.smartvan.repository.AtendimentoRepository;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.repository.EscolaRepository;
import br.com.smartvan.repository.MotoristaRepository;
import br.com.smartvan.repository.VeiculoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

//veículos, atendimentos (escolas e horários), endereço de partida e perfil do motorista
@Service
public class MotoristaCadastroService {

    private final MotoristaRepository motoristas;
    private final VeiculoRepository veiculos;
    private final EscolaRepository escolas;
    private final AtendimentoRepository atendimentos;
    private final ContratoRepository contratos;
    private final EnderecoService enderecoService;
    private final int margemChegadaMinutos;

    public MotoristaCadastroService(MotoristaRepository motoristas, VeiculoRepository veiculos,
                                    EscolaRepository escolas, AtendimentoRepository atendimentos,
                                    ContratoRepository contratos, EnderecoService enderecoService,
                                    @Value("${smartvan.rota.margem-chegada-minutos:15}") int margemChegadaMinutos) {
        this.motoristas = motoristas;
        this.veiculos = veiculos;
        this.escolas = escolas;
        this.atendimentos = atendimentos;
        this.contratos = contratos;
        this.enderecoService = enderecoService;
        this.margemChegadaMinutos = margemChegadaMinutos;
    }

    private Motorista motorista(Long id) {
        return motoristas.findById(id).orElseThrow(RegraNegocioException::semPermissao);
    }

    @Transactional
    public VeiculoResponseDTO cadastrarVeiculo(Long motoristaId, VeiculoRequestDTO req) {
        Motorista m = motorista(motoristaId);
        String placa = req.placa().toUpperCase(Locale.ROOT);
        if (veiculos.existsByPlaca(placa)) {
            throw RegraNegocioException.conflito("PLACA_JA_CADASTRADA", "Placa já cadastrada.");
        }
        Veiculo v = new Veiculo(m, req.tipo(), placa, req.modelo().trim(), req.capacidade(), req.ano());
        return VeiculoResponseDTO.de(veiculos.save(v));
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponseDTO> listarVeiculos(Long motoristaId) {
        return veiculos.findByMotoristaIdOrderByIdAsc(motoristaId).stream().map(VeiculoResponseDTO::de).toList();
    }

    //a lista recebida substitui todos os atendimentos do motorista; itens idênticos contam uma vez.
    //o que já existe e continua na lista fica com o mesmo id, porque os contratos apontam para ele
    @Transactional
    public List<AtendimentoResponseDTO> definirAtendimentos(Long motoristaId, List<AtendimentoRequestDTO> novos) {
        Motorista m = motorista(motoristaId);
        Set<Long> ids = novos.stream().map(AtendimentoRequestDTO::escolaId).collect(Collectors.toSet());
        Map<Long, Escola> encontradas = escolas.findAllById(ids).stream()
                .collect(Collectors.toMap(Escola::getId, Function.identity()));
        if (encontradas.size() != ids.size()) {
            throw RegraNegocioException.naoEncontrado("Escola");
        }

        Set<AtendimentoRequestDTO> aCriar = new LinkedHashSet<>(novos);
        List<Atendimento> aRemover = atendimentos.travarDoMotorista(motoristaId).stream()
                .filter(a -> !aCriar.remove(new AtendimentoRequestDTO(a.getEscola().getId(),
                        a.getHorarioInicioAula(), a.getHorarioSaidaMotorista())))
                .toList();
        remover(aRemover);

        atendimentos.saveAll(aCriar.stream().map(req -> {
            Atendimento a = new Atendimento();
            a.setMotorista(m);
            a.setEscola(encontradas.get(req.escolaId()));
            a.setHorarioInicioAula(req.horarioInicioAula());
            a.setHorarioSaidaMotorista(req.horarioSaidaMotorista());
            return a;
        }).toList());
        return listarAtendimentos(motoristaId);
    }

    //turno com proposta ou contrato em andamento não sai; contrato já terminado continua, sem o turno
    private void remover(List<Atendimento> aRemover) {
        if (aRemover.isEmpty()) {
            return;
        }
        List<Long> ids = aRemover.stream().map(Atendimento::getId).toList();
        if (contratos.existsByAtendimentoIdInAndStatusIn(ids, List.of(StatusContrato.PENDENTE, StatusContrato.ATIVO))) {
            throw RegraNegocioException.conflito("ATENDIMENTO_COM_CONTRATO",
                    "Não é possível remover um atendimento com proposta pendente ou contrato ativo.");
        }
        contratos.desvincularAtendimentos(ids);
        atendimentos.deleteAll(aRemover);
    }

    @Transactional(readOnly = true)
    public List<AtendimentoResponseDTO> listarAtendimentos(Long motoristaId) {
        return atendimentos.doMotorista(motoristaId).stream()
                .map(a -> AtendimentoResponseDTO.de(a, margemChegadaMinutos))
                .toList();
    }

    @Transactional
    public EnderecoResponseDTO definirEnderecoPartida(Long motoristaId, EnderecoRequestDTO req) {
        Motorista m = motorista(motoristaId);
        Endereco atual = m.getEnderecoPartida() != null ? m.getEnderecoPartida() : new Endereco();
        Endereco salvo = enderecoService.atualizar(atual, req);
        m.setEnderecoPartida(salvo);
        return EnderecoResponseDTO.de(salvo);
    }

    @Transactional(readOnly = true)
    public PerfilMotoristaResponseDTO perfil(Long motoristaId) {
        Motorista m = motorista(motoristaId);
        //o perfil lista cada escola uma vez, mesmo com vários atendimentos nela
        List<EscolaResumoResponseDTO> escolasAtendidas = atendimentos.doMotorista(motoristaId).stream()
                .map(a -> EscolaResumoResponseDTO.de(a.getEscola()))
                .distinct()
                .toList();
        return new PerfilMotoristaResponseDTO(
                UsuarioDetalheResponseDTO.de(m),
                m.getEnderecoPartida() == null ? null : EnderecoResponseDTO.de(m.getEnderecoPartida()),
                listarVeiculos(motoristaId),
                escolasAtendidas);
    }
}
