package br.com.smartvan.suporte;

import br.com.smartvan.maps.Coordenada;
import br.com.smartvan.maps.MapsIndisponivelException;
import br.com.smartvan.maps.MapsProvider;
import br.com.smartvan.maps.RotaOtimizada;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * MapsProvider falso. Comportamento padrão (volta a ele em {@link #reiniciar()}, chamado antes de cada teste):
 * <ul>
 *   <li>geocodificar: devolve sempre {@link #COORDENADA_PADRAO};</li>
 *   <li>otimizarRota: mantém a ordem recebida (0,1,2...), 300 s entre paradas, 2 km por trecho, polyline "fake".</li>
 * </ul>
 * Todas as chamadas ficam registradas em {@link #geocodificacoes} e {@link #otimizacoes}.
 */
public class FakeMapsProvider implements MapsProvider {

    public static final Coordenada COORDENADA_PADRAO =
            new Coordenada(new BigDecimal("-23.5650000"), new BigDecimal("-46.6900000"));

    @FunctionalInterface
    public interface Geocodificador {
        Optional<Coordenada> geocodificar(String enderecoCompleto);
    }

    @FunctionalInterface
    public interface Otimizador {
        RotaOtimizada otimizar(Coordenada partida, List<Coordenada> paradas, Coordenada destino, Instant saida);
    }

    /** Uma chamada a otimizarRota. */
    public record ChamadaOtimizacao(Coordenada partida, List<Coordenada> paradas, Coordenada destino, Instant saida) {
    }

    /** Textos de endereço recebidos em geocodificar, na ordem. */
    public final List<String> geocodificacoes = new ArrayList<>();
    /** Chamadas a otimizarRota, na ordem. */
    public final List<ChamadaOtimizacao> otimizacoes = new ArrayList<>();

    private Geocodificador geocodificador;
    private Otimizador otimizador;

    public FakeMapsProvider() {
        reiniciar();
    }

    // --- configuração (um teste troca o comportamento chamando estes métodos) ---

    public void reiniciar() {
        geocodificacoes.clear();
        otimizacoes.clear();
        geocodificador = texto -> Optional.of(COORDENADA_PADRAO);
        otimizador = (partida, paradas, destino, saida) -> ordem(IntStream.range(0, paradas.size()).boxed().toList());
    }

    /** Ex.: {@code maps.geocodificarComo(t -> Optional.empty())} para simular endereço não encontrado. */
    public void geocodificarComo(Geocodificador g) {
        this.geocodificador = g;
    }

    public void naoGeocodificar() {
        this.geocodificador = texto -> Optional.empty();
    }

    public void otimizarComo(Otimizador o) {
        this.otimizador = o;
    }

    /** Faz otimizarRota lançar {@link MapsIndisponivelException} (aciona o plano B de proximidade). */
    public void falharNaOtimizacao() {
        this.otimizador = (p, ps, d, s) -> {
            throw new MapsIndisponivelException("falha simulada no teste");
        };
    }

    /** Otimização devolve esta ordem de índices (tamanho deve ser o nº de paradas), 300 s entre paradas. */
    public void otimizarComOrdem(Integer... ordemDeIndices) {
        List<Integer> ordem = Arrays.asList(ordemDeIndices);
        this.otimizador = (p, ps, d, s) -> ordem(ordem);
    }

    /** Monta um RotaOtimizada simples: 300 s e 2 km por trecho, polyline "fake-polyline". */
    public static RotaOtimizada ordem(List<Integer> ordemParadas) {
        int n = ordemParadas.size();
        List<Integer> segundos = IntStream.rangeClosed(1, n).map(i -> i * 300).boxed().toList();
        return new RotaOtimizada(ordemParadas, segundos, (n + 1) * 300, (n + 1) * 2000, "fake-polyline");
    }

    // --- MapsProvider ---

    @Override
    public Optional<Coordenada> geocodificar(String enderecoCompleto) {
        geocodificacoes.add(enderecoCompleto);
        return geocodificador.geocodificar(enderecoCompleto);
    }

    @Override
    public RotaOtimizada otimizarRota(Coordenada partida, List<Coordenada> paradas, Coordenada destino, Instant saida) {
        otimizacoes.add(new ChamadaOtimizacao(partida, List.copyOf(paradas), destino, saida));
        return otimizador.otimizar(partida, paradas, destino, saida);
    }
}
