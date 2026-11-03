import React from 'react';
import { useRouter } from 'expo-router';

import { Botao, Cartao, Linha, Secao, Tela } from '@/components/ui';
import { useAuth } from '@/services/auth';

// Dados da conta e sair; cada papel acrescenta suas seções em `children`.
export function TelaConta({ children }: { children?: React.ReactNode }) {
  const router = useRouter();
  const { usuario, sair } = useAuth();

  return (
    <Tela titulo="Minha conta">
      <Secao titulo="Dados">
        <Cartao>
          <Linha rotulo="Nome" valor={usuario?.nome} />
          <Linha rotulo="E-mail" valor={usuario?.email} />
          <Linha rotulo="Telefone" valor={usuario?.telefone} />
          {usuario?.statusValidacao && <Linha rotulo="Cadastro" valor={usuario.statusValidacao} />}
        </Cartao>
      </Secao>
      {children}
      <Botao
        titulo="Sair"
        secundario
        aoTocar={() => {
          sair();
          router.replace('/login');
        }}
      />
    </Tela>
  );
}
