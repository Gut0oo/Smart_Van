package br.com.smartvan.maps;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
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
 * a URL com a chave: a chave nunca vai para log nem para mensagem de exceção. O log registra o status e a mensagem
 * do Google (com a chave trocada por ***, se aparecer), para a recusa não passar em silêncio.
 */
@Slf4j
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
            String status = raiz.path("status").asString("");
            if (!"OK".equals(status)) {
                //endereço não encontrado é resultado normal; o resto é recusa ou problema de configuração
                if ("ZERO_RESULTS".equals(status)) {
                    log.info("Google não encontrou o endereço (ZERO_RESULTS).");
                } else {
                    log.warn("Google recusou a geocodificação: status {}, mensagem: {}", status,
                            semChave(raiz.path("error_message").asString("")));
                }
                return Optional.empty();
            }
            JsonNode local = raiz.path("results").path(0).path("geometry").path("location");
            if (!local.has("lat") || !local.has("lng")) {
                return Optional.empty();
            }
            return Optional.of(new Coordenada(new BigDecimal(local.get("lat").asString()),
                    new BigDecimal(local.get("lng").asString())));
        } catch (Exception e) {
            log.warn("Falha ao geocodificar no Google: {}", descrever(e));
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
            //resposta do Google que não dá para usar; a mensagem é nossa e não tem a chave
            log.warn("Resposta do Google para a rota não usada: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Falha ao otimizar rota no Google: {}", descrever(e));
            throw new MapsIndisponivelException("Falha ao otimizar rota no Google (" + e.getClass().getSimpleName() + ").");
        }
    }

    //erro HTTP: status e mensagem do Google; outra falha: só o tipo, porque a mensagem pode trazer a URL com a chave
    private String descrever(Exception e) {
        if (e instanceof RestClientResponseException resposta) {
            return "HTTP " + resposta.getStatusCode().value() + ", mensagem: "
                    + semChave(mensagemDoGoogle(resposta.getResponseBodyAsString()));
        }
        return e.getClass().getSimpleName();
    }

    //a Routes API devolve { error: { message } } e a Geocoding API, { error_message }
    private String mensagemDoGoogle(String corpo) {
        try {
            JsonNode raiz = json.readTree(corpo);
            JsonNode mensagem = raiz.path("error").path("message");
            return mensagem.isMissingNode() ? raiz.path("error_message").asString("") : mensagem.asString("");
        } catch (Exception e) {
            return "";
        }
    }

    private String semChave(String texto) {
        return texto == null || chave == null || chave.isEmpty() ? texto : texto.replace(chave, "***");
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
