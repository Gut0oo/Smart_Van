import React, { useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';

import { Aviso, Botao, Campo, cores } from '@/components/ui';
import { inicioDoPapel, useAuth } from '@/services/auth';
import { mensagemDeErro } from '@/services/useCarregar';

export default function LoginScreen() {
  const router = useRouter();
  const { entrar } = useAuth();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function aoEntrar() {
    setErro(null);
    setEnviando(true);
    try {
      const u = await entrar(email.trim(), senha);
      router.replace(inicioDoPapel(u.tipo));
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.conteudo}>
        <Text style={styles.saudacao}>Bem-vindo ao</Text>
        <Text style={styles.titulo}>Smart Van</Text>

        <Campo rotulo="E-mail" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
        <Campo
          rotulo="Senha"
          value={senha}
          onChangeText={setSenha}
          secureTextEntry
          onSubmitEditing={() => email && senha && aoEntrar()}
        />
        {erro && <Aviso texto={erro} erro />}
        <Botao titulo="Entrar" aoTocar={aoEntrar} carregando={enviando} desativado={!email || !senha} />

        <TouchableOpacity style={styles.link} onPress={() => router.push('/cadastro')}>
          <Text style={styles.textoLink}>Não tem conta? Cadastre-se</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: cores.fundo },
  conteudo: { flex: 1, padding: 20, justifyContent: 'center' },
  saudacao: { fontSize: 14, color: '#666' },
  titulo: { fontSize: 28, fontWeight: 'bold', color: cores.texto, marginBottom: 32 },
  link: { marginTop: 20, alignItems: 'center' },
  textoLink: { fontSize: 14, color: cores.textoMedio, textDecorationLine: 'underline' },
});
