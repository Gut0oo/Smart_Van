import { AbasDoPapel } from '@/components/AbasDoPapel';

export default function AdminLayout() {
  return (
    <AbasDoPapel
      papel="ADMINISTRADOR"
      abas={[
        { nome: 'index', titulo: 'Motoristas', icone: 'shield-checkmark-outline' },
        { nome: 'escolas', titulo: 'Escolas', icone: 'school-outline' },
        { nome: 'conta', titulo: 'Conta', icone: 'person-outline' },
      ]}
    />
  );
}
