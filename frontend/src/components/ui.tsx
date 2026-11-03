// Peças visuais comuns, no mesmo estilo das telas originais (fundo branco, bordas cinza, botão escuro).
import React from 'react';
import {
  ActivityIndicator,
  Alert,
  Platform,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  TouchableOpacity,
  View,
  ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';

export const cores = {
  texto: '#111',
  textoMedio: '#555',
  textoClaro: '#888',
  borda: '#e0e0e0',
  bordaForte: '#333',
  fundo: '#fff',
  fundoSuave: '#f9f9f9',
  botao: '#222',
  erro: '#b00020',
};

// Pergunta antes de uma ação; no navegador o Alert do React Native não tem botões, então usa o confirm.
export function confirmar(titulo: string, texto: string, aoConfirmar: () => void) {
  if (Platform.OS === 'web') {
    if (globalThis.confirm?.(`${titulo}\n\n${texto}`)) aoConfirmar();
    return;
  }
  Alert.alert(titulo, texto, [
    { text: 'Voltar', style: 'cancel' },
    { text: 'Confirmar', onPress: aoConfirmar },
  ]);
}

export function Tela({
  titulo,
  voltar,
  acaoDireita,
  rodape,
  children,
  carregando,
  aoAtualizar,
}: {
  titulo: string;
  voltar?: boolean;
  acaoDireita?: React.ReactNode;
  rodape?: React.ReactNode;
  children: React.ReactNode;
  carregando?: boolean;
  aoAtualizar?: () => void;
}) {
  const router = useRouter();
  return (
    <SafeAreaView style={s.container} edges={['top', 'left', 'right']}>
      <View style={s.header}>
        {voltar && (
          <TouchableOpacity style={s.btnVoltar} onPress={() => router.back()} accessibilityLabel="Voltar">
            <Text style={s.textoVoltar}>‹</Text>
          </TouchableOpacity>
        )}
        <Text style={s.tituloHeader} numberOfLines={1}>
          {titulo}
        </Text>
        {acaoDireita}
      </View>
      <ScrollView
        contentContainerStyle={s.conteudo}
        keyboardShouldPersistTaps="handled"
        refreshControl={aoAtualizar ? <RefreshControl refreshing={!!carregando} onRefresh={aoAtualizar} /> : undefined}>
        {children}
      </ScrollView>
      {rodape && <View style={s.rodape}>{rodape}</View>}
    </SafeAreaView>
  );
}

export function Secao({ titulo, children }: { titulo: string; children?: React.ReactNode }) {
  return (
    <View style={{ marginBottom: 24 }}>
      <Text style={s.tituloSecao}>{titulo.toUpperCase()}</Text>
      {children}
    </View>
  );
}

export function Cartao({
  children,
  destaque,
  aoTocar,
  estilo,
}: {
  children: React.ReactNode;
  destaque?: boolean;
  aoTocar?: () => void;
  estilo?: ViewStyle;
}) {
  const conteudo = <View style={[s.cartao, destaque && s.cartaoDestaque, estilo]}>{children}</View>;
  return aoTocar ? (
    <TouchableOpacity onPress={aoTocar} activeOpacity={0.7}>
      {conteudo}
    </TouchableOpacity>
  ) : (
    conteudo
  );
}

export function Linha({ rotulo, valor }: { rotulo: string; valor?: React.ReactNode }) {
  return (
    <View style={s.linha}>
      <Text style={s.rotulo}>{rotulo}</Text>
      <Text style={s.valor}>{valor ?? '—'}</Text>
    </View>
  );
}

export function Selo({ texto, escuro }: { texto: string; escuro?: boolean }) {
  return (
    <View style={[s.selo, escuro && s.seloEscuro]}>
      <Text style={[s.textoSelo, escuro && { color: '#fff' }]}>{texto.toUpperCase()}</Text>
    </View>
  );
}

export function Botao({
  titulo,
  aoTocar,
  secundario,
  perigo,
  desativado,
  carregando,
}: {
  titulo: string;
  aoTocar: () => void;
  secundario?: boolean;
  perigo?: boolean;
  desativado?: boolean;
  carregando?: boolean;
}) {
  const inativo = desativado || carregando;
  return (
    <TouchableOpacity
      style={[s.botao, secundario && s.botaoSecundario, perigo && s.botaoPerigo, inativo && { opacity: 0.5 }]}
      onPress={aoTocar}
      disabled={inativo}>
      {carregando ? (
        <ActivityIndicator color={secundario || perigo ? cores.texto : '#fff'} />
      ) : (
        <Text style={[s.textoBotao, (secundario || perigo) && { color: perigo ? cores.erro : cores.texto }]}>
          {titulo}
        </Text>
      )}
    </TouchableOpacity>
  );
}

export function Campo({ rotulo, ...props }: TextInputProps & { rotulo: string }) {
  return (
    <View style={{ marginBottom: 12 }}>
      <Text style={s.rotuloCampo}>{rotulo}</Text>
      <TextInput style={s.input} placeholderTextColor="#aaa" {...props} />
    </View>
  );
}

// Opções curtas lado a lado (papel, tipo de rota, parentesco...).
export function Opcoes<T extends string>({
  rotulo,
  valor,
  opcoes,
  aoMudar,
}: {
  rotulo?: string;
  valor: T | null;
  opcoes: { valor: T; texto: string }[];
  aoMudar: (v: T) => void;
}) {
  return (
    <View style={{ marginBottom: 12 }}>
      {rotulo && <Text style={s.rotuloCampo}>{rotulo}</Text>}
      <View style={s.opcoes}>
        {opcoes.map((o) => (
          <TouchableOpacity
            key={o.valor}
            style={[s.opcao, valor === o.valor && s.opcaoAtiva]}
            onPress={() => aoMudar(o.valor)}>
            <Text style={[s.textoOpcao, valor === o.valor && { color: '#fff' }]}>{o.texto}</Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );
}

export function Aviso({ texto, erro }: { texto: string; erro?: boolean }) {
  return (
    <View style={[s.aviso, erro && { borderColor: cores.erro, borderStyle: 'solid' }]}>
      <Text style={[s.textoAviso, erro && { color: cores.erro }]}>{texto}</Text>
    </View>
  );
}

export function Carregando() {
  return (
    <View style={{ paddingVertical: 32 }}>
      <ActivityIndicator color={cores.texto} />
    </View>
  );
}

export function Divisor() {
  return <View style={s.divisor} />;
}

export const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: cores.fundo },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
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
  textoVoltar: { fontSize: 20, fontWeight: 'bold', color: '#333', marginTop: -2 },
  tituloHeader: { flex: 1, fontSize: 20, fontWeight: 'bold', color: cores.texto },
  conteudo: { padding: 16, paddingBottom: 40 },
  rodape: { padding: 16, borderTopWidth: 1, borderTopColor: '#eee', backgroundColor: cores.fundo, gap: 8 },
  tituloSecao: { fontSize: 11, fontWeight: 'bold', color: cores.textoClaro, letterSpacing: 1, marginBottom: 8 },
  cartao: {
    borderWidth: 1,
    borderColor: cores.borda,
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    backgroundColor: cores.fundo,
  },
  cartaoDestaque: { borderColor: cores.bordaForte, backgroundColor: cores.fundoSuave },
  linha: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 4, gap: 12 },
  rotulo: { fontSize: 13, color: cores.textoClaro },
  valor: { fontSize: 14, fontWeight: '600', color: cores.texto, flexShrink: 1, textAlign: 'right' },
  selo: {
    alignSelf: 'flex-start',
    borderWidth: 1,
    borderColor: cores.bordaForte,
    paddingVertical: 2,
    paddingHorizontal: 6,
    borderRadius: 4,
  },
  seloEscuro: { backgroundColor: cores.botao, borderColor: cores.botao },
  textoSelo: { fontSize: 10, fontWeight: 'bold', color: '#333', letterSpacing: 0.5 },
  botao: {
    backgroundColor: cores.botao,
    height: 48,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 16,
  },
  botaoSecundario: { backgroundColor: cores.fundo, borderWidth: 1, borderColor: '#ccc' },
  botaoPerigo: { backgroundColor: cores.fundo, borderWidth: 1, borderColor: cores.erro },
  textoBotao: { color: '#fff', fontSize: 15, fontWeight: 'bold', letterSpacing: 0.5 },
  rotuloCampo: { fontSize: 12, color: cores.textoClaro, marginBottom: 4 },
  input: {
    height: 48,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    paddingHorizontal: 16,
    fontSize: 15,
    color: cores.texto,
  },
  opcoes: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  opcao: { paddingVertical: 8, paddingHorizontal: 14, borderRadius: 20, backgroundColor: '#EEEEEE' },
  opcaoAtiva: { backgroundColor: cores.botao },
  textoOpcao: { fontSize: 14, color: '#333' },
  aviso: {
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: '#ccc',
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
  },
  textoAviso: { fontSize: 13, color: cores.textoClaro, lineHeight: 18 },
  divisor: { height: 1, backgroundColor: '#eee', marginVertical: 12 },
  titulo: { fontSize: 16, fontWeight: 'bold', color: '#333', marginBottom: 4 },
  subtitulo: { fontSize: 14, color: '#666' },
  pequeno: { fontSize: 12, color: cores.textoClaro },
  avatar: { width: 50, height: 50, borderRadius: 25, backgroundColor: '#f0f0f0', marginRight: 16 },
});
