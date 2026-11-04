import React, { useEffect, useState } from 'react';

import { CamposEndereco, enderecoCompleto, enderecoDe, enderecoParaApi, enderecoVazio } from '@/components/formularios';
import { Aviso, Botao, Tela, useVoltar } from '@/components/ui';
import { api } from '@/services/api';
import { Endereco } from '@/services/tipos';
import { mensagemDeErro } from '@/services/useCarregar';

// Endereço de onde a van sai na ida e para onde volta no fim da volta.
export default function EnderecoPartida() {
  const voltar = useVoltar();
  const [endereco, setEndereco] = useState(enderecoVazio);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    api
      .get<{ enderecoPartida?: Endereco | null }>('/motorista/perfil')
      .then((p) => setEndereco(enderecoDe(p.enderecoPartida)))
      .catch(() => {});
  }, []);

  async function salvar() {
    setErro(null);
    setEnviando(true);
    try {
      const salvo = await api.put<Endereco>('/motorista/endereco-partida', enderecoParaApi(endereco));
      if (salvo.latitude == null) {
        setErro('Endereço salvo, mas não foi possível localizar no mapa. Confira rua, número e CEP.');
        return;
      }
      voltar('/motorista/conta');
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tela
      titulo="Endereço de partida"
      voltar
      rodape={<Botao titulo="Salvar endereço" aoTocar={salvar} carregando={enviando} desativado={!enderecoCompleto(endereco)} />}>
      <Aviso texto="De onde a van sai na ida e para onde volta no fim da volta (casa ou garagem)." />
      <CamposEndereco valor={endereco} aoMudar={setEndereco} />
      {erro && <Aviso texto={erro} erro />}
    </Tela>
  );
}
