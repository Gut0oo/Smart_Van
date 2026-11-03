import React, { useState } from 'react';
import { useRouter } from 'expo-router';

import { Aviso, Botao, Campo, Opcoes, Tela } from '@/components/ui';
import { api, TipoUsuario } from '@/services/api';
import { inicioDoPapel, useAuth } from '@/services/auth';
import { mensagemDeErro } from '@/services/useCarregar';

// Usa o cadastro original do projeto (POST /usuarios) e já entra na conta criada.
export default function CadastroScreen() {
  const router = useRouter();
  const { entrar } = useAuth();
  const [tipo, setTipo] = useState<TipoUsuario>('RESPONSAVEL');
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [cpf, setCpf] = useState('');
  const [telefone, setTelefone] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function aoCadastrar() {
    setErro(null);
    setEnviando(true);
    try {
      await api.post('/usuarios', {
        nome: nome.trim(),
        email: email.trim(),
        senha,
        cpf: cpf.replace(/\D/g, ''),
        telefone: telefone.replace(/\D/g, ''),
        tipo,
      });
      const u = await entrar(email.trim(), senha);
      router.replace(inicioDoPapel(u.tipo));
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  const completo = nome && email && senha.length >= 6 && cpf && telefone;

  return (
    <Tela
      titulo="Criar conta"
      voltar
      rodape={<Botao titulo="Cadastrar" aoTocar={aoCadastrar} carregando={enviando} desativado={!completo} />}>
      <Opcoes
        rotulo="Eu sou"
        valor={tipo}
        aoMudar={setTipo}
        opcoes={[
          { valor: 'RESPONSAVEL', texto: 'Responsável' },
          { valor: 'MOTORISTA', texto: 'Motorista' },
          { valor: 'ADMINISTRADOR', texto: 'Administrador' },
        ]}
      />
      <Campo rotulo="Nome completo" value={nome} onChangeText={setNome} />
      <Campo rotulo="E-mail" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
      <Campo rotulo="Senha (mínimo 6 caracteres)" value={senha} onChangeText={setSenha} secureTextEntry />
      <Campo rotulo="CPF (só números)" value={cpf} onChangeText={setCpf} keyboardType="number-pad" maxLength={14} />
      <Campo rotulo="Telefone com DDD" value={telefone} onChangeText={setTelefone} keyboardType="phone-pad" />
      {tipo === 'MOTORISTA' && (
        <Aviso texto="Motoristas só aparecem na busca depois de aprovados por um administrador." />
      )}
      {erro && <Aviso texto={erro} erro />}
    </Tela>
  );
}
