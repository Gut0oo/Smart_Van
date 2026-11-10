import React from 'react';
import { Text, View } from 'react-native';
import { useRouter } from 'expo-router';

import { Aviso, Cartao, Selo, s } from '@/components/ui';
import { data, dinheiro, endereco, hora, STATUS_CONTRATO } from '@/services/formato';
import { Contrato } from '@/services/tipos';

// Cartões de contrato; `doMotorista` mostra o responsável em vez do motorista.
export function ListaContratos({
  contratos,
  doMotorista,
  vazio,
}: {
  contratos: Contrato[];
  doMotorista?: boolean;
  vazio: string;
}) {
  const router = useRouter();
  if (contratos.length === 0) return <Aviso texto={vazio} />;

  return (
    <>
      {contratos.map((c) => {
        const outro = doMotorista ? c.responsavel : c.motorista;
        return (
          <Cartao
            key={c.id}
            destaque={c.status === 'PENDENTE'}
            aoTocar={() => router.push({ pathname: '/contrato/[id]', params: { id: c.id } })}>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 8 }}>
              <Text style={[s.titulo, { flex: 1 }]}>{c.aluno.nome}</Text>
              <Selo
                texto={c.cancelamento && c.status === 'ATIVO' ? 'Em aviso prévio' : STATUS_CONTRATO[c.status]}
                escuro={c.status === 'PENDENTE'}
              />
            </View>
            <Text style={s.subtitulo}>
              {doMotorista ? 'Responsável' : 'Motorista'}: {outro.nome}
            </Text>
            <Text style={s.pequeno}>
              {c.escola?.nome ?? ''}
              {c.atendimento ? ` · aula ${hora(c.atendimento.horarioInicioAula)}` : ''} · {dinheiro(c.valorMensal)}/mês
            </Text>
            {doMotorista && c.enderecoAluno && <Text style={s.pequeno}>Endereço: {endereco(c.enderecoAluno)}</Text>}
            {c.status === 'ATIVO' && <Text style={s.pequeno}>Início: {data(c.dataInicio)}</Text>}
          </Cartao>
        );
      })}
    </>
  );
}
