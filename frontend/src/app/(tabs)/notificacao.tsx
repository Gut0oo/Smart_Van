import React from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';

const NOTIFICACOES_MOCK = [
  {
    id: '1',
    titulo: 'Proposta enviada!',
    mensagem: 'Sua solicitação para Carlos Mendes foi entregue.',
    data: 'Hoje, 14:30',
    lida: false,
  },
  {
    id: '2',
    titulo: 'Proposta aceita!',
    mensagem: 'Sua solicitação para Carlos Mendes foi aceita.',
    data: 'Hoje, 16:30',
    lida: false,
  },
];

export default function NotificacaoScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Ionicons name="notifications-outline" size={22} color="#111" />
        <Text style={styles.tituloHeader}>Notificações</Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {NOTIFICACOES_MOCK.map((item) => (
          <View
            key={item.id}
            style={[styles.card, !item.lida && styles.cardNaoLida]}
          >
            <View style={styles.cardHeader}>
              <Text style={styles.tituloNotificacao}>{item.titulo}</Text>
              <Text style={styles.dataNotificacao}>{item.data}</Text>
              {/*envia pro banco que já leu*/}
              <TouchableOpacity>
                <Ionicons name="close" size={22} color="#111" />
              </TouchableOpacity>
            </View>
            <Text style={styles.mensagemNotificacao}>{item.mensagem}</Text>
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  tituloHeader: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#111',
  },
  content: {
    padding: 16,
    gap: 12,
  },
  card: {
    borderWidth: 1,
    borderColor: '#e0e0e0',
    borderRadius: 12,
    padding: 16,
    backgroundColor: '#fff',
  },
  cardNaoLida: {
    borderColor: '#222',
    backgroundColor: '#f9f9f9',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  tituloNotificacao: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#111',
  },
  dataNotificacao: {
    fontSize: 11,
    color: '#888',
  },
  mensagemNotificacao: {
    fontSize: 13,
    color: '#555',
    lineHeight: 18,
  },
});