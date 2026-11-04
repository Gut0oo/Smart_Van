import { AbasDoPapel } from '@/components/AbasDoPapel';

export default function ResponsavelLayout() {
  return (
    <AbasDoPapel
      papel="RESPONSAVEL"
      abas={[
        { nome: 'index', titulo: 'Início', icone: 'home-outline' },
        { nome: 'busca', titulo: 'Busca', icone: 'search-outline' },
        { nome: 'contratos', titulo: 'Contratos', icone: 'document-text-outline' },
        { nome: 'notificacoes', titulo: 'Notificações', icone: 'notifications-outline' },
        { nome: 'conta', titulo: 'Conta', icone: 'person-outline' },
      ]}
    />
  );
}
