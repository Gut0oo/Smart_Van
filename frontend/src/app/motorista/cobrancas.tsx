import React, { useState } from 'react';
import { Text, View } from 'react-native';

import { Aviso, Botao, Carregando, Cartao, Opcoes, Selo, Tela, confirmar, s } from '@/components/ui';
import { api } from '@/services/api';
import { competencia, data, dinheiro, STATUS_MENSALIDADE } from '@/services/formato';
import { Mensalidade } from '@/services/tipos';
import { mensagemDeErro, useCarregar } from '@/services/useCarregar';

type Filtro = 'ABERTAS' | 'PAGA' | 'TODAS';

export default function CobrancasScreen() {
  const [filtro, setFiltro] = useState<Filtro>('ABERTAS');
  const { dados, carregando, erro, recarregar } = useCarregar(() => api.get<Mensalidade[]>('/motorista/cobrancas'));
  const [baixando, setBaixando] = useState<number | null>(null);
  const [erroBaixa, setErroBaixa] = useState<string | null>(null);

  const lista =
    dados?.filter((m) => (filtro === 'TODAS' ? true : filtro === 'PAGA' ? m.status === 'PAGA' : m.status !== 'PAGA')) ?? [];
  const totalAberto = dados?.filter((m) => m.status !== 'PAGA').reduce((t, m) => t + Number(m.valor), 0) ?? 0;

  async function baixa(m: Mensalidade) {
    setErroBaixa(null);
    setBaixando(m.id);
    try {
      await api.post(`/mensalidades/${m.id}/baixa`, {});
      await recarregar();
    } catch (e) {
      setErroBaixa(mensagemDeErro(e));
    } finally {
      setBaixando(null);
    }
  }

  return (
    <Tela titulo="Cobranças" carregando={carregando} aoAtualizar={recarregar}>
      <Cartao destaque>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
          <Text style={s.pequeno}>A RECEBER</Text>
          <Text style={{ fontSize: 24, fontWeight: 'bold', color: '#111' }}>{dinheiro(totalAberto)}</Text>
        </View>
      </Cartao>
      <Opcoes
        valor={filtro}
        aoMudar={setFiltro}
        opcoes={[
          { valor: 'ABERTAS', texto: 'Em aberto' },
          { valor: 'PAGA', texto: 'Pagas' },
          { valor: 'TODAS', texto: 'Todas' },
        ]}
      />
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {erroBaixa && <Aviso texto={erroBaixa} erro />}
      {dados && lista.length === 0 && <Aviso texto="Nenhuma cobrança nesta lista." />}
      {lista.map((m) => (
        <Cartao key={m.id} destaque={m.status === 'ATRASADA'}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 8 }}>
            <Text style={[s.titulo, { flex: 1 }]}>
              {m.aluno.nome} · {competencia(m.competencia)}
            </Text>
            <Selo texto={STATUS_MENSALIDADE[m.status]} escuro={m.status === 'ATRASADA'} />
          </View>
          <Text style={s.subtitulo}>
            {dinheiro(m.valor)} · vence {data(m.vencimento)}
          </Text>
          <Text style={s.pequeno}>
            {m.responsavel.nome} · {m.responsavel.telefone}
          </Text>
          {m.dataPagamento && <Text style={s.pequeno}>Pago em {data(m.dataPagamento)}</Text>}
          {m.status !== 'PAGA' && (
            <View style={{ marginTop: 12 }}>
              <Botao
                titulo="Registrar pagamento"
                secundario
                carregando={baixando === m.id}
                aoTocar={() =>
                  confirmar('Registrar pagamento', `Confirmar o pagamento de ${m.aluno.nome} (${competencia(m.competencia)}) hoje?`, () =>
                    baixa(m),
                  )
                }
              />
            </View>
          )}
        </Cartao>
      ))}
    </Tela>
  );
}
