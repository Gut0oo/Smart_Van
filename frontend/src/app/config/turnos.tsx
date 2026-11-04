import React, { useEffect, useState } from 'react';
import { Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

import { SeletorEscola } from '@/components/formularios';
import { Aviso, Botao, Campo, Cartao, Carregando, Secao, Tela, s, useVoltar } from '@/components/ui';
import { api } from '@/services/api';
import { dinheiro, hora, horaValida } from '@/services/formato';
import { Atendimento } from '@/services/tipos';
import { mensagemDeErro } from '@/services/useCarregar';

type Turno = { escolaId: number; escolaNome: string; inicio: string; saida: string; saidaEscola: string; piso: string };

// Edita a lista de escolas e horários; o PUT substitui a lista inteira.
export default function TurnosScreen() {
  const voltar = useVoltar();
  const [turnos, setTurnos] = useState<Turno[] | null>(null);
  const [escolaId, setEscolaId] = useState<number | null>(null);
  const [inicio, setInicio] = useState('');
  const [saida, setSaida] = useState('');
  const [saidaEscola, setSaidaEscola] = useState('');
  const [piso, setPiso] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    api
      .get<Atendimento[]>('/motorista/escolas')
      .then((lista) =>
        setTurnos(
          lista.map((a) => ({
            escolaId: a.escola.id,
            escolaNome: a.escola.nome,
            inicio: hora(a.horarioInicioAula),
            saida: hora(a.horarioSaidaMotorista),
            saidaEscola: a.horarioSaidaEscola ? hora(a.horarioSaidaEscola) : '',
            piso: a.valorMensal != null ? String(Number(a.valorMensal)) : '',
          })),
        ),
      )
      .catch((e) => setErro(mensagemDeErro(e)));
  }, []);

  const pisoNumero = (t: string) => Number(t.replace(',', '.'));
  const novoValido =
    escolaId && horaValida(inicio) && horaValida(saida) && horaValida(saidaEscola) && pisoNumero(piso) > 0;

  async function adicionar() {
    const escolas = await api.get<{ id: number; nome: string }[]>('/escolas');
    const nome = escolas.find((e) => e.id === escolaId)?.nome ?? '';
    setTurnos([...(turnos ?? []), { escolaId: escolaId!, escolaNome: nome, inicio, saida, saidaEscola, piso }]);
    setInicio('');
    setSaida('');
    setSaidaEscola('');
    setPiso('');
  }

  async function salvar() {
    setErro(null);
    setEnviando(true);
    try {
      await api.put('/motorista/escolas', {
        atendimentos: (turnos ?? []).map((t) => ({
          escolaId: t.escolaId,
          horarioInicioAula: t.inicio,
          horarioSaidaMotorista: t.saida,
          horarioSaidaEscola: t.saidaEscola,
          valorMensal: pisoNumero(t.piso),
        })),
      });
      voltar('/motorista/conta');
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  const semSaidaEscola = turnos?.some((t) => !t.saidaEscola);
  const semPiso = turnos?.some((t) => !(pisoNumero(t.piso) > 0));

  return (
    <Tela
      titulo="Escolas e horários"
      voltar
      rodape={<Botao titulo="Salvar alterações" aoTocar={salvar} carregando={enviando} desativado={!turnos || semSaidaEscola || semPiso} />}>
      {!turnos && !erro && <Carregando />}
      {turnos && (
        <>
          <Secao titulo="Seus horários">
            {turnos.length === 0 && <Aviso texto="Nenhum horário. Adicione abaixo." />}
            {turnos.map((t, i) => (
              <Cartao key={`${t.escolaId}-${t.inicio}-${i}`}>
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  <View style={{ flex: 1 }}>
                    <Text style={s.titulo}>{t.escolaNome}</Text>
                    <Text style={s.pequeno}>
                      Aula {t.inicio} · sai {t.saida} · volta {t.saidaEscola || '—'}
                    </Text>
                    <Text style={s.pequeno}>Preço: {t.piso ? `${dinheiro(pisoNumero(t.piso))}/mês` : 'não informado'}</Text>
                  </View>
                  <TouchableOpacity
                    onPress={() => setTurnos(turnos.filter((_, j) => j !== i))}
                    accessibilityLabel="Remover horário">
                    <Ionicons name="trash-outline" size={20} color="#555" />
                  </TouchableOpacity>
                </View>
              </Cartao>
            ))}
            {(semSaidaEscola || semPiso) && (
              <Aviso texto="Há horário antigo sem saída da escola ou sem preço: remova e adicione de novo." erro />
            )}
          </Secao>

          <Secao titulo="Adicionar horário">
            <SeletorEscola valor={escolaId} aoMudar={setEscolaId} />
            <Campo rotulo="Início da aula" placeholder="07:30" value={inicio} onChangeText={setInicio} maxLength={5} />
            <Campo rotulo="Sua saída na ida" placeholder="06:30" value={saida} onChangeText={setSaida} maxLength={5} />
            <Campo rotulo="Saída da escola (volta)" placeholder="12:30" value={saidaEscola} onChangeText={setSaidaEscola} maxLength={5} />
            <Campo
              rotulo="Preço mensal deste horário (R$)"
              placeholder="350,00"
              keyboardType="decimal-pad"
              value={piso}
              onChangeText={setPiso}
            />
            <Botao titulo="Adicionar à lista" secundario aoTocar={adicionar} desativado={!novoValido} />
          </Secao>
          <Aviso texto="Horário com proposta pendente ou contrato ativo não pode ser removido." />
        </>
      )}
      {erro && <Aviso texto={erro} erro />}
    </Tela>
  );
}
