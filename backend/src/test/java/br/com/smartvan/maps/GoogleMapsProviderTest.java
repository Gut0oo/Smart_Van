package br.com.smartvan.maps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

//nunca chama o Google: o RestClient aponta para um MockRestServiceServer ou para um socket local
class GoogleMapsProviderTest {

    private static final String CHAVE = "chave-de-teste-123";
    private static final Instant AGORA = Instant.parse("2026-11-03T12:00:00Z");
    private static final Instant DEPOIS = AGORA.plusSeconds(3600);

    private MockRestServiceServer servidor;
    private GoogleMapsProvider provider;

    private static Coordenada c(String lat, String lng) {
        return new Coordenada(new BigDecimal(lat), new BigDecimal(lng));
    }

    private static final Coordenada PARTIDA = c("-23.5", "-46.6");
    private static final Coordenada DESTINO = c("-23.53", "-46.63");
    private static final List<Coordenada> UMA_PARADA = List.of(c("-23.51", "-46.61"));

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        provider = new GoogleMapsProvider(builder.build(), CHAVE, GoogleMapsProvider.URL_ROTAS,
                GoogleMapsProvider.URL_GEOCODING, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private void assertSemChave(Throwable e) {
        assertThat(e).isInstanceOf(MapsIndisponivelException.class).hasNoCause();
        assertThat(e.getMessage()).doesNotContain(CHAVE);
        assertThat(e.toString()).doesNotContain(CHAVE);
    }

    @Test
    void otimizaRotaEInterpretaResposta() {
        servidor.expect(requestTo(GoogleMapsProvider.URL_ROTAS))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-FieldMask", GoogleMapsProvider.FIELD_MASK))
                .andExpect(header("X-Goog-Api-Key", CHAVE))
                .andExpect(jsonPath("$.optimizeWaypointOrder").value(true))
                .andExpect(jsonPath("$.travelMode").value("DRIVE"))
                .andExpect(jsonPath("$.routingPreference").value("TRAFFIC_AWARE"))
                .andExpect(jsonPath("$.intermediates.length()").value(2))
                .andExpect(jsonPath("$.origin.location.latLng.latitude").value(-23.5))
                .andExpect(jsonPath("$.destination.location.latLng.latitude").value(-23.53))
                .andExpect(jsonPath("$.departureTime").value(DEPOIS.toString()))
                .andRespond(withSuccess("""
                        {"routes":[{
                          "optimizedIntermediateWaypointIndex":[1,0],
                          "legs":[{"duration":"300s","distanceMeters":1000},
                                  {"duration":"420s","distanceMeters":1500},
                                  {"duration":"180s","distanceMeters":700}],
                          "duration":"900s","distanceMeters":3200,
                          "polyline":{"encodedPolyline":"xyz_abc"}}]}
                        """, MediaType.APPLICATION_JSON));

        RotaOtimizada r = provider.otimizarRota(PARTIDA, List.of(c("-23.51", "-46.61"), c("-23.52", "-46.62")),
                DESTINO, DEPOIS);

        assertThat(r.ordemParadas()).containsExactly(1, 0);
        assertThat(r.segundosAteCadaParada()).containsExactly(300, 720);
        assertThat(r.segundosTotal()).isEqualTo(900);
        assertThat(r.metrosTotal()).isEqualTo(3200);
        assertThat(r.polyline()).isEqualTo("xyz_abc");
        servidor.verify();
    }

    @Test
    void saidaNoPassadoUsaOAgoraDoClockMaisUmMinuto() {
        servidor.expect(requestTo(GoogleMapsProvider.URL_ROTAS))
                .andExpect(jsonPath("$.departureTime").value(AGORA.plusSeconds(60).toString()))
                .andRespond(withSuccess("""
                        {"routes":[{"optimizedIntermediateWaypointIndex":[0],
                          "legs":[{"duration":"60s"},{"duration":"60s"}],"duration":"120s","distanceMeters":10}]}
                        """, MediaType.APPLICATION_JSON));

        RotaOtimizada r = provider.otimizarRota(PARTIDA, UMA_PARADA, DESTINO, AGORA.minusSeconds(3600));

        assertThat(r.polyline()).isNull();
        servidor.verify();
    }

    @Test
    void maisDe25ParadasLancaIndisponivelSemChamarOGoogle() {
        List<Coordenada> muitas = new ArrayList<>(Collections.nCopies(26, PARTIDA));

        assertThatThrownBy(() -> provider.otimizarRota(PARTIDA, muitas, DESTINO, DEPOIS))
                .isInstanceOf(MapsIndisponivelException.class);
        servidor.verify();
    }

    @Test
    void erroHttpNaOtimizacaoViraIndisponivelSemVazarAChave() {
        servidor.expect(requestTo(GoogleMapsProvider.URL_ROTAS)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> provider.otimizarRota(PARTIDA, UMA_PARADA, DESTINO, DEPOIS))
                .satisfies(this::assertSemChave);
    }

    @Test
    void falhaDeRedeNaOtimizacaoViraIndisponivelSemVazarAChave() {
        servidor.expect(requestTo(GoogleMapsProvider.URL_ROTAS))
                .andRespond(withException(new IOException("falha em ?key=" + CHAVE)));

        assertThatThrownBy(() -> provider.otimizarRota(PARTIDA, UMA_PARADA, DESTINO, DEPOIS))
                .satisfies(this::assertSemChave);
    }

    @Test
    void respostaSemRotaViraIndisponivel() {
        servidor.expect(requestTo(GoogleMapsProvider.URL_ROTAS))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.otimizarRota(PARTIDA, UMA_PARADA, DESTINO, DEPOIS))
                .isInstanceOf(MapsIndisponivelException.class);
    }

