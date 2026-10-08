import React, { useState } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  TextInput, 
  FlatList, 
  TouchableOpacity,
} from 'react-native';
import { SelectCountry } from 'react-native-element-dropdown';
import {
  SafeAreaProvider,
  SafeAreaView,
  Edge,
} from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';


const DADOS_MOTORISTAS = [  //eventualmente vai ser uma query já filtrada
  { id: '1', nome: 'Carlos Mendes', veiculo: 'Renault Master', ano: '2021', vagasTotais: 10, vagas: 4, escolas: [1], validado: true, cidade: [2, 3], tipo: 'van', marca: 'renault', placa: 'ABC-1234' },
  { id: '2', nome: 'Rita Alencar', veiculo: 'Mercedes Sprinter', ano: '2019', vagasTotais: 10, vagas: 1, escolas: [2, 3, 4], validado: true, cidade: [1], tipo: 'van', marca: 'renault', placa: 'ABC-1234'  },
  { id: '3', nome: 'João Petrini', veiculo: 'Iveco Daily', ano: '2022', vagasTotais: 10, vagas: 6, escolas: [1, 2], validado: true, cidade: [1, 2], tipo: 'van', marca: 'renault', placa: 'ABC-1234'  },
];

const escola_data = [
  {id: 0, lable: 'Todas as escolas', bairro: ''},
  { id: 1, lable: 'EE Jardim das Flores', bairro: 'Centro' },
  { id: 2, lable: 'Colégio São Bento', bairro: 'Vila Nova' },
  { id: 3, lable: 'Escola 3', bairro: 'Jardim Amália' },
  { id: 4, lable: 'Escola 4', bairro: 'Bela Vista' },
  { id: 5, lable: 'Escola 5', bairro: 'Industrial' },
];

const cidade_data = [
    {
      id: 0,
      lable: 'Todas as cidades'
    },  
    {
      id: 1,
      lable: 'Cidade 1',
    },
    {
      id: 2,
      lable: 'Cidade 2',
    },
    {
      id: 3,
      lable: 'Cidade 3',
    },
    {
      id: 4,
      lable: 'Cidade 4',
    },
    {
      id: 5,
      lable: 'Cidade 5',
    },
];

interface Motorista {
  id: string;
  nome: string;
  veiculo: string;
  ano: string;
  vagasTotais: number;
  vagas: number;
  escolas: number[];
  validado: boolean;
  cidade: number[];
  tipo: string;
  marca: string;
  placa: string;
}

