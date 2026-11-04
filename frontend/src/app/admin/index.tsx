import React, { useState } from 'react';
import { Text, View } from 'react-native';

import { Aviso, Botao, Campo, Carregando, Cartao, Tela, s } from '@/components/ui';
import { api, Usuario } from '@/services/api';
import { mensagemDeErro, useCarregar } from '@/services/useCarregar';

// Motoristas aguardando aprovação; rejeitar exige parecer.
export default function ValidacaoMotoristas() {
  const { dados, carregando, erro, recarregar } = useCarregar(() => api.get<Usuario[]>('/admin/motoristas/pendentes'));
  const [rejeitando, setRejeitando] = useState<number | null>(null);
  const [parecer, setParecer] = useState('');
  const [acao, setAcao] = useState<string | null>(null);
  const [erroAcao, setErroAcao] = useState<string | null>(null);

  async function decidir(id: number, decisao: 'VALIDADO' | 'REJEITADO') {
    setErroAcao(null);
    setAcao(`${decisao}${id}`);
    try {
      await api.post(`/admin/motoristas/${id}/validacao`, {
        decisao,
        parecer: decisao === 'REJEITADO' ? parecer.trim() : undefined,
      });
      setRejeitando(null);
      setParecer('');
      await recarregar();
    } catch (e) {
      setErroAcao(mensagemDeErro(e));
    } finally {
      setAcao(null);
    }
  }

  return (
    <Tela titulo="Aprovar motoristas" carregando={carregando} aoAtualizar={recarregar}>
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {erroAcao && <Aviso texto={erroAcao} erro />}
      {dados?.length === 0 && <Aviso texto="Nenhum motorista aguardando aprovação." />}
      {dados?.map((m) => (
        <Cartao key={m.id}>
          <Text style={s.titulo}>{m.nome}</Text>
          <Text style={s.subtitulo}>{m.email}</Text>
          <Text style={s.pequeno}>
            CPF {m.cpf} · {m.telefone}
          </Text>
          {rejeitando === m.id ? (
            <View style={{ marginTop: 12 }}>
              <Campo rotulo="Motivo da rejeição" value={parecer} onChangeText={setParecer} />
              <View style={{ gap: 8 }}>
                <Botao
                  titulo="Confirmar rejeição"
                  perigo
                  desativado={!parecer.trim()}
                  carregando={acao === `REJEITADO${m.id}`}
                  aoTocar={() => decidir(m.id, 'REJEITADO')}
                />
                <Botao titulo="Voltar" secundario aoTocar={() => setRejeitando(null)} />
              </View>
            </View>
          ) : (
            <View style={{ flexDirection: 'row', gap: 8, marginTop: 12 }}>
              <View style={{ flex: 1 }}>
                <Botao titulo="Aprovar" carregando={acao === `VALIDADO${m.id}`} aoTocar={() => decidir(m.id, 'VALIDADO')} />
              </View>
              <View style={{ flex: 1 }}>
                <Botao titulo="Rejeitar" secundario aoTocar={() => setRejeitando(m.id)} />
              </View>
            </View>
          )}
        </Cartao>
      ))}
    </Tela>
  );
}
