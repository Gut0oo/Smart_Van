import React, { useState } from 'react';
import { Text } from 'react-native';

import { CamposEndereco, enderecoCompleto, enderecoParaApi, enderecoVazio } from '@/components/formularios';
import { Aviso, Botao, Campo, Carregando, Cartao, Secao, Tela, s } from '@/components/ui';
import { api } from '@/services/api';
import { Escola } from '@/services/tipos';
import { mensagemDeErro, useCarregar } from '@/services/useCarregar';

export default function EscolasAdmin() {
  const { dados, carregando, erro, recarregar } = useCarregar(() => api.get<Escola[]>('/escolas'));
  const [nome, setNome] = useState('');
  const [telefone, setTelefone] = useState('');
  const [endereco, setEndereco] = useState(enderecoVazio);
  const [erroForm, setErroForm] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function salvar() {
    setErroForm(null);
    setAviso(null);
    setEnviando(true);
    try {
      const e = await api.post<Escola>('/escolas', {
        nome: nome.trim(),
        telefone: telefone.trim() || null,
        endereco: enderecoParaApi(endereco),
      });
      setNome('');
      setTelefone('');
      setEndereco(enderecoVazio);
      setAviso(
        e.endereco.latitude == null
          ? 'Escola cadastrada, mas o endereço não foi localizado no mapa. Rotas para ela vão falhar.'
          : 'Escola cadastrada.',
      );
      await recarregar();
    } catch (e) {
      setErroForm(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tela titulo="Escolas" carregando={carregando} aoAtualizar={recarregar}>
      <Secao titulo="Nova escola">
        <Campo rotulo="Nome" value={nome} onChangeText={setNome} />
        <Campo rotulo="Telefone (opcional)" value={telefone} onChangeText={setTelefone} keyboardType="phone-pad" />
        <CamposEndereco valor={endereco} aoMudar={setEndereco} />
        {erroForm && <Aviso texto={erroForm} erro />}
        {aviso && <Aviso texto={aviso} />}
        <Botao
          titulo="Cadastrar escola"
          aoTocar={salvar}
          carregando={enviando}
          desativado={!nome.trim() || !enderecoCompleto(endereco)}
        />
      </Secao>

      <Secao titulo="Escolas cadastradas">
        {carregando && !dados && <Carregando />}
        {erro && <Aviso texto={erro} erro />}
        {dados?.length === 0 && <Aviso texto="Nenhuma escola ainda." />}
        {dados?.map((e) => (
          <Cartao key={e.id}>
            <Text style={s.titulo}>{e.nome}</Text>
            <Text style={s.subtitulo}>
              {e.endereco.logradouro}, {e.endereco.numero} · {e.endereco.bairro}
            </Text>
            <Text style={s.pequeno}>
              {e.endereco.cidade}/{e.endereco.uf}
              {e.endereco.latitude == null ? ' · sem localização no mapa' : ''}
            </Text>
          </Cartao>
        ))}
      </Secao>
    </Tela>
  );
}
