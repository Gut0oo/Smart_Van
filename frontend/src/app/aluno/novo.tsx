import React, { useState } from 'react';

import { CamposEndereco, enderecoCompleto, enderecoParaApi, enderecoVazio, SeletorEscola } from '@/components/formularios';
import { Aviso, Botao, Campo, Opcoes, Secao, Tela, useVoltar } from '@/components/ui';
import { api } from '@/services/api';
import { dataParaApi } from '@/services/formato';
import { mensagemDeErro } from '@/services/useCarregar';

type Parentesco = 'PAI' | 'MAE' | 'AVO' | 'TIO' | 'RESPONSAVEL_LEGAL';

export default function NovoAluno() {
  const voltar = useVoltar();
  const [nome, setNome] = useState('');
  const [nascimento, setNascimento] = useState('');
  const [serie, setSerie] = useState('');
  const [escolaId, setEscolaId] = useState<number | null>(null);
  const [parentesco, setParentesco] = useState<Parentesco | null>(null);
  const [endereco, setEndereco] = useState(enderecoVazio);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const dataNascimento = dataParaApi(nascimento);
  const completo = nome.trim() && dataNascimento && escolaId && parentesco && enderecoCompleto(endereco);

  async function salvar() {
    setErro(null);
    setEnviando(true);
    try {
      await api.post('/responsavel/alunos', {
        nome: nome.trim(),
        dataNascimento,
        serie: serie.trim() || null,
        escolaId,
        parentesco,
        endereco: enderecoParaApi(endereco),
      });
      voltar('/responsavel');
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tela
      titulo="Cadastrar aluno"
      voltar
      rodape={<Botao titulo="Salvar aluno" aoTocar={salvar} carregando={enviando} desativado={!completo} />}>
      <Secao titulo="Aluno">
        <Campo rotulo="Nome" value={nome} onChangeText={setNome} />
        <Campo
          rotulo="Data de nascimento"
          placeholder="DD/MM/AAAA"
          value={nascimento}
          onChangeText={setNascimento}
          keyboardType="numbers-and-punctuation"
          maxLength={10}
        />
        {nascimento.length === 10 && !dataNascimento && <Aviso texto="Data inválida. Use DD/MM/AAAA." erro />}
        <Campo rotulo="Série (opcional)" value={serie} onChangeText={setSerie} />
        <SeletorEscola valor={escolaId} aoMudar={setEscolaId} />
        <Opcoes
          rotulo="Parentesco"
          valor={parentesco}
          aoMudar={setParentesco}
          opcoes={[
            { valor: 'MAE', texto: 'Mãe' },
            { valor: 'PAI', texto: 'Pai' },
            { valor: 'AVO', texto: 'Avô/Avó' },
            { valor: 'TIO', texto: 'Tio/Tia' },
            { valor: 'RESPONSAVEL_LEGAL', texto: 'Responsável legal' },
          ]}
        />
      </Secao>
      <Secao titulo="Endereço de casa (onde a van busca)">
        <CamposEndereco valor={endereco} aoMudar={setEndereco} />
      </Secao>
      {erro && <Aviso texto={erro} erro />}
    </Tela>
  );
}
