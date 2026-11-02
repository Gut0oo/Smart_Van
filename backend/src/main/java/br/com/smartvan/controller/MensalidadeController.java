package br.com.smartvan.controller;

import br.com.smartvan.dto.BaixaMensalidadeRequestDTO;
import br.com.smartvan.dto.MensalidadeResponseDTO;
import br.com.smartvan.enums.StatusMensalidade;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.security.Papel;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.MensalidadeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
public class MensalidadeController {

    private final MensalidadeService service;

    public MensalidadeController(MensalidadeService service) {
        this.service = service;
    }

    @GetMapping("/contratos/{id}/mensalidades")
    public List<MensalidadeResponseDTO> doContrato(@PathVariable Long id) {
        return service.listarDoContrato(UsuarioLogado.atual().id(), id);
    }

    @GetMapping("/responsavel/mensalidades")
    @Papel(TipoUsuario.RESPONSAVEL)
    public List<MensalidadeResponseDTO> doResponsavel(@RequestParam(required = false) StatusMensalidade status) {
        return service.listarDoResponsavel(UsuarioLogado.atual().id(), status);
    }

    @GetMapping("/motorista/cobrancas")
    @Papel(TipoUsuario.MOTORISTA)
    public List<MensalidadeResponseDTO> cobrancas(@RequestParam(required = false) StatusMensalidade status,
                                                  @RequestParam(required = false) YearMonth competencia) {
        return service.listarCobrancas(UsuarioLogado.atual().id(), status, competencia);
    }

    @PostMapping("/mensalidades/{id}/baixa")
    @Papel(TipoUsuario.MOTORISTA)
    public MensalidadeResponseDTO baixa(@PathVariable Long id,
                                        @Valid @RequestBody(required = false) BaixaMensalidadeRequestDTO corpo) {
        return service.darBaixa(UsuarioLogado.atual().id(), id, corpo == null ? null : corpo.dataPagamento());
    }
}
