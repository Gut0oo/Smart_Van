import React from 'react';
import { Linking, StyleSheet, Text, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';

import { Aviso, Botao, Carregando, Cartao, Linha, Secao, Tela, cores, s } from '@/components/ui';
import { api } from '@/services/api';
import { data, hora } from '@/services/formato';
import { Rota } from '@/services/tipos';
import { useCarregar } from '@/services/useCarregar';

// O link de rotas do Google Maps aceita até 9 paradas intermediárias.
const MAX_PARADAS_LINK = 9;

// Monta o link do Google Maps com as paradas na ordem da rota. Sem origem, o Maps parte de onde o motorista está.
function linkGoogleMaps(r: Rota) {
  const pontos = r.paradas
    .filter((p) => p.endereco.latitude != null && p.endereco.longitude != null)
    .map((p) => `${p.endereco.latitude},${p.endereco.longitude}`);
  if (pontos.length === 0) return null;
  const destino = pontos[pontos.length - 1];
  const intermediarios = pontos.slice(0, -1);
  const params = new URLSearchParams({ api: '1', destination: destino, travelmode: 'driving' });
  if (intermediarios.length > 0) params.set('waypoints', intermediarios.join('|'));
  return { url: `https://www.google.com/maps/dir/?${params.toString()}`, intermediarios: intermediarios.length };
}

export default function RotaDetalhe() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { dados: r, carregando, erro, recarregar } = useCarregar(() => api.get<Rota>(`/motorista/rotas/${id}`), [id]);
  const link = r ? linkGoogleMaps(r) : null;

  return (
    <Tela
      titulo="Rota"
      voltar
      carregando={carregando}
      aoAtualizar={recarregar}
      rodape={
        link && (
          <>
            {link.intermediarios > MAX_PARADAS_LINK && (
              <Aviso texto="A rota tem mais paradas do que o Google Maps aceita num link: abra e confira as últimas paradas pela lista." erro />
            )}
            <Botao titulo="Abrir rota no Google Maps" aoTocar={() => Linking.openURL(link.url)} />
          </>
        )
      }>
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

          <Secao titulo="Paradas (registro)">
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