    @Test
    void respostaSemDuracaoViraIndisponivel() {
        servidor.expect(requestTo(GoogleMapsProvider.URL_ROTAS))
                .andRespond(withSuccess("""
                        {"routes":[{"optimizedIntermediateWaypointIndex":[0],"legs":[{},{}],"distanceMeters":10}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.otimizarRota(PARTIDA, UMA_PARADA, DESTINO, DEPOIS))
                .isInstanceOf(MapsIndisponivelException.class);
    }

    @Test
    void respostaComPernasFaltandoViraIndisponivel() {
        servidor.expect(requestTo(GoogleMapsProvider.URL_ROTAS))
                .andRespond(withSuccess("""
                        {"routes":[{"optimizedIntermediateWaypointIndex":[0],
                          "legs":[{"duration":"60s"}],"duration":"60s"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.otimizarRota(PARTIDA, UMA_PARADA, DESTINO, DEPOIS))
                .isInstanceOf(MapsIndisponivelException.class);
    }

    @Test
    void geocodificaEndereco() {
        servidor.expect(requestTo(containsString("/geocode/json")))
                .andExpect(queryParam("key", CHAVE))
                .andExpect(queryParam("region", "br"))
                .andRespond(withSuccess("""
                        {"status":"OK","results":[{"geometry":{"location":{"lat":-23.5614,"lng":-46.6921}}}]}
                        """, MediaType.APPLICATION_JSON));

        var r = provider.geocodificar("Rua A, 10, Pinheiros, São Paulo - SP, 05422001");

        assertThat(r).isPresent();
        assertThat(r.get().latitude()).isEqualByComparingTo("-23.5614");
        assertThat(r.get().longitude()).isEqualByComparingTo("-46.6921");
        servidor.verify();
    }

    @Test
    void geocodificacaoSemResultadoDevolveVazio() {
        servidor.expect(requestTo(containsString("/geocode/json")))
                .andRespond(withSuccess("{\"status\":\"ZERO_RESULTS\",\"results\":[]}", MediaType.APPLICATION_JSON));

        assertThat(provider.geocodificar("endereco que nao existe")).isEmpty();
    }

    @Test
    void erroHttpNaGeocodificacaoViraIndisponivelSemVazarAChave() {
        servidor.expect(requestTo(containsString("/geocode/json")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> provider.geocodificar("qualquer")).satisfies(this::assertSemChave);
    }

    @Test
    void falhaDeRedeNaGeocodificacaoViraIndisponivelSemVazarAChave() {
        //a chave vai na URL da geocodificação, e o RestClient costuma pôr a URL na mensagem de erro
        servidor.expect(requestTo(containsString("/geocode/json")))
                .andRespond(withException(new IOException("falha em ?key=" + CHAVE)));

        assertThatThrownBy(() -> provider.geocodificar("qualquer")).satisfies(this::assertSemChave);
    }

    @Test
    void servidorQueNaoRespondeEstouraOTimeoutEViraIndisponivel() throws IOException {
        //aceita a conexão e nunca responde
        try (ServerSocket mudo = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            String base = "http://" + mudo.getInetAddress().getHostAddress() + ":" + mudo.getLocalPort();
            GoogleMapsProvider comTimeout = new GoogleMapsProvider(
                    GoogleMapsProvider.clienteComTimeout(Duration.ofMillis(200)), CHAVE, base + "/rotas",
                    base + "/geocode", Clock.fixed(AGORA, ZoneOffset.UTC));

            assertThatThrownBy(() -> comTimeout.otimizarRota(PARTIDA, UMA_PARADA, DESTINO, DEPOIS))
                    .satisfies(this::assertSemChave);
            assertThatThrownBy(() -> comTimeout.geocodificar("qualquer")).satisfies(this::assertSemChave);
        }
    }

    @Test
    void timeoutPadraoECurto() {
        assertThat(GoogleMapsProvider.TIMEOUT).isLessThanOrEqualTo(Duration.ofSeconds(5));
    }
}
