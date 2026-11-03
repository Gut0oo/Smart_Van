import React, { useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';

import { Aviso, Botao, Campo, Carregando, Cartao, Opcoes, Secao, Selo, Tela, cores, s } from '@/components/ui';
import { api } from '@/services/api';
import { dinheiro, hora } from '@/services/formato';
import { Aluno, Contrato, MotoristaBusca } from '@/services/tipos';
import { mensagemDeErro, useCarregar } from '@/services/useCarregar';

// Perfil do motorista para o responsável e envio da proposta de contrato.
export default function PerfilMotorista() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { dados, carregando, erro } = useCarregar(async () => {
    const [motorista, alunos] = await Promise.all([
      api.get<MotoristaBusca>(`/motoristas/${id}`),
      api.get<Aluno[]>('/responsavel/alunos'),
    ]);
    return { motorista, alunos };
  }, [id]);

  const [atendimentoId, setAtendimentoId] = useState<number | null>(null);
  const [alunoId, setAlunoId] = useState<string | null>(null);
  const [valor, setValor] = useState('');
  const [erroEnvio, setErroEnvio] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const m = dados?.motorista;
  const atendimento = m?.atendimentos.find((a) => a.id === atendimentoId);
  const aluno = dados?.alunos.find((a) => String(a.id) === alunoId);
  const valorNumero = Number(valor.replace(',', '.'));
  const piso = atendimento?.valorMensal != null ? Number(atendimento.valorMensal) : null;

  // Ao escolher o horário, o valor já vem com o preço do motorista; o responsável pode mudar para negociar.
  function escolherHorario(id: number) {
    setAtendimentoId(id);
    const a = m?.atendimentos.find((x) => x.id === id);
    if (a?.valorMensal != null) setValor(Number(a.valorMensal).toFixed(2).replace('.', ','));
  }
  const outraEscola = atendimento && aluno && atendimento.escola.id !== aluno.escola.id;

  async function enviar() {
    setErroEnvio(null);
    setEnviando(true);
    try {
      const c = await api.post<Contrato>('/contratos', {
        atendimentoId,
        alunoId: Number(alunoId),
        valorMensal: valorNumero,
      });
      router.replace({ pathname: '/contrato/[id]', params: { id: c.id } });
    } catch (e) {
      setErroEnvio(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  const podeEnviar = atendimento && atendimento.vagasRestantes > 0 && aluno && valorNumero > 0 && !outraEscola;

  return (
    <Tela
      titulo="Perfil do motorista"
      voltar
      rodape={m && <Botao titulo="Enviar proposta" aoTocar={enviar} carregando={enviando} desativado={!podeEnviar} />}>
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {m && dados && (
        <>
          <View style={styles.cabecalho}>
            <View style={styles.avatar} />
            <View style={{ flex: 1, gap: 6 }}>
              <Text style={styles.nome}>{m.nome}</Text>
              <Selo texto="● Documentação validada" />
            </View>
          </View>

          <View style={styles.cardNumeros}>
            <View>
              <Text style={styles.rotuloNumero}>CLIENTES ATÉ HOJE</Text>
              <Text style={styles.sub}>Famílias atendidas</Text>
            </View>
            <Text style={styles.numero}>{m.totalClientes ?? 0}</Text>
          </View>

          <Secao titulo="Veículo">
            <Cartao>
              {m.veiculo ? (
                <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                  <View>
                    <Text style={s.pequeno}>Modelo</Text>
                    <Text style={styles.valor}>{m.veiculo.modelo}</Text>
                  </View>
                  <View>
                    <Text style={s.pequeno}>Tipo</Text>
                    <Text style={styles.valor}>{m.veiculo.tipo}</Text>
                  </View>
                  <View>
                    <Text style={s.pequeno}>Capacidade</Text>
                    <Text style={styles.valor}>{m.veiculo.capacidade}</Text>
                  </View>
                </View>
              ) : (
                <Text style={s.subtitulo}>Sem veículo cadastrado</Text>
              )}
            </Cartao>
          </Secao>

          <Secao titulo="Escolha o horário">
            {m.atendimentos.length === 0 && <Aviso texto="Este motorista ainda não cadastrou escolas e horários." />}
            {m.atendimentos.map((a) => {
              const lotado = a.vagasRestantes <= 0;
              const ativo = a.id === atendimentoId;
              return (
                <TouchableOpacity key={a.id} disabled={lotado} onPress={() => escolherHorario(a.id)}>
                  <View style={[styles.atendimento, ativo && styles.atendimentoAtivo, lotado && { opacity: 0.5 }]}>
                    <View style={{ flex: 1 }}>
                      <Text style={[s.titulo, ativo && { color: '#fff' }]}>{a.escola.nome}</Text>
                      <Text style={[s.pequeno, ativo && { color: '#ddd' }]}>
                        {a.cidade} · aula às {hora(a.horarioInicioAula)} · sai às {hora(a.horarioSaidaMotorista)}
                      </Text>
                      <Text style={[s.pequeno, ativo && { color: '#ddd' }]}>
                        {a.valorMensal != null ? `Preço do motorista: ${dinheiro(a.valorMensal)}/mês` : 'Preço a combinar'}
                      </Text>
                      {a.capacidade != null && (
                        <Text style={[s.pequeno, ativo && { color: '#ddd' }]}>
                          {a.contratosAtivos} de {a.capacidade} lugares ocupados
                        </Text>
                      )}
                    </View>
                    <Text style={[styles.vagas, ativo && { color: '#fff' }]}>
                      {lotado ? 'Lotado' : `${a.vagasRestantes} vagas`}
                    </Text>
                  </View>
                </TouchableOpacity>
              );
            })}
          </Secao>

          <Secao titulo="Proposta">
            {dados.alunos.length === 0 ? (
              <Aviso texto="Cadastre um aluno na tela Início antes de enviar uma proposta." />
            ) : (
              <Opcoes
                rotulo="Para qual aluno"
                valor={alunoId}
                aoMudar={setAlunoId}
                opcoes={dados.alunos.map((a) => ({ valor: String(a.id), texto: a.nome }))}
              />
            )}
            {outraEscola && <Aviso texto="Esse aluno estuda em outra escola. Escolha um horário da escola dele." erro />}
            {piso != null && valorNumero > 0 && (
              <Aviso
                texto={
                  valorNumero === piso
                    ? `Proposta no preço do motorista (${dinheiro(piso)}).`
                    : valorNumero < piso
                      ? `Proposta abaixo do preço do motorista (${dinheiro(piso)}): ele vai avaliar se aceita.`
                      : `Proposta acima do preço do motorista (${dinheiro(piso)}).`
                }
              />
            )}
            <Campo
              rotulo={piso != null ? 'Valor mensal (mude para negociar)' : 'Valor mensal proposto (R$)'}
              placeholder="350,00"
              keyboardType="decimal-pad"
              value={valor}
              onChangeText={setValor}
            />
            {erroEnvio && <Aviso texto={erroEnvio} erro />}
          </Secao>
        </>
      )}
    </Tela>
  );
}

const styles = StyleSheet.create({
  cabecalho: { flexDirection: 'row', alignItems: 'center', gap: 16, marginBottom: 20 },
  avatar: { width: 70, height: 70, borderRadius: 35, backgroundColor: '#eee' },
  nome: { fontSize: 22, fontWeight: 'bold', color: cores.texto },
  cardNumeros: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: cores.bordaForte,
    borderRadius: 12,
    padding: 16,
    backgroundColor: cores.fundoSuave,
    marginBottom: 24,
  },
  rotuloNumero: { fontSize: 11, fontWeight: 'bold', color: '#666', letterSpacing: 1, marginBottom: 2 },
  sub: { fontSize: 12, color: cores.textoClaro },
  numero: { fontSize: 32, fontWeight: 'bold', color: cores.texto },
  valor: { fontSize: 16, fontWeight: 'bold', color: cores.texto },
  atendimento: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: cores.borda,
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    gap: 12,
  },
  atendimentoAtivo: { backgroundColor: cores.botao, borderColor: cores.botao },
  vagas: { fontSize: 13, fontWeight: 'bold', color: cores.texto },
});
