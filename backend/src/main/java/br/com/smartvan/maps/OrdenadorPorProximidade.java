package br.com.smartvan.maps;

import java.util.ArrayList;
import java.util.List;

/**
 * Plano B quando o serviço de mapas não responde: vizinho mais próximo (haversine) a partir da partida.
 * Distância estimada = haversine x 1,3; tempo a 25 km/h; sem polyline. Classe pura (sem Spring).
 */
public final class OrdenadorPorProximidade {

    static final double RAIO_TERRA_KM = 6371.0088;
    static final double FATOR_RUA = 1.3;
    static final double VELOCIDADE_KMH = 25.0;

    private OrdenadorPorProximidade() {
    }

    public static RotaOtimizada ordenar(Coordenada partida, List<Coordenada> paradas, Coordenada destino) {
        List<Integer> restantes = new ArrayList<>();
        for (int i = 0; i < paradas.size(); i++) {
            restantes.add(i);
        }
        List<Integer> ordem = new ArrayList<>();
        List<Integer> segundosAte = new ArrayList<>();
        Coordenada atual = partida;
        double kmAcumulado = 0;
        while (!restantes.isEmpty()) {
            int melhor = restantes.getFirst();
            double menor = Double.MAX_VALUE;
            for (int indice : restantes) {
                double distancia = haversineKm(atual, paradas.get(indice));
                if (distancia < menor) {
                    menor = distancia;
                    melhor = indice;
                }
            }
            restantes.remove(Integer.valueOf(melhor));
            ordem.add(melhor);
            kmAcumulado += menor * FATOR_RUA;
            segundosAte.add(segundos(kmAcumulado));
            atual = paradas.get(melhor);
        }
        kmAcumulado += haversineKm(atual, destino) * FATOR_RUA;
        return new RotaOtimizada(ordem, segundosAte, segundos(kmAcumulado), (int) Math.round(kmAcumulado * 1000), null);
    }

    private static int segundos(double km) {
        return (int) Math.round(km / VELOCIDADE_KMH * 3600);
    }

    /** Distância em linha reta (km) entre duas coordenadas. */
    public static double haversineKm(Coordenada a, Coordenada b) {
        double lat1 = Math.toRadians(a.latitude().doubleValue());
        double lat2 = Math.toRadians(b.latitude().doubleValue());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude().doubleValue() - a.longitude().doubleValue());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAIO_TERRA_KM * Math.asin(Math.min(1, Math.sqrt(h)));
    }
}
