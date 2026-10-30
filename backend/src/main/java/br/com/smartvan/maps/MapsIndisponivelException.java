package br.com.smartvan.maps;

/** Serviço de mapas fora do ar ou não configurado: quem chama aciona o plano B (proximidade). */
public class MapsIndisponivelException extends RuntimeException {

    public MapsIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public MapsIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
