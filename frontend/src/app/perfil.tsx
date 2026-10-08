import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter, useLocalSearchParams } from 'expo-router';

interface Escola {
  id: number;
  lable: string;
  bairro?: string;
}

// Mock de todas as escolas
const escola_data: Escola[] = [
  { id: 1, lable: 'EE Jardim das Flores', bairro: 'Centro' },
  { id: 2, lable: 'Colégio São Bento', bairro: 'Vila Nova' },
  { id: 3, lable: 'Escola 3', bairro: 'Jardim Amália' },
  { id: 4, lable: 'Escola 4', bairro: 'Bela Vista' },
  { id: 5, lable: 'Escola 5', bairro: 'Industrial' },
];

export default function PerfilScreen() {
  const router = useRouter();
  const params = useLocalSearchParams();

  const nome = params.nome as string;
  const veiculo = params.veiculo as string;
  const tipo = params.tipo as string;
  const marca = params.marca as string;
  const placa = params.placa as string;
  const validado = params.validado === 'true';

  // Converte a string JSON de volta para array de números [1, 2]
  const idsEscolasMotorista: number[] = params.escolas ? JSON.parse(params.escolas as string) : [];
  
  const vagasTotais = Number(params.vagasTotais) || 0;
  const vagas = Number(params.vagas) || 0;
  const ano = Number(params.ano);

  // 1. FILTRAGEM: Pega apenas as escolas que têm o ID no array do motorista
  const escolasDoMotorista = escola_data.filter((escola) =>
    idsEscolasMotorista.includes(escola.id)
  );

  return (
    <SafeAreaView style={styles.container}>
      {/* 1. Header com botão de voltar */}
      <View style={styles.header}>
        <TouchableOpacity 
          style={styles.btnVoltar} 
          onPress={() => router.back()}
        >
          <Text style={styles.textoIconeVoltar}>‹</Text>
        </TouchableOpacity>
        <Text style={styles.tituloHeader}>Perfil do motorista</Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {/* 2. Cabeçalho do Motorista */}
        <View style={styles.motoristaHeader}>
          <View style={styles.avatar} />
          <View style={styles.motoristaInfo}>
            <Text style={styles.nomeMotorista}>{nome}</Text>
            {validado && (
              <View style={styles.seloValidado}>
                <Text style={styles.textoValidado}>● DOCUMENTAÇÃO VALIDADA</Text>
              </View>
            )}
          </View>
        </View>

        {/* 3. Card de Vagas */}
        <View style={styles.cardVagas}>
          <View>
            <Text style={styles.labelVagas}>VAGAS RESTANTES</Text>
            <Text style={styles.sublabelVagas}>
              Capacidade {vagasTotais} · {vagasTotais - vagas} ocupadas
            </Text>
          </View>
          <Text style={styles.numeroVagas}>{vagas}</Text>
        </View>

        {/* 4. Seção Veículo */}
        <Text style={styles.sectionTitle}>VEÍCULO</Text>
        <View style={styles.cardInfo}>
          <View style={styles.gridVeiculo}>
            <View style={styles.colunaVeiculo}>
              <Text style={styles.labelGrid}>Modelo</Text>
              <Text style={styles.valorGrid}>{veiculo}</Text>
            </View>
            <View style={styles.colunaVeiculo}>
              <Text style={styles.labelGrid}>Ano</Text>
              <Text style={styles.valorGrid}>{ano}</Text>
            </View>
            <View style={styles.colunaVeiculo}>
              <Text style={styles.labelGrid}>Capacidade</Text>
              <Text style={styles.valorGrid}>{vagasTotais}</Text>
            </View>
          </View>
          <View style={styles.divisor} />
          <Text style={styles.detalheVeiculo}>
            {tipo} · {marca} · placa {placa}
          </Text>
        </View>

        {/* 5. Seção Escolas Atendidas (Renderização mapeada e limpa) */}
        <Text style={styles.sectionTitle}>ESCOLAS ATENDIDAS</Text>
        <View style={styles.cardInfo}>
          {escolasDoMotorista.map((escola, index) => (
            <React.Fragment key={escola.id}>
              <View style={styles.itemEscola}>
                <View style={styles.iconeEscola} />
                <View>
                  <Text style={styles.nomeEscola}>{escola.lable}</Text>
                  <Text style={styles.bairroEscola}>{escola.bairro || 'Centro'}</Text>
                </View>
              </View>
              {/* Adiciona o divisor apenas entre os itens, não no último */}
              {index < escolasDoMotorista.length - 1 && <View style={styles.divisor} />}
            </React.Fragment>
          ))}
        </View>
      </ScrollView>

      {/* 6. Botão Fixo */}
      <View style={styles.footer}>
        <TouchableOpacity style={styles.btnProposta} onPress={() => router.back()}>
          <Text style={styles.textoBtnProposta}>Enviar proposta</Text>
        </TouchableOpacity>
      </View>
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
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
    gap: 12,
  },
  btnVoltar: {
    width: 32,
    height: 32,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  textoIconeVoltar: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginTop: -2,
  },
  tituloHeader: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#111',
  },
  content: {
    padding: 16,
    paddingBottom: 24,
  },
  motoristaHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 20,
    gap: 16,
  },
  avatar: {
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: '#eee',
  },
  motoristaInfo: {
    flex: 1,
    alignItems: 'flex-start',
    gap: 6,
  },
  nomeMotorista: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#111',
  },
  seloValidado: {
    borderWidth: 1,
    borderColor: '#333',
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: 6,
  },
  textoValidado: {
    fontSize: 10,
    fontWeight: 'bold',
    color: '#333',
    letterSpacing: 0.5,
  },
  cardVagas: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#333',
    borderRadius: 12,
    padding: 16,
    backgroundColor: '#f9f9f9',
    marginBottom: 24,
  },
  labelVagas: {
    fontSize: 11,
    fontWeight: 'bold',
    color: '#666',
    letterSpacing: 1,
    marginBottom: 2,
  },
  sublabelVagas: {
    fontSize: 12,
    color: '#888',
  },
  numeroVagas: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#111',
  },
  sectionTitle: {
    fontSize: 11,
    fontWeight: 'bold',
    color: '#888',
    letterSpacing: 1,
    marginBottom: 8,
  },
  cardInfo: {
    borderWidth: 1,
    borderColor: '#e0e0e0',
    borderRadius: 12,
    padding: 16,
    marginBottom: 24,
  },
  gridVeiculo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  colunaVeiculo: {
    flex: 1,
  },
  labelGrid: {
    fontSize: 12,
    color: '#888',
    marginBottom: 4,
  },
  valorGrid: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#111',
  },
  divisor: {
    height: 1,
    backgroundColor: '#eee',
    marginVertical: 12,
  },
  detalheVeiculo: {
    fontSize: 13,
    color: '#666',
  },
  itemEscola: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  iconeEscola: {
    width: 24,
    height: 24,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: '#ccc',
  },
  nomeEscola: {
    fontSize: 14,
    fontWeight: '600',
    color: '#222',
  },
  bairroEscola: {
    fontSize: 12,
    color: '#888',
  },
  footer: {
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: '#eee',
    backgroundColor: '#fff',
  },
  btnProposta: {
    backgroundColor: '#222',
    height: 48,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  textoBtnProposta: {
    color: '#fff',
    fontSize: 15,
    fontWeight: 'bold',
    letterSpacing: 0.5,
  },
});