package br.com.smartvan.maps;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OrdenadorPorProximidadeTest {

    private static Coordenada c(String lat, String lng) {
        return new Coordenada(new BigDecimal(lat), new BigDecimal(lng));
    }

    @Test
    void haversineConhecida() {
        //1 grau de latitude = ~111,19 km
        assertThat(OrdenadorPorProximidade.haversineKm(c("0", "0"), c("1", "0"))).isCloseTo(111.19, within(0.1));
        assertThat(OrdenadorPorProximidade.haversineKm(c("-23.5", "-46.6"), c("-23.5", "-46.6"))).isZero();
    }

    @Test
    void vizinhoMaisProximoAPartirDaPartida() {
        //índices: 0 = longe, 1 = médio, 2 = perto
        List<Coordenada> paradas = List.of(c("0", "0.30"), c("0", "0.20"), c("0", "0.10"));

        RotaOtimizada r = OrdenadorPorProximidade.ordenar(c("0", "0"), paradas, c("0", "0.40"));

        assertThat(r.ordemParadas()).containsExactly(2, 1, 0);
        assertThat(r.polyline()).isNull();
        assertThat(r.segundosAteCadaParada()).isSorted().hasSize(3);
        assertThat(r.segundosTotal()).isGreaterThan(r.segundosAteCadaParada().getLast());
    }

    @Test
    void distanciaEHaversineVezes1Virgula3ETempoA25KmPorHora() {
        Coordenada a = c("0", "0");
        Coordenada b = c("0", "0.1");
        double km = OrdenadorPorProximidade.haversineKm(a, b) * 1.3;

        RotaOtimizada r = OrdenadorPorProximidade.ordenar(a, List.of(b), b);

        assertThat(r.metrosTotal()).isCloseTo((int) Math.round(km * 1000), within(1));
        assertThat(r.segundosAteCadaParada().getFirst()).isCloseTo((int) Math.round(km / 25 * 3600), within(1));
        assertThat(r.segundosTotal()).isEqualTo(r.segundosAteCadaParada().getFirst());
    }

    @Test
    void semParadasVaiDiretoAoDestino() {
        RotaOtimizada r = OrdenadorPorProximidade.ordenar(c("0", "0"), List.of(), c("0", "0.1"));

        assertThat(r.ordemParadas()).isEmpty();
        assertThat(r.segundosAteCadaParada()).isEmpty();
        assertThat(r.segundosTotal()).isPositive();
    }
}
