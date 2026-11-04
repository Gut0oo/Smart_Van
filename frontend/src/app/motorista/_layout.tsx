import { AbasDoPapel } from '@/components/AbasDoPapel';

export default function MotoristaLayout() {
  return (
    <AbasDoPapel
      papel="MOTORISTA"
      abas={[
        { nome: 'index', titulo: 'Início', icone: 'home-outline' },
        { nome: 'rota', titulo: 'Rota', icone: 'map-outline' },
        { nome: 'cobrancas', titulo: 'Cobranças', icone: 'cash-outline' },
        { nome: 'notificacoes', titulo: 'Notificações', icone: 'notifications-outline' },
        { nome: 'conta', titulo: 'Conta', icone: 'person-outline' },
      ]}
    />
  );
}
