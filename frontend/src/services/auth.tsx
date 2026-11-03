import React, { createContext, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

import { definirUsuario, login as loginApi, sair as sairApi, Usuario } from './api';

type Auth = {
  usuario: Usuario | null;
  entrar: (email: string, senha: string) => Promise<Usuario>;
  sair: () => void;
  atualizar: (usuario: Usuario) => void;
};

const AuthContext = createContext<Auth | null>(null);
const CHAVE = 'smartvan.usuario';

// Guarda o usuário logado para as telas e no aparelho, para não pedir login de novo ao reabrir o app.
// O id vai no header X-Usuario-Id (ver api.ts).
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [pronto, setPronto] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(CHAVE)
      .then((salvo) => {
        if (salvo) {
          const u = JSON.parse(salvo) as Usuario;
          definirUsuario(u);
          setUsuario(u);
        }
      })
      .catch(() => {})
      .finally(() => setPronto(true));
  }, []);

  function guardar(u: Usuario | null) {
    definirUsuario(u);
    setUsuario(u);
    (u ? AsyncStorage.setItem(CHAVE, JSON.stringify(u)) : AsyncStorage.removeItem(CHAVE)).catch(() => {});
  }

  const valor: Auth = {
    usuario,
    entrar: async (email, senha) => {
      const u = await loginApi(email, senha);
      guardar(u);
      return u;
    },
    sair: () => {
      sairApi();
      guardar(null);
    },
    atualizar: guardar,
  };

  // Espera ler o aparelho antes de decidir entre login e área do papel.
  if (!pronto) return null;
  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth precisa estar dentro de AuthProvider');
  }
  return ctx;
}

// Tela inicial de cada papel.
export function inicioDoPapel(tipo: Usuario['tipo']) {
  if (tipo === 'MOTORISTA') return '/motorista' as const;
  if (tipo === 'ADMINISTRADOR') return '/admin' as const;
  return '/responsavel' as const;
}
