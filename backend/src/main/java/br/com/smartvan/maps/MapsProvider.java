package br.com.smartvan.maps;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Porta para o serviço de mapas (Google Maps Platform).
 * Sem GOOGLE_MAPS_API_KEY, a implementação ativa é {@link MapsIndisponivel}: geocodificação vazia e
 * otimização lançando {@link MapsIndisponivelException}, o que aciona o plano B (proximidade, otimizada=false).
 * Testes nunca chamam o Google.
 */
public interface MapsProvider {

    /** Endereço em texto ("Rua X, 123, Bairro, Cidade - UF, CEP") vira coordenada, se encontrada. */
    Optional<Coordenada> geocodificar(String enderecoCompleto);

    /**
     * Ordena as paradas intermediárias entre partida e destino.
     * @throws MapsIndisponivelException se o serviço falhar ou não estiver configurado
     */
    RotaOtimizada otimizarRota(Coordenada partida, List<Coordenada> paradas, Coordenada destino, Instant saida);
}
