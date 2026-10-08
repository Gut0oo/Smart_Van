import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';

export default function TabsLayout() {
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
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Início',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="home-outline" size={size || 22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="busca"
        options={{
          title: 'Busca',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="search-outline" size={size || 22} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="notificacao"
        options={{
          title: 'Notificações',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="notifications-outline" size={size || 22} color={color} />
          ),
        }}
      />
    </Tabs>
  );
}