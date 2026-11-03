import React, { useState } from 'react';
import { Text } from 'react-native';
import { useRouter } from 'expo-router';

import { Aviso, Botao, Campo, Carregando, Cartao, Opcoes, Secao, Selo, Tela, s } from '@/components/ui';
import { api } from '@/services/api';
import { data, hojeIso, hora, horaValida } from '@/services/formato';
import { Atendimento, Rota } from '@/services/tipos';
import { mensagemDeErro, useCarregar } from '@/services/useCarregar';

type Dia = 'hoje' | 'amanha';

// Gera a rota de um horário (atendimento) e lista as rotas do dia escolhido.
export default function RotaScreen() {
  const router = useRouter();
  const [dia, setDia] = useState<Dia>('amanha');
  const dataIso = hojeIso(dia === 'hoje' ? 0 : 1);
  const { dados, carregando, erro, recarregar } = useCarregar(async () => {
    const [atendimentos, rotas] = await Promise.all([
      api.get<Atendimento[]>('/motorista/escolas'),
      api.get<Rota[]>(`/motorista/rotas?data=${dataIso}`),
    ]);
    return { atendimentos, rotas };
  }, [dataIso]);

  const [atendimentoId, setAtendimentoId] = useState<string | null>(null);
  const [tipo, setTipo] = useState<'IDA' | 'VOLTA'>('IDA');
  const [saida, setSaida] = useState('');
  const [erroGerar, setErroGerar] = useState<string | null>(null);
  const [gerando, setGerando] = useState(false);

  const atendimento = dados?.atendimentos.find((a) => String(a.id) === atendimentoId);
  const saidaPadrao = atendimento ? (tipo === 'IDA' ? atendimento.horarioSaidaMotorista : atendimento.horarioSaidaEscola) : null;
  const saidaInvalida = saida.trim() !== '' && !horaValida(saida);

  async function gerar() {
    setErroGerar(null);
    setGerando(true);
    try {
      const r = await api.post<Rota>('/motorista/rotas/gerar', {
        data: dataIso,
        tipo,
        atendimentoId: Number(atendimentoId),
        horarioSaida: saida.trim() || undefined,
      });
      setSaida('');
      router.push({ pathname: '/rota/[id]', params: { id: r.id } });
    } catch (e) {
      setErroGerar(mensagemDeErro(e));
    } finally {
      setGerando(false);
    }
  }

  return (
    <Tela titulo="Rota do dia" carregando={carregando} aoAtualizar={recarregar}>
      <Opcoes
        valor={dia}
        aoMudar={setDia}
        opcoes={[
          { valor: 'hoje', texto: `Hoje (${data(hojeIso(0))})` },
          { valor: 'amanha', texto: `Amanhã (${data(hojeIso(1))})` },
        ]}
      />
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {dados && (
        <>
          <Secao titulo="Gerar rota">
            {dados.atendimentos.length === 0 ? (
              <Aviso texto="Cadastre seus horários de escola em Conta para gerar rotas." />
            ) : (
              <>
                <Opcoes
                  rotulo="Horário"
                  valor={atendimentoId}
                  aoMudar={setAtendimentoId}
                  opcoes={dados.atendimentos.map((a) => ({
                    valor: String(a.id),
                    texto: `${a.escola.nome} · aula ${hora(a.horarioInicioAula)}`,
                  }))}
                />
                <Opcoes
                  rotulo="Trajeto"
                  valor={tipo}
                  aoMudar={setTipo}
                  opcoes={[
                    { valor: 'IDA', texto: 'Ida (casa → escola)' },
                    { valor: 'VOLTA', texto: 'Volta (escola → casa)' },
                  ]}
                />
                <Campo
                  rotulo={`Horário de saída (vazio = ${saidaPadrao ? hora(saidaPadrao) : 'padrão do horário'})`}
                  placeholder={saidaPadrao ? hora(saidaPadrao) : 'HH:MM'}
                  value={saida}
                  onChangeText={setSaida}
                  maxLength={5}
                />
                {saidaInvalida && <Aviso texto="Horário no formato HH:MM." erro />}
                {erroGerar && <Aviso texto={erroGerar} erro />}
                <Botao titulo="Gerar rota" aoTocar={gerar} carregando={gerando} desativado={!atendimentoId || saidaInvalida} />
              </>
            )}
          </Secao>

          <Secao titulo={`Rotas de ${data(dataIso)}`}>
            {dados.rotas.length === 0 && <Aviso texto="Nenhuma rota gerada para este dia." />}
            {dados.rotas.map((r) => (
              <Cartao key={r.id} destaque={r.foraDoTeto} aoTocar={() => router.push({ pathname: '/rota/[id]', params: { id: r.id } })}>
                <Text style={s.titulo}>
                  {r.tipo === 'IDA' ? 'Ida' : 'Volta'} · {r.escola.nome}
                </Text>
                <Text style={s.subtitulo}>
                  {hora(r.horarioInicio)} → {hora(r.horarioFim)} · {r.paradas.filter((p) => p.aluno).length} alunos
                </Text>
                {r.foraDoTeto && <Selo texto={`Adiantar ${r.minutosAdiantar} min`} escuro />}
              </Cartao>
            ))}
          </Secao>
        </>
      )}
    </Tela>
  );
}
