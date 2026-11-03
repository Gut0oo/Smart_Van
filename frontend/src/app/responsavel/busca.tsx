import React, { useState } from 'react';
import { StyleSheet, Text, TextInput, View } from 'react-native';
import { useRouter } from 'expo-router';

import { SeletorEscola } from '@/components/formularios';
import { Aviso, Botao, Campo, Cartao, Selo, Tela, cores, s } from '@/components/ui';
import { api } from '@/services/api';
import { hora, horaValida } from '@/services/formato';
import { MotoristaBusca } from '@/services/tipos';
import { mensagemDeErro } from '@/services/useCarregar';

// Busca por filtros (escola obrigatória) ou só pelo nome do motorista.
export default function BuscaScreen() {
  const router = useRouter();
  const [nome, setNome] = useState('');
  const [escolaId, setEscolaId] = useState<number | null>(null);
  const [horario, setHorario] = useState('');
  const [cidade, setCidade] = useState('');
  const [resultado, setResultado] = useState<MotoristaBusca[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [buscando, setBuscando] = useState(false);

  const horarioInvalido = horario.trim() !== '' && !horaValida(horario);
  const podeBuscar = (nome.trim() !== '' || escolaId !== null) && !horarioInvalido;

  async function buscar() {
    setErro(null);
    setBuscando(true);
    try {
      const p = new URLSearchParams();
      if (escolaId) p.set('escolaId', String(escolaId));
      if (nome.trim()) p.set('nome', nome.trim());
      if (horario.trim()) p.set('horarioAula', horario.trim());
      if (cidade.trim()) p.set('cidade', cidade.trim());
      setResultado(await api.get<MotoristaBusca[]>(`/motoristas?${p.toString()}`));
    } catch (e) {
      setErro(mensagemDeErro(e));
      setResultado(null);
    } finally {
      setBuscando(false);
    }
  }

  return (
    <Tela titulo="Buscar motorista">
      <TextInput
        style={styles.inputBusca}
        placeholder="Buscar motorista pelo nome"
        placeholderTextColor="#aaa"
        value={nome}
        onChangeText={setNome}
        returnKeyType="search"
        onSubmitEditing={() => podeBuscar && buscar()}
      />
      <SeletorEscola valor={escolaId} aoMudar={setEscolaId} rotulo="Escola (obrigatória sem o nome)" />
      <View style={{ flexDirection: 'row', gap: 8 }}>
        <View style={{ flex: 1 }}>
          <Campo rotulo="Horário da aula" placeholder="07:30" value={horario} onChangeText={setHorario} maxLength={5} />
        </View>
        <View style={{ flex: 2 }}>
          <Campo rotulo="Cidade" value={cidade} onChangeText={setCidade} />
        </View>
      </View>
      {horarioInvalido && <Aviso texto="Horário no formato HH:MM, por exemplo 07:30." erro />}
      <Botao titulo="Buscar" aoTocar={buscar} carregando={buscando} desativado={!podeBuscar} />

      <View style={{ height: 20 }} />
      {erro && <Aviso texto={erro} erro />}
      {!resultado && !erro && <Aviso texto="Digite o nome do motorista ou escolha a escola para começar." />}
      {resultado && (
        <Text style={styles.contagem}>
          {resultado.length} {resultado.length === 1 ? 'MOTORISTA ENCONTRADO' : 'MOTORISTAS ENCONTRADOS'}
        </Text>
      )}
      {resultado?.length === 0 && <Aviso texto="Nenhum motorista com vaga para esses filtros." />}
      {resultado?.map((m) => {
        const vagas = m.atendimentos.reduce((t, a) => t + a.vagasRestantes, 0);
        return (
          <Cartao key={m.id} aoTocar={() => router.push({ pathname: '/perfil-motorista/[id]', params: { id: m.id } })}>
            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
              <View style={s.avatar} />
              <View style={{ flex: 1 }}>
                <Text style={s.titulo}>{m.nome}</Text>
                <Text style={s.subtitulo}>
                  {m.veiculo ? `${m.veiculo.modelo} · ${m.veiculo.capacidade} lugares` : 'Sem veículo cadastrado'}
                </Text>
              </View>
              <Selo texto="Validado" />
            </View>
            <View style={styles.tags}>
              <Text style={styles.tag}>{vagas > 0 ? `${vagas} VAGAS` : 'SEM VAGAS'}</Text>
              {m.atendimentos.map((a) => (
                <Text key={a.id} style={styles.tag}>
                  {a.escola.nome.toUpperCase()} · AULA {hora(a.horarioInicioAula)}
                </Text>
              ))}
            </View>
          </Cartao>
        );
      })}
    </Tela>
  );
}

const styles = StyleSheet.create({
  inputBusca: {
    height: 48,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    paddingHorizontal: 16,
    marginBottom: 16,
    fontSize: 15,
    color: cores.texto,
  },
  contagem: { fontSize: 12, color: cores.textoClaro, letterSpacing: 1, marginBottom: 12 },
  tags: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
  tag: {
    fontSize: 10,
    borderWidth: 1,
    borderColor: '#ccc',
    paddingVertical: 2,
    paddingHorizontal: 6,
    borderRadius: 4,
    color: '#555',
  },
});
