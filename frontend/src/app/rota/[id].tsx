import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';

import { Aviso, Carregando, Cartao, Linha, Secao, Tela, cores, s } from '@/components/ui';
import { api } from '@/services/api';
import { data, hora } from '@/services/formato';
import { Rota } from '@/services/tipos';
import { useCarregar } from '@/services/useCarregar';

export default function RotaDetalhe() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { dados: r, carregando, erro, recarregar } = useCarregar(() => api.get<Rota>(`/motorista/rotas/${id}`), [id]);

  return (
    <Tela titulo="Rota" voltar carregando={carregando} aoAtualizar={recarregar}>
      {carregando && !r && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {r && (
        <>
          {r.foraDoTeto && (
            <Aviso
              erro
              texto={`A chegada à escola passa do limite em ${r.minutosAdiantar} min. Adiante a saída e gere a rota de novo.`}
            />
          )}
          <Cartao destaque>
            <Text style={s.titulo}>
              {r.tipo === 'IDA' ? 'Ida' : 'Volta'} · {r.escola.nome}
            </Text>
            <Linha rotulo="Data" valor={data(r.data)} />
            <Linha rotulo="Saída" valor={hora(r.horarioInicio)} />
            <Linha rotulo="Chegada" valor={hora(r.horarioFim)} />
            <Linha rotulo="Distância" valor={r.distanciaKm != null ? `${Number(r.distanciaKm).toFixed(1)} km` : '—'} />
            <Linha rotulo="Tempo estimado" valor={r.tempoEstimadoMinutos != null ? `${r.tempoEstimadoMinutos} min` : '—'} />
            <Linha rotulo="Veículo" valor={`${r.veiculo.modelo} · ${r.veiculo.placa}`} />
            <Linha rotulo="Cálculo" valor={r.otimizada ? 'Otimizada pelo Google' : 'Estimativa por proximidade'} />
          </Cartao>

          <Secao titulo="Paradas">
            {r.paradas.map((p) => (
              <View key={p.ordem} style={styles.parada}>
                <View style={styles.ordem}>
                  <Text style={styles.textoOrdem}>{p.ordem}</Text>
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={s.titulo}>
                    {p.aluno ? p.aluno.nome : r.escola.nome}
                  </Text>
                  <Text style={s.pequeno}>
                    {p.tipoParada === 'EMBARQUE' ? 'Embarque' : 'Desembarque'} · {p.endereco.logradouro}, {p.endereco.numero}
                  </Text>
                </View>
                <Text style={styles.hora}>{hora(p.horarioPrevisto)}</Text>
              </View>
            ))}
          </Secao>
        </>
      )}
    </Tela>
  );
}

const styles = StyleSheet.create({
  parada: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderWidth: 1,
    borderColor: cores.borda,
    borderRadius: 12,
    padding: 12,
    marginBottom: 8,
  },
  ordem: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: cores.botao,
    justifyContent: 'center',
    alignItems: 'center',
  },
  textoOrdem: { color: '#fff', fontWeight: 'bold', fontSize: 13 },
  hora: { fontSize: 16, fontWeight: 'bold', color: cores.texto },
});
