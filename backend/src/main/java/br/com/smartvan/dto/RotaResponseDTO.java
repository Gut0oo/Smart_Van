package br.com.smartvan.dto;

import br.com.smartvan.enums.StatusRota;
import br.com.smartvan.enums.TipoParada;
import br.com.smartvan.enums.TipoRota;
import br.com.smartvan.model.ParadaRota;
import br.com.smartvan.model.Rota;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Objeto {@code Rota} do contrato. As associações são lazy: chame {@link #de} dentro de uma transação.
 * {@code atendimentoId} sai nulo se o motorista removeu o turno e {@code minutosAdiantar}, dentro do teto.
 */
public record RotaResponseDTO(Long id, LocalDate data, TipoRota tipo, StatusRota status, Long atendimentoId,
                              EscolaResumoResponseDTO escola, Veiculo veiculo, boolean otimizada,
                              Integer tempoEstimadoMinutos, BigDecimal distanciaKm,
                              @JsonFormat(pattern = "HH:mm") LocalTime horarioInicio,
                              @JsonFormat(pattern = "HH:mm") LocalTime horarioFim,
                              String polyline, boolean foraDoTeto, Integer minutosAdiantar, List<Parada> paradas) {

    public record Veiculo(Long id, String placa, String modelo) {
    }

    public record Parada(int ordem, TipoParada tipoParada, AlunoResumoResponseDTO aluno, EnderecoResponseDTO endereco,
                         @JsonFormat(pattern = "HH:mm") LocalTime horarioPrevisto) {

        static Parada de(ParadaRota p) {
            return new Parada(p.getOrdem(), p.getTipoParada(),
                    p.getAluno() == null ? null : AlunoResumoResponseDTO.de(p.getAluno()),
                    EnderecoResponseDTO.de(p.getEndereco()), p.getHorarioPrevisto());
        }
    }

    public static RotaResponseDTO de(Rota r) {
        return new RotaResponseDTO(r.getId(), r.getDataRota(), r.getTipo(), r.getStatus(),
                r.getAtendimento() == null ? null : r.getAtendimento().getId(),
                EscolaResumoResponseDTO.de(r.getEscola()),
                new Veiculo(r.getVeiculo().getId(), r.getVeiculo().getPlaca(), r.getVeiculo().getModelo()),
                r.isOtimizada(), r.getTempoEstimadoMinutos(), r.getDistanciaKm(), r.getHorarioInicio(),
                r.getHorarioFim(), r.getPolyline(), r.getMinutosAcimaTeto() != null, r.getMinutosAcimaTeto(),
                r.getParadas().stream().map(Parada::de).toList());
    }
}
