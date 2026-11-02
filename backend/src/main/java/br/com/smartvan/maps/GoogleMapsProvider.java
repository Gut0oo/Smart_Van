package br.com.smartvan.maps;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Google Routes API (otimização de paradas) e Geocoding API. A chave vem só da configuração.
 * Toda falha vira {@link MapsIndisponivelException} sem a causa e sem a mensagem original, porque elas podem trazer
 * a URL com a chave: a chave nunca vai para log nem para mensagem de exceção.
 */
public class GoogleMapsProvider implements MapsProvider {

    public static final String URL_ROTAS = "https://routes.googleapis.com/directions/v2:computeRoutes";
    public static final String URL_GEOCODING = "https://maps.googleapis.com/maps/api/geocode/json";
    static final String FIELD_MASK = "routes.optimizedIntermediateWaypointIndex,routes.legs.duration,"
            + "routes.legs.distanceMeters,routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline";
    static final int MAX_PARADAS = 25;
    //timeouts curtos: o Google não pode travar a geração da rota, que espera com o atendimento travado
    static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient http;
    private final JsonMapper json = JsonMapper.builder().build();
    private final String chave;
    private final String urlRotas;
    private final String urlGeocoding;
    private final Clock clock;

    public GoogleMapsProvider(String chave, Clock clock) {
        this(clienteComTimeout(TIMEOUT), chave, URL_ROTAS, URL_GEOCODING, clock);
    }

    GoogleMapsProvider(RestClient http, String chave, String urlRotas, String urlGeocoding, Clock clock) {
        this.http = http;
        this.chave = chave;
        this.urlRotas = urlRotas;
        this.urlGeocoding = urlGeocoding;
        this.clock = clock;
    }

    static RestClient clienteComTimeout(Duration timeout) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeout);
        fabrica.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public Optional<Coordenada> geocodificar(String enderecoCompleto) {
        try {
            String corpo = http.get()
                    .uri(urlGeocoding + "?address={a}&key={k}&region=br", enderecoCompleto, chave)
                    .retrieve().body(String.class);
            JsonNode raiz = json.readTree(corpo);
            if (!"OK".equals(raiz.path("status").asString(""))) {
                return Optional.empty();
            }
            JsonNode local = raiz.path("results").path(0).path("geometry").path("location");
            if (!local.has("lat") || !local.has("lng")) {
                return Optional.empty();
            }
            return Optional.of(new Coordenada(new BigDecimal(local.get("lat").asString()),
                    new BigDecimal(local.get("lng").asString())));
        } catch (Exception e) {
            throw new MapsIndisponivelException("Falha ao geocodificar no Google (" + e.getClass().getSimpleName() + ").");
        }
    }

    @Override
    public RotaOtimizada otimizarRota(Coordenada partida, List<Coordenada> paradas, Coordenada destino, Instant saida) {
        if (paradas.size() > MAX_PARADAS) {
            throw new MapsIndisponivelException("Mais de " + MAX_PARADAS + " paradas: otimização do Google não usada.");
        }
        //o Google recusa horário de saída no passado quando considera o trânsito
        Instant agora = clock.instant();
        Instant partidaEm = saida.isAfter(agora) ? saida : agora.plusSeconds(60);
        try {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("origin", ponto(partida));
            corpo.put("destination", ponto(destino));
            if (!paradas.isEmpty()) {
                corpo.put("intermediates", paradas.stream().map(GoogleMapsProvider::ponto).toList());
                corpo.put("optimizeWaypointOrder", true);
            }
            corpo.put("travelMode", "DRIVE");
            corpo.put("routingPreference", "TRAFFIC_AWARE");
            corpo.put("departureTime", partidaEm.toString());

            String resposta = http.post().uri(urlRotas)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", chave)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(json.writeValueAsString(corpo))
                    .retrieve().body(String.class);
            return interpretar(json.readTree(resposta), paradas.size());
        } catch (MapsIndisponivelException e) {
            throw e;
        } catch (Exception e) {
            throw new MapsIndisponivelException("Falha ao otimizar rota no Google (" + e.getClass().getSimpleName() + ").");
        }
    }

    private RotaOtimizada interpretar(JsonNode raiz, int totalParadas) {
        JsonNode rota = raiz.path("routes").path(0);
        if (rota.isMissingNode()) {
            throw new MapsIndisponivelException("Google não devolveu rota.");
        }
        List<Integer> ordem = new ArrayList<>();
        JsonNode indices = rota.path("optimizedIntermediateWaypointIndex");
        if (indices.isArray()) {
            indices.forEach(n -> ordem.add(n.asInt()));
        } else {
            for (int i = 0; i < totalParadas; i++) {
                ordem.add(i);
            }
        }
        // n paradas intermediárias => n+1 pernas; a perna i chega na i-ésima parada (já na ordem ótima)
        JsonNode pernas = rota.path("legs");
        if (ordem.size() != totalParadas || pernas.size() != totalParadas + 1) {
            throw new MapsIndisponivelException("Resposta do Google com paradas inesperadas.");
        }
        List<Integer> acumulado = new ArrayList<>();
        int soma = 0;
        for (int i = 0; i < totalParadas; i++) {
            soma += segundos(pernas.path(i).path("duration"));
            acumulado.add(soma);
        }
        JsonNode polyline = rota.path("polyline").path("encodedPolyline");
        return new RotaOtimizada(ordem, acumulado, segundos(rota.path("duration")),
                rota.path("distanceMeters").asInt(0), polyline.isMissingNode() ? null : polyline.asString());
    }

    //a duração vem como texto "123s"; sem ela os horários previstos e o teto ficariam errados, então vai para o plano B
    private static int segundos(JsonNode duracao) {
        String texto = duracao.asString("");
        if (!texto.endsWith("s")) {
            throw new MapsIndisponivelException("Resposta do Google sem duração.");
        }
        try {
            return (int) Math.round(Double.parseDouble(texto.substring(0, texto.length() - 1)));
        } catch (NumberFormatException e) {
            throw new MapsIndisponivelException("Resposta do Google com duração inválida.");
        }
    }

    private static Map<String, Object> ponto(Coordenada c) {
        return Map.of("location", Map.of("latLng", Map.of(
                "latitude", c.latitude().doubleValue(), "longitude", c.longitude().doubleValue())));
    }
}
