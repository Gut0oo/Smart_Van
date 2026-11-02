package br.com.smartvan.service;

import br.com.smartvan.dto.EnderecoRequestDTO;
import br.com.smartvan.maps.Coordenada;
import br.com.smartvan.maps.MapsIndisponivelException;
import br.com.smartvan.maps.MapsProvider;
import br.com.smartvan.model.Endereco;
import br.com.smartvan.repository.EnderecoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

//cria/atualiza endereços e geocodifica quando o cliente não mandou latitude e longitude
@Slf4j
@Service
public class EnderecoService {

    private final EnderecoRepository enderecos;
    private final MapsProvider maps;

    public EnderecoService(EnderecoRepository enderecos, MapsProvider maps) {
        this.enderecos = enderecos;
        this.maps = maps;
    }

    public Endereco criar(EnderecoRequestDTO req) {
        return atualizar(new Endereco(), req);
    }

    public Endereco atualizar(Endereco e, EnderecoRequestDTO req) {
        e.setLogradouro(req.logradouro().trim());
        e.setNumero(req.numero().trim());
        e.setComplemento(req.complemento());
        e.setBairro(req.bairro().trim());
        e.setCidade(req.cidade().trim());
        e.setUf(req.uf());
        e.setCep(req.cep());
        if (req.latitude() != null && req.longitude() != null) {
            e.setLatitude(req.latitude());
            e.setLongitude(req.longitude());
        } else {
            Optional<Coordenada> c = geocodificar(texto(req));
            e.setLatitude(c.map(Coordenada::latitude).orElse(null));
            e.setLongitude(c.map(Coordenada::longitude).orElse(null));
        }
        return enderecos.save(e);
    }

    //coordenada do endereço; se ele foi gravado sem, tenta geocodificar de novo e grava o que vier
    public Optional<Coordenada> coordenadaDe(Endereco e) {
        if (e.getLatitude() != null && e.getLongitude() != null) {
            return Optional.of(new Coordenada(e.getLatitude(), e.getLongitude()));
        }
        Optional<Coordenada> c = geocodificar("%s, %s, %s, %s - %s, %s".formatted(e.getLogradouro(), e.getNumero(),
                e.getBairro(), e.getCidade(), e.getUf(), e.getCep()));
        c.ifPresent(coordenada -> {
            e.setLatitude(coordenada.latitude());
            e.setLongitude(coordenada.longitude());
            enderecos.save(e);
        });
        return c;
    }

    //mapas fora do ar não impede salvar o endereço: ele fica sem coordenadas e a rota tenta de novo
    private Optional<Coordenada> geocodificar(String texto) {
        try {
            return maps.geocodificar(texto);
        } catch (MapsIndisponivelException ex) {
            log.warn("Geocodificação indisponível: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static String texto(EnderecoRequestDTO r) {
        return "%s, %s, %s, %s - %s, %s".formatted(r.logradouro(), r.numero(), r.bairro(), r.cidade(), r.uf(), r.cep());
    }
}
