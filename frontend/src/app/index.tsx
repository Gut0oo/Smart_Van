import { Redirect } from 'expo-router';

import { inicioDoPapel, useAuth } from '@/services/auth';

// Sem login vai para a tela de entrar; logado vai para a área do seu papel.
export default function Index() {
  const { usuario } = useAuth();
  return <Redirect href={usuario ? inicioDoPapel(usuario.tipo) : '/login'} />;
}
