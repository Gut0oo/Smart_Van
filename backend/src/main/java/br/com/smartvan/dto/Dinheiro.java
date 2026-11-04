package br.com.smartvan.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valores em dinheiro saem da API sempre com 2 casas (Convenções do contrato). */
final class Dinheiro {

    private Dinheiro() {
    }

    static BigDecimal duasCasas(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
