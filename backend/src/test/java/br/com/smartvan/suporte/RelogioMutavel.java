package br.com.smartvan.suporte;

import br.com.smartvan.config.RelogioConfig;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * {@link Clock} mutável dos testes. Começa em {@link #INICIAL} (2026-11-03 09:00, São Paulo) e o teste pode
 * avançar/definir o "agora". É reiniciado antes de cada teste pelo {@link TesteIntegracaoBase}.
 */
public class RelogioMutavel extends Clock {

    public static final ZoneId ZONA = RelogioConfig.ZONA;
    public static final Instant INICIAL = LocalDateTime.of(2026, 11, 3, 9, 0).atZone(ZONA).toInstant();

    private volatile Instant agora = INICIAL;

    /** Define a data (mantém o horário atual do relógio). */
    public void definirData(LocalDate data) {
        LocalTime hora = agora.atZone(ZONA).toLocalTime();
        agora = data.atTime(hora).atZone(ZONA).toInstant();
    }

    public void definir(LocalDateTime dataHora) {
        agora = dataHora.atZone(ZONA).toInstant();
    }

    public void avancarDias(long dias) {
        agora = agora.atZone(ZONA).plusDays(dias).toInstant();
    }

    public void avancarSegundos(long segundos) {
        agora = agora.plusSeconds(segundos);
    }

    public void reiniciar() {
        agora = INICIAL;
    }

    public LocalDate hoje() {
        return LocalDate.now(this);
    }

    @Override
    public ZoneId getZone() {
        return ZONA;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(agora, zone);
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
