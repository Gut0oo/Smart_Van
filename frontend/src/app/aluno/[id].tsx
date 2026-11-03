import React, { useState } from 'react';
import { Text, TouchableOpacity, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';

import { Aviso, Botao, Campo, Carregando, Cartao, Linha, Secao, Tela, s } from '@/components/ui';
import { api } from '@/services/api';
import { data, dataParaApi } from '@/services/formato';
import { Aluno, Falta } from '@/services/tipos';
import { mensagemDeErro, useCarregar } from '@/services/useCarregar';

// Dados do aluno e faltas: o motorista é avisado na hora quando uma falta é registrada.
export default function AlunoScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { dados, carregando, erro, recarregar } = useCarregar(async () => {
    const [alunos, faltas] = await Promise.all([
      api.get<Aluno[]>('/responsavel/alunos'),
      api.get<Falta[]>(`/alunos/${id}/faltas`),
    ]);
    return { aluno: alunos.find((a) => String(a.id) === id), faltas };
  }, [id]);

  const [dia, setDia] = useState('');
  const [motivo, setMotivo] = useState('');
  const [erroAcao, setErroAcao] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const dataFalta = dataParaApi(dia);

  async function registrar() {
    setErroAcao(null);
    setEnviando(true);
    try {
      await api.post(`/alunos/${id}/faltas`, { data: dataFalta, motivo: motivo.trim() || null });
      setDia('');
      setMotivo('');
      await recarregar();
    } catch (e) {
      setErroAcao(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  async function remover(dataIso: string) {
    setErroAcao(null);
    try {
      await api.delete(`/alunos/${id}/faltas/${dataIso}`);
      await recarregar();
    } catch (e) {
      setErroAcao(mensagemDeErro(e));
    }
  }

  const a = dados?.aluno;

  return (
    <Tela titulo={a?.nome ?? 'Aluno'} voltar carregando={carregando} aoAtualizar={recarregar}>
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {a && (
        <Secao titulo="Dados">
          <Cartao>
            <Linha rotulo="Escola" valor={a.escola.nome} />
            <Linha rotulo="Nascimento" valor={data(a.dataNascimento)} />
            {a.serie && <Linha rotulo="Série" valor={a.serie} />}
            <Linha rotulo="Endereço" valor={`${a.endereco.logradouro}, ${a.endereco.numero}`} />
          </Cartao>
        </Secao>
      )}

      {dados && (
        <>
          <Secao titulo="Avisar falta">
            <Campo
              rotulo="Dia da falta"
              placeholder="DD/MM/AAAA"
              value={dia}
              onChangeText={setDia}
              keyboardType="numbers-and-punctuation"
              maxLength={10}
            />
            <Campo rotulo="Motivo (opcional)" value={motivo} onChangeText={setMotivo} />
            {erroAcao && <Aviso texto={erroAcao} erro />}
            <Botao titulo="Registrar falta" aoTocar={registrar} carregando={enviando} desativado={!dataFalta} />
            <Text style={[s.pequeno, { marginTop: 8 }]}>O motorista recebe um aviso na hora.</Text>
          </Secao>

          <Secao titulo="Próximas faltas">
            {dados.faltas.length === 0 && <Aviso texto="Nenhuma falta registrada." />}
            {dados.faltas.map((f) => (
              <Cartao key={f.id}>
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  <View style={{ flex: 1 }}>
                    <Text style={s.titulo}>{data(f.data)}</Text>
                    {f.motivo && <Text style={s.pequeno}>{f.motivo}</Text>}
                  </View>
                  <TouchableOpacity onPress={() => remover(f.data)} accessibilityLabel="Remover falta">
                    <Ionicons name="trash-outline" size={20} color="#555" />
                  </TouchableOpacity>
                </View>
              </Cartao>
            ))}
          </Secao>
        </>
      )}
    </Tela>
  );
}
