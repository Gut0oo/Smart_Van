import React from 'react';

import { ListaContratos } from '@/components/ListaContratos';
import { Aviso, Carregando, Secao, Tela } from '@/components/ui';
import { api } from '@/services/api';
import { Contrato } from '@/services/tipos';
import { useCarregar } from '@/services/useCarregar';

export default function ContratosResponsavel() {
  const { dados, carregando, erro, recarregar } = useCarregar(() => api.get<Contrato[]>('/contratos'));
  const ativos = dados?.filter((c) => c.status === 'ATIVO' || c.status === 'PENDENTE') ?? [];
  const antigos = dados?.filter((c) => c.status === 'RECUSADO' || c.status === 'ENCERRADO') ?? [];

  return (
    <Tela titulo="Contratos" carregando={carregando} aoAtualizar={recarregar}>
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {dados && (
        <>
          <Secao titulo="Ativos e propostas">
            <ListaContratos contratos={ativos} vazio="Nenhum contrato ainda. Use a busca para encontrar um motorista." />
          </Secao>
          {antigos.length > 0 && (
            <Secao titulo="Encerrados e recusados">
              <ListaContratos contratos={antigos} vazio="" />
            </Secao>
          )}
        </>
      )}
    </Tela>
  );
}
