package br.com.smartvan.event;

/** Publicado pela contratação quando um contrato vira ATIVO. As mensalidades escutam. */
public record ContratoAtivadoEvent(Long contratoId) {
}
