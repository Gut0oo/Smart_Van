import React from 'react';
import { Text } from 'react-native';
import { useRouter } from 'expo-router';

import { TelaConta } from '@/components/TelaConta';
import { Aviso, Botao, Carregando, Cartao, Linha, Secao, s } from '@/components/ui';
import { api, Usuario } from '@/services/api';
import { useAuth } from '@/services/auth';
import { hora } from '@/services/formato';
import { Atendimento, Endereco, Veiculo } from '@/services/tipos';
import { useCarregar } from '@/services/useCarregar';

type Perfil = { usuario: Usuario; enderecoPartida?: Endereco | null; veiculos: Veiculo[] };

// Conta do motorista: veículos, endereço de partida e horários de escola (atendimentos).
export default function ContaMotorista() {
  const router = useRouter();
  const { atualizar } = useAuth();
  const { dados, carregando, erro } = useCarregar(async () => {
    const [perfil, atendimentos] = await Promise.all([
      api.get<Perfil>('/motorista/perfil'),
      api.get<Atendimento[]>('/motorista/escolas'),
    ]);
    atualizar(perfil.usuario); // mantém o status de validação em dia
    return { perfil, atendimentos };
  });

  const p = dados?.perfil;

  return (
    <TelaConta>
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {p && dados && (
        <>
          <Secao titulo="Veículos">
            {p.veiculos.length === 0 && <Aviso texto="Cadastre o veículo para ter vagas na busca." />}
            {p.veiculos.map((v) => (
              <Cartao key={v.id}>
                <Text style={s.titulo}>
                  {v.modelo} · {v.ano}
                </Text>
                <Text style={s.subtitulo}>
                  {v.tipo} · placa {v.placa} · {v.capacidade} lugares
                </Text>
              </Cartao>
            ))}
            <Botao titulo="Adicionar veículo" secundario aoTocar={() => router.push('/config/veiculo')} />
          </Secao>

          <Secao titulo="Endereço de partida">
            <Cartao>
              {p.enderecoPartida ? (
                <>
                  <Text style={s.titulo}>
                    {p.enderecoPartida.logradouro}, {p.enderecoPartida.numero}
                  </Text>
                  <Text style={s.subtitulo}>
                    {p.enderecoPartida.bairro} · {p.enderecoPartida.cidade}
                  </Text>
                </>
              ) : (
                <Text style={s.subtitulo}>Não cadastrado: é de onde a van sai na ida e para onde volta.</Text>
              )}
            </Cartao>
            <Botao titulo="Alterar endereço de partida" secundario aoTocar={() => router.push('/config/partida')} />
          </Secao>

          <Secao titulo="Escolas e horários">
            {dados.atendimentos.length === 0 && <Aviso texto="Cadastre as escolas e horários que você atende." />}
            {dados.atendimentos.map((a) => (
              <Cartao key={a.id}>
                <Text style={s.titulo}>{a.escola.nome}</Text>
                <Linha rotulo="Início da aula" valor={hora(a.horarioInicioAula)} />
                <Linha rotulo="Sua saída (ida)" valor={hora(a.horarioSaidaMotorista)} />
                <Linha rotulo="Saída da escola (volta)" valor={hora(a.horarioSaidaEscola)} />
                <Linha rotulo="Chegar até" valor={hora(a.tetoChegada)} />
              </Cartao>
            ))}
            <Botao titulo="Editar escolas e horários" secundario aoTocar={() => router.push('/config/turnos')} />
          </Secao>
        </>
      )}
    </TelaConta>
  );
}
