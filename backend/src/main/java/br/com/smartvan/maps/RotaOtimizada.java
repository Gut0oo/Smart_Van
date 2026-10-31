package br.com.smartvan.maps;

import java.util.List;

/**
 * Resultado da otimização.
 * @param ordemParadas índices de {@code paradas} (entrada de otimizarRota) na ordem ótima
 * @param segundosAteCadaParada tempo acumulado desde a partida até cada parada, na ordem ótima (mesmo tamanho)
 * @param segundosTotal tempo até o destino
 * @param metrosTotal distância até o destino
 * @param polyline traçado codificado (pode ser null)
 */
public record RotaOtimizada(List<Integer> ordemParadas, List<Integer> segundosAteCadaParada,
                            int segundosTotal, int metrosTotal, String polyline) {
}