export default function BuscaScreen() {
  const router = useRouter();
  // Estado para capturar o que o usuário digita na busca
  const [termoBusca, setTermoBusca] = useState('');
  const [escolaFiltro, setEscolaFiltro] = useState(0);
  const [cidadeFiltro, setCidadeFiltro] = useState(0);
  const [vagasFiltro, setVagasFiltro] = useState(false);

  // 2. Componente que renderiza CADA item da lista (O Card do Motorista)
  const renderMotorista = ({ item }: { item: Motorista }) => (
    <TouchableOpacity 
      onPress={() => router.push({
        pathname: '/perfil' as const,
        params: { id: item.id, nome: item.nome, veiculo: item.veiculo, ano: item.ano, vagasTotais: item.vagasTotais, vagas: item.vagas, validado: String(item.validado), escolas: JSON.stringify(item.escolas), cidades: JSON.stringify(item.cidade), tipo: item.tipo, marca: item.marca, placa: item.placa }
      })}
    >

      <View style={styles.card}>
        {/* Círculo simulando a foto/avatar */}
        <View style={styles.avatar} />

        <View style={styles.cardContent}>
          <Text style={styles.nomeMotorista}>{item.nome}</Text>
          <Text style={styles.textoVeiculo}>{item.veiculo} - {item.ano}</Text>
          
          {/* Linha das tags de vagas e escolas */}
          <View style={styles.tagsContainer}>
            <Text style={styles.tagBadge}>{item.vagas} VAGAS</Text>
            <Text style={styles.tagBadge}>{item.escolas.length} ESCOLAS</Text>
          </View>
        </View>

        {/* Selo posicionado no canto */}
        {item.validado && (
          <View style={styles.seloValidado}>
            <Text style={styles.textoValidado}>● VALIDADO</Text>
          </View>
        )}
      </View>
    </TouchableOpacity>
  );
  if(termoBusca == '' && escolaFiltro == 0 && cidadeFiltro == 0){
    return (
      // SafeAreaView garante que o topo não fique escondido sob a barra de status do celular
      <SafeAreaView style={styles.container}>
        
        {/* 3. Cabeçalho Fixo (Busca e Filtros) */}
        <View style={styles.headerArea}>
          <TextInput
            style={styles.inputBusca}
            placeholder="Buscar motorista"
            value={termoBusca}
            onChangeText={setTermoBusca}
          />

          {/* Linha de Filtros (Simulada com Scroll horizontal) */}
          <View style={styles.filtrosContainer}>
            <SelectCountry
              style={styles.dropdown}
              selectedTextStyle={styles.selectedTextStyle}
              placeholderStyle={styles.placeholderStyle}
              imageStyle={styles.imageStyle}
              iconStyle={styles.iconStyle}
              maxHeight={200}
              value={escolaFiltro}
              data={escola_data}
              valueField="id"
              labelField="lable"
              imageField="image"
              placeholder="Escolas"
              searchPlaceholder="Search..."
              onChange={e => {
                setEscolaFiltro(e.value);
              }}
            />
            <SelectCountry
              style={styles.dropdown}
              selectedTextStyle={styles.selectedTextStyle}
              placeholderStyle={styles.placeholderStyle}
              imageStyle={styles.imageStyle}
              iconStyle={styles.iconStyle}
              maxHeight={200}
              value={cidadeFiltro}
              data={cidade_data}
              valueField="id"
              labelField="lable"
              imageField="image"
              placeholder="Cidades"
              searchPlaceholder="Search..."
              onChange={e => {
                setCidadeFiltro(e.value);
              }}
            />
            <TouchableOpacity style={styles.dropdown} onPress={() => setVagasFiltro(!vagasFiltro)}>
              <Text style={styles.placeholderStyle}>Vagas</Text>
            </TouchableOpacity>
          </View>

          {/* Barra de Contagem e Ordenação */}
          <View style={styles.infoBar}>
            <Text style={styles.textoInfo}>Digite algo ou escolha os filtros para começar</Text>
          </View>
        </View>
      </SafeAreaView>
    );
  }
  else{
    return (
      // SafeAreaView garante que o topo não fique escondido sob a barra de status do celular
      <SafeAreaView style={styles.container}>
        
        {/* 3. Cabeçalho Fixo (Busca e Filtros) */}
        <View style={styles.headerArea}>
          <TextInput
            style={styles.inputBusca}
            placeholder="Buscar motorista"
            value={termoBusca}
            onChangeText={setTermoBusca}
          />

          {/* Linha de Filtros (Simulada com Scroll horizontal) */}
          <View style={styles.filtrosContainer}>
            <SelectCountry
              style={styles.dropdown}
              selectedTextStyle={styles.selectedTextStyle}
              placeholderStyle={styles.placeholderStyle}
              imageStyle={styles.imageStyle}
              iconStyle={styles.iconStyle}
              maxHeight={200}
              value={escolaFiltro}
              data={escola_data}
              valueField="value"
              labelField="lable"
              imageField="image"
              placeholder="Escolas"
              searchPlaceholder="Search..."
              onChange={e => {
                setEscolaFiltro(e.value);
              }}
            />
            <SelectCountry
              style={styles.dropdown}
              selectedTextStyle={styles.selectedTextStyle}
              placeholderStyle={styles.placeholderStyle}
              imageStyle={styles.imageStyle}
              iconStyle={styles.iconStyle}
              maxHeight={200}
              value={cidadeFiltro}
              data={cidade_data}
              valueField="value"
              labelField="lable"
              imageField="image"
              placeholder="Cidades"
              searchPlaceholder="Search..."
              onChange={e => {
                setCidadeFiltro(e.value);
              }}
            />
            <TouchableOpacity style={styles.dropdown} onPress={() => setVagasFiltro(!vagasFiltro)}>
              <Text style={styles.placeholderStyle}>Vagas</Text>
            </TouchableOpacity>
          </View>

          {/* Barra de Contagem e Ordenação */}
          <View style={styles.infoBar}>
            <Text style={styles.textoInfo}>{DADOS_MOTORISTAS.length} MOTORISTAS VALIDADOS</Text>
            <Text style={styles.textoOrdenar}>ORDENAR</Text>
          </View>
        </View>

        {/* 4. Lista Dinâmica */}
        <FlatList
          data={DADOS_MOTORISTAS}
          keyExtractor={(item) => item.id}
          renderItem={renderMotorista}
          contentContainerStyle={styles.listaContainer}
          // Rodapé da lista com a caixa tracejada
        />

      </SafeAreaView>
    );
  }
}

