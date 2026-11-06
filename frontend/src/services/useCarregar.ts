import { useCallback, useState } from 'react';
import { useFocusEffect } from 'expo-router';

import { ErroApi } from './api';

// Carrega dados da API sempre que a tela ganha foco; devolve dados, carregando, erro e recarregar.
export function useCarregar<T>(buscar: () => Promise<T>, deps: unknown[] = []) {
  const [dados, setDados] = useState<T | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const recarregar = useCallback(async () => {
    setCarregando(true);
    setErro(null);
    try {
      setDados(await buscar());
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setCarregando(false);
    }
  // As telas fornecem as dependências da busca (por exemplo, o ID da rota).
  // eslint-disable-next-line react-hooks/exhaustive-deps, react-hooks/use-memo
  }, deps);

  useFocusEffect(
    useCallback(() => {
      recarregar();
    }, [recarregar]),
  );

  return { dados, carregando, erro, recarregar };
}

export function mensagemDeErro(e: unknown) {
  if (e instanceof ErroApi) {
    if (e.campos && Object.keys(e.campos).length > 0) {
      return Object.entries(e.campos).map(([campo, msg]) => `${campo}: ${msg}`).join('\n');
    }
    return e.message;
  }
  return 'Algo deu errado. Tente de novo.';
}
