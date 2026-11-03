import React, { useState } from 'react';
import { useRouter } from 'expo-router';

import { Aviso, Botao, Campo, Opcoes, Tela } from '@/components/ui';
import { api } from '@/services/api';
import { mensagemDeErro } from '@/services/useCarregar';

type TipoVeiculo = 'VAN' | 'MICRO_ONIBUS' | 'CARRO';

export default function NovoVeiculo() {
  const router = useRouter();
  const [tipo, setTipo] = useState<TipoVeiculo>('VAN');
  const [placa, setPlaca] = useState('');
  const [modelo, setModelo] = useState('');
  const [ano, setAno] = useState('');
  const [capacidade, setCapacidade] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const cap = Number(capacidade);
  const completo = placa.trim() && modelo.trim() && /^\d{4}$/.test(ano) && cap >= 1 && cap <= 60;

  async function salvar() {
    setErro(null);
    setEnviando(true);
    try {
      await api.post('/motorista/veiculos', {
        placa: placa.trim().toUpperCase(),
        modelo: modelo.trim(),
        ano: Number(ano),
        capacidade: cap,
        tipo,
      });
      router.back();
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tela
      titulo="Adicionar veículo"
      voltar
      rodape={<Botao titulo="Salvar veículo" aoTocar={salvar} carregando={enviando} desativado={!completo} />}>
      <Opcoes
        rotulo="Tipo"
        valor={tipo}
        aoMudar={setTipo}
        opcoes={[
          { valor: 'VAN', texto: 'Van' },
          { valor: 'MICRO_ONIBUS', texto: 'Micro-ônibus' },
          { valor: 'CARRO', texto: 'Carro' },
        ]}
      />
      <Campo rotulo="Placa" value={placa} onChangeText={setPlaca} autoCapitalize="characters" maxLength={8} />
      <Campo rotulo="Modelo" placeholder="Renault Master" value={modelo} onChangeText={setModelo} />
      <Campo rotulo="Ano" value={ano} onChangeText={setAno} keyboardType="number-pad" maxLength={4} />
      <Campo
        rotulo="Lugares para alunos (1 a 60)"
        value={capacidade}
        onChangeText={setCapacidade}
        keyboardType="number-pad"
        maxLength={2}
      />
      {erro && <Aviso texto={erro} erro />}
    </Tela>
  );
}
