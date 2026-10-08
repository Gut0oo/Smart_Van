import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons, Feather } from '@expo/vector-icons';

export default function HomeScreen() {
  const router = useRouter();

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        
        {/* Header de Boas-Vindas */}
        <View style={styles.header}>
          <Text style={styles.saudacao}>Bem-vindo ao</Text>
          <Text style={styles.titulo}>Smart Van</Text>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  content: {
    padding: 20,
  },
  header: {
    marginBottom: 24,
  },
  saudacao: {
    fontSize: 14,
    color: '#666',
  },
  titulo: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#111',
  },
  cardAtalho: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#222',
    padding: 20,
    borderRadius: 16,
    marginBottom: 28,
  },
  cardInfo: {
    flex: 1,
    marginRight: 12,
  },
  cardTitulo: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 4,
  },
  cardSubtitulo: {
    fontSize: 12,
    color: '#ccc',
    lineHeight: 16,
  },
  secaoAvisos: {
    gap: 8,
  },
  labelSecao: {
    fontSize: 11,
    fontWeight: 'bold',
    color: '#888',
    letterSpacing: 1,
  },
  cardAviso: {
    borderWidth: 1,
    borderColor: '#e0e0e0',
    borderRadius: 12,
    padding: 16,
    backgroundColor: '#fafafa',
  },
  headerAviso: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 6,
  },
  tituloAviso: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#111',
  },
  textoAviso: {
    fontSize: 12,
    color: '#666',
    lineHeight: 18,
  },
});