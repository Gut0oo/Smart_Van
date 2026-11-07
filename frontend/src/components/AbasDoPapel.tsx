import React from 'react';
import { Redirect, Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';

import { TipoUsuario } from '@/services/api';
import { inicioDoPapel, useAuth } from '@/services/auth';

export type Aba = { nome: string; titulo: string; icone: React.ComponentProps<typeof Ionicons>['name'] };

// Barra de abas de cada papel, no estilo da original; quem não é desse papel volta para a sua área.
export function AbasDoPapel({ papel, abas }: { papel: TipoUsuario; abas: Aba[] }) {
  const { usuario } = useAuth();
  if (!usuario) return <Redirect href="/login" />;
  if (usuario.tipo !== papel) return <Redirect href={inicioDoPapel(usuario.tipo)} />;

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#111',
        tabBarInactiveTintColor: '#888',
        tabBarStyle: {
          height: 60,
          paddingBottom: 8,
          paddingTop: 8,
          backgroundColor: '#fff',
          borderTopWidth: 1,
          borderTopColor: '#eee',
        },
      }}>
      {abas.map((a) => (
        <Tabs.Screen
          key={a.nome}
          name={a.nome}
          options={{
            title: a.titulo,
            tabBarIcon: ({ color, size }) => <Ionicons name={a.icone} size={size || 22} color={typeof color === 'number' ? undefined : (color ?? undefined)} />,
          }}
        />
      ))}
    </Tabs>
  );
}
