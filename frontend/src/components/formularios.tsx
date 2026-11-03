import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Dropdown } from 'react-native-element-dropdown';

import { Campo, cores } from '@/components/ui';
import { api } from '@/services/api';
import { Endereco, Escola } from '@/services/tipos';

export type EnderecoForm = {
  logradouro: string;
  numero: string;
  complemento: string;
  bairro: string;
  cidade: string;
  uf: string;
  cep: string;
};

export const enderecoVazio: EnderecoForm = {
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
  cep: '',
};

export function enderecoDe(e?: Endereco | null): EnderecoForm {
  if (!e) return enderecoVazio;
  return {
    logradouro: e.logradouro,
    numero: e.numero,
    complemento: e.complemento ?? '',
    bairro: e.bairro,
    cidade: e.cidade,
    uf: e.uf,
    cep: e.cep,
  };
}

// Corpo do endereço para a API: sem coordenadas, o back-end pede ao Google.
export function enderecoParaApi(e: EnderecoForm) {
  return {
    logradouro: e.logradouro.trim(),
    numero: e.numero.trim(),
    complemento: e.complemento.trim() || null,
    bairro: e.bairro.trim(),
    cidade: e.cidade.trim(),
    uf: e.uf.trim().toUpperCase(),
    cep: e.cep.replace(/\D/g, ''),
  };
}

export function enderecoCompleto(e: EnderecoForm) {
  return !!(e.logradouro && e.numero && e.bairro && e.cidade && e.uf.trim().length === 2 && e.cep.replace(/\D/g, '').length === 8);
}

export function CamposEndereco({ valor, aoMudar }: { valor: EnderecoForm; aoMudar: (e: EnderecoForm) => void }) {
  const muda = (campo: keyof EnderecoForm) => (texto: string) => aoMudar({ ...valor, [campo]: texto });
  return (
    <View>
      <Campo rotulo="Rua" value={valor.logradouro} onChangeText={muda('logradouro')} />
      <View style={styles.linha}>
        <View style={{ flex: 1 }}>
          <Campo rotulo="Número" value={valor.numero} onChangeText={muda('numero')} />
        </View>
        <View style={{ flex: 2 }}>
          <Campo rotulo="Complemento" value={valor.complemento} onChangeText={muda('complemento')} />
        </View>
      </View>
      <Campo rotulo="Bairro" value={valor.bairro} onChangeText={muda('bairro')} />
      <View style={styles.linha}>
        <View style={{ flex: 3 }}>
          <Campo rotulo="Cidade" value={valor.cidade} onChangeText={muda('cidade')} />
        </View>
        <View style={{ flex: 1 }}>
          <Campo rotulo="UF" value={valor.uf} onChangeText={muda('uf')} autoCapitalize="characters" maxLength={2} />
        </View>
      </View>
      <Campo rotulo="CEP (só números)" value={valor.cep} onChangeText={muda('cep')} keyboardType="number-pad" maxLength={9} />
    </View>
  );
}

// Lista suspensa de escolas (rota pública GET /escolas).
export function SeletorEscola({
  valor,
  aoMudar,
  rotulo = 'Escola',
}: {
  valor: number | null;
  aoMudar: (id: number) => void;
  rotulo?: string;
}) {
  const [escolas, setEscolas] = useState<Escola[]>([]);
  const [erro, setErro] = useState(false);

  useEffect(() => {
    api
      .get<Escola[]>('/escolas')
      .then(setEscolas)
      .catch(() => setErro(true));
  }, []);

  return (
    <View style={{ marginBottom: 12 }}>
      <Text style={styles.rotulo}>{rotulo}</Text>
      <Dropdown
        style={styles.dropdown}
        placeholderStyle={styles.textoDropdown}
        selectedTextStyle={styles.textoDropdown}
        data={escolas.map((e) => ({ valor: e.id, texto: `${e.nome} · ${e.endereco.cidade}` }))}
        labelField="texto"
        valueField="valor"
        value={valor}
        placeholder={erro ? 'Não foi possível carregar as escolas' : escolas.length ? 'Escolha a escola' : 'Nenhuma escola cadastrada'}
        search={escolas.length > 6}
        searchPlaceholder="Buscar escola"
        maxHeight={260}
        onChange={(item) => aoMudar(item.valor)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  linha: { flexDirection: 'row', gap: 8 },
  rotulo: { fontSize: 12, color: cores.textoClaro, marginBottom: 4 },
  dropdown: { height: 48, backgroundColor: '#EEEEEE', borderRadius: 22, paddingHorizontal: 16 },
  textoDropdown: { fontSize: 15, color: '#333' },
});
