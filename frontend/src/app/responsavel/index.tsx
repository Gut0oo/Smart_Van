import React from 'react';
import { Text, View } from 'react-native';
import { useRouter } from 'expo-router';

import { Aviso, Botao, Carregando, Cartao, Secao, Selo, Tela, s } from '@/components/ui';
import { api } from '@/services/api';
import { useAuth } from '@/services/auth';
import { competencia, data, dinheiro, STATUS_MENSALIDADE } from '@/services/formato';
import { Aluno, Mensalidade } from '@/services/tipos';
import { useCarregar } from '@/services/useCarregar';

export default function InicioResponsavel() {
  const router = useRouter();
  const { usuario } = useAuth();
  const { dados, carregando, erro, recarregar } = useCarregar(async () => {
    const [alunos, mensalidades] = await Promise.all([
      api.get<Aluno[]>('/responsavel/alunos'),
      api.get<Mensalidade[]>('/responsavel/mensalidades'),
    ]);
    return { alunos, abertas: mensalidades.filter((m) => m.status !== 'PAGA') };
  });

  return (
    <Tela titulo={`Olá, ${usuario?.nome.split(' ')[0] ?? ''}`} carregando={carregando} aoAtualizar={recarregar}>
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}

      {dados && (
        <>
          <Secao titulo="Meus alunos">
            {dados.alunos.length === 0 && (
              <Aviso texto="Cadastre um aluno para buscar motoristas e contratar o transporte." />
            )}
            {dados.alunos.map((a) => (
              <Cartao key={a.id} aoTocar={() => router.push({ pathname: '/aluno/[id]', params: { id: a.id } })}>
                <Text style={s.titulo}>{a.nome}</Text>
                <Text style={s.subtitulo}>{a.escola.nome}</Text>
                <Text style={s.pequeno}>Toque para registrar faltas</Text>
              </Cartao>
            ))}
            <Botao titulo="Cadastrar aluno" secundario aoTocar={() => router.push('/aluno/novo')} />
          </Secao>

          <Secao titulo="Mensalidades em aberto">
            {dados.abertas.length === 0 && <Aviso texto="Nenhuma mensalidade em aberto." />}
            {dados.abertas.map((m) => (
              <Cartao
                key={m.id}
                destaque={m.status === 'ATRASADA'}
                aoTocar={() => router.push({ pathname: '/contrato/[id]', params: { id: m.contratoId } })}>
                <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                  <Text style={s.titulo}>
                    {m.aluno.nome} · {competencia(m.competencia)}
                  </Text>
                  <Selo texto={STATUS_MENSALIDADE[m.status]} escuro={m.status === 'ATRASADA'} />
                </View>
                <Text style={s.subtitulo}>
                  {dinheiro(m.valor)} · vence {data(m.vencimento)}
                </Text>
                <Text style={s.pequeno}>Motorista: {m.motorista.nome}</Text>
              </Cartao>
            ))}
          </Secao>
        </>
      )}
    </Tela>
  );
}
