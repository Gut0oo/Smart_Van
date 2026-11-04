import React from 'react';
import { useRouter } from 'expo-router';

import { ListaContratos } from '@/components/ListaContratos';
import { Aviso, Botao, Carregando, Secao, Tela } from '@/components/ui';
import { api } from '@/services/api';
import { useAuth } from '@/services/auth';
import { Contrato } from '@/services/tipos';
import { useCarregar } from '@/services/useCarregar';

export default function InicioMotorista() {
  const router = useRouter();
  const { usuario } = useAuth();
  const { dados, carregando, erro, recarregar } = useCarregar(() => api.get<Contrato[]>('/contratos'));
  const pendentes = dados?.filter((c) => c.status === 'PENDENTE') ?? [];
  const ativos = dados?.filter((c) => c.status === 'ATIVO') ?? [];
  const validado = usuario?.statusValidacao === 'VALIDADO';

  return (
    <Tela titulo={`Olá, ${usuario?.nome.split(' ')[0] ?? ''}`} carregando={carregando} aoAtualizar={recarregar}>
      {!validado && (
        <Aviso
          texto={
            usuario?.statusValidacao === 'REJEITADO'
              ? 'Seu cadastro foi rejeitado pelo administrador. Veja a notificação com o motivo.'
              : 'Seu cadastro está aguardando aprovação. Enquanto isso, cadastre veículo, endereço de partida e horários em Conta.'
          }
        />
      )}
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {dados && (
        <>
          <Secao titulo="Propostas para responder">
            <ListaContratos contratos={pendentes} doMotorista vazio="Nenhuma proposta nova." />
          </Secao>
          <Secao titulo="Contratos ativos">
            <ListaContratos contratos={ativos} doMotorista vazio="Nenhum contrato ativo ainda." />
          </Secao>
        </>
      )}
      <Botao titulo="Configurar veículo, partida e horários" secundario aoTocar={() => router.push('/motorista/conta')} />
    </Tela>
  );
}
