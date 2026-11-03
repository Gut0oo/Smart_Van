import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

import { Aviso, Carregando, Tela, cores } from '@/components/ui';
import { api } from '@/services/api';
import { dataHora } from '@/services/formato';
import { Notificacao } from '@/services/tipos';
import { useCarregar } from '@/services/useCarregar';

// Lista de notificações do usuário logado; tocar no X marca como lida.
export function TelaNotificacoes() {
  const { dados, carregando, erro, recarregar } = useCarregar(() => api.get<Notificacao[]>('/notificacoes'));

  async function marcarLida(id: number) {
    try {
      await api.post(`/notificacoes/${id}/lida`);
    } finally {
      recarregar();
    }
  }

  return (
    <Tela titulo="Notificações" carregando={carregando} aoAtualizar={recarregar}>
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {dados?.length === 0 && <Aviso texto="Nenhuma notificação por enquanto." />}
      {dados?.map((n) => (
        <View key={n.id} style={[styles.card, !n.lida && styles.cardNaoLida]}>
          <View style={styles.cardHeader}>
            <Text style={styles.titulo}>{n.titulo}</Text>
            <Text style={styles.data}>{dataHora(n.dataCriacao)}</Text>
            {!n.lida && (
              <TouchableOpacity onPress={() => marcarLida(n.id)} accessibilityLabel="Marcar como lida">
                <Ionicons name="close" size={22} color="#111" />
              </TouchableOpacity>
            )}
          </View>
          <Text style={styles.mensagem}>{n.mensagem}</Text>
        </View>
      ))}
    </Tela>
  );
}

const styles = StyleSheet.create({
  card: { borderWidth: 1, borderColor: cores.borda, borderRadius: 12, padding: 16, marginBottom: 12 },
  cardNaoLida: { borderColor: '#222', backgroundColor: cores.fundoSuave },
  cardHeader: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 6 },
  titulo: { flex: 1, fontSize: 15, fontWeight: 'bold', color: cores.texto },
  data: { fontSize: 11, color: cores.textoClaro },
  mensagem: { fontSize: 13, color: cores.textoMedio, lineHeight: 18 },
});
