package br.com.smartvan.service;

import java.time.LocalDate;

//vencimento mensal do contrato: todo mês no dia do início; em mês mais curto, no último dia
final class VencimentoMensal {

    private VencimentoMensal() {
    }

    /** Vencimento do contrato no mês da competência. */
    static LocalDate noMes(LocalDate dataInicio, LocalDate competencia) {
        LocalDate mes = competencia.withDayOfMonth(1);
        return mes.withDayOfMonth(Math.min(dataInicio.getDayOfMonth(), mes.lengthOfMonth()));
    }

    /** Primeiro vencimento depois da data. Se o contrato ainda não começou, é o próprio início. */
    static LocalDate proximoDepoisDe(LocalDate dataInicio, LocalDate data) {
        if (dataInicio.isAfter(data)) {
            return dataInicio;
        }
        LocalDate desteMes = noMes(dataInicio, data);
        return desteMes.isAfter(data) ? desteMes : noMes(dataInicio, data.withDayOfMonth(1).plusMonths(1));
    }
}