// 5. Estilização Flexbox
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  headerArea: {
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  inputBusca: {
    height: 48,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    paddingHorizontal: 16,
    marginBottom: 16,
  },
  filtrosContainer: {
    flexDirection: 'row',
    marginBottom: 16,
    marginHorizontal: 10
  },
  filtroBtn: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderWidth: 1,
    borderRadius: 20,
  },
  filtroAtivo: {
    backgroundColor: '#333',
    borderColor: '#333',
  },
  filtroInativo:{
    backgroundColor: '#ccc',
    borderColor: '#ccc',
  },
  textoFiltro: {
    color: '#333',
  },
  textoFiltroAtivo: {
    color: '#fff',
  },
  infoBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 8,
  },
  textoInfo: {
    fontSize: 12,
    color: '#888',
    letterSpacing: 1,
  },
  textoOrdenar: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#555',
  },
  listaContainer: {
    padding: 16,
    paddingBottom: 40,
  },
  card: {
    flexDirection: 'row',
    borderWidth: 1,
    borderColor: '#e0e0e0',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    alignItems: 'center', // Centraliza o avatar e o texto no eixo Y
  },
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#f0f0f0',
    marginRight: 16,
  },
  cardContent: {
    flex: 1, // Faz a área de texto ocupar o resto do espaço
  },
  nomeMotorista: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  textoVeiculo: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  tagsContainer: {
    flexDirection: 'row',
    gap: 8,
  },
  tagBadge: {
    fontSize: 10,
    borderWidth: 1,
    borderColor: '#ccc',
    paddingVertical: 2,
    paddingHorizontal: 6,
    borderRadius: 4,
    color: '#555',
  },
  seloValidado: {
    position: 'absolute', // Gruda no topo direito do card
    top: 16,
    right: 16,
    borderWidth: 1,
    borderColor: '#333',
    paddingVertical: 2,
    paddingHorizontal: 6,
    borderRadius: 4,
  },
  textoValidado: {
    fontSize: 10,
    fontWeight: 'bold',
    color: '#333',
  },
  rodapeAviso: {
    borderWidth: 1,
    borderStyle: 'dashed', // Cria a borda tracejada
    borderColor: '#ccc',
    borderRadius: 8,
    padding: 16,
    marginTop: 8,
  },
  textoAviso: {
    fontSize: 12,
    color: '#888',
  },
  dropdown: {
      marginHorizontal: 5,
      height: 50,
      width: '33%',
      backgroundColor: '#EEEEEE',
      borderRadius: 22,
      padding: 8,
    },
    imageStyle: {
      width: 24,
      height: 24,
      borderRadius: 12,
    },
    placeholderStyle: {
      fontSize: 16,
    },
    selectedTextStyle: {
      fontSize: 16,
      marginLeft: 8,
    },
    iconStyle: {
      width: 20,
      height: 20,
    }
});