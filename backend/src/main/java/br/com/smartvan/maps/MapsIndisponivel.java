package br.com.smartvan.maps;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Implementação usada quando não há chave do Google. Registrada em br.com.smartvan.config.MapsConfig. */
public class MapsIndisponivel implements MapsProvider {

    @Override
    public Optional<Coordenada> geocodificar(String enderecoCompleto) {
        return Optional.empty();
    }

    @Override
    public RotaOtimizada otimizarRota(Coordenada partida, List<Coordenada> paradas, Coordenada destino, Instant saida) {
        throw new MapsIndisponivelException("Serviço de mapas não configurado (GOOGLE_MAPS_API_KEY ausente).");
    }
}
