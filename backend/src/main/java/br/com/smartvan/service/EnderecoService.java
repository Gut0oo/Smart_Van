package br.com.smartvan.service;

import br.com.smartvan.dto.EnderecoRequestDTO;
import br.com.smartvan.maps.Coordenada;
import br.com.smartvan.maps.MapsProvider;
import br.com.smartvan.model.Endereco;
import br.com.smartvan.repository.EnderecoRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

//cria/atualiza endereços e geocodifica quando o cliente não mandou latitude e longitude
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
            Optional<Coordenada> c = maps.geocodificar(texto(req));
            e.setLatitude(c.map(Coordenada::latitude).orElse(null));
            e.setLongitude(c.map(Coordenada::longitude).orElse(null));
        }
        return enderecos.save(e);
    }

    private static String texto(EnderecoRequestDTO r) {
        return "%s, %s, %s, %s - %s, %s".formatted(r.logradouro(), r.numero(), r.bairro(), r.cidade(), r.uf(), r.cep());
    }
}
