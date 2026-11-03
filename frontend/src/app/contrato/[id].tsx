import React, { useState } from 'react';
import { Text, View } from 'react-native';
import { useLocalSearchParams } from 'expo-router';

import { Aviso, Botao, Campo, Carregando, Cartao, Linha, Secao, Selo, Tela, confirmar, s } from '@/components/ui';
import { api } from '@/services/api';
import { useAuth } from '@/services/auth';
import { competencia, data, dinheiro, hora, STATUS_CONTRATO, STATUS_MENSALIDADE } from '@/services/formato';
import { Contrato, Mensalidade } from '@/services/tipos';
import { mensagemDeErro, useCarregar } from '@/services/useCarregar';

// Detalhe do contrato: aceitar/recusar (motorista), mensalidades e baixa, e pedido de cancelamento.
export default function ContratoScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { usuario } = useAuth();
  const ehMotorista = usuario?.tipo === 'MOTORISTA';
  const { dados, carregando, erro, recarregar } = useCarregar(async () => {
    const contrato = await api.get<Contrato>(`/contratos/${id}`);
    const mensalidades =
      contrato.status === 'ATIVO' || contrato.status === 'ENCERRADO'
        ? await api.get<Mensalidade[]>(`/contratos/${id}/mensalidades`)
        : [];
    return { contrato, mensalidades };
  }, [id]);

  const [motivo, setMotivo] = useState('');
  const [mostrarCancelar, setMostrarCancelar] = useState(false);
  const [acao, setAcao] = useState<string | null>(null);
  const [erroAcao, setErroAcao] = useState<string | null>(null);

  async function executar(nome: string, chamada: () => Promise<unknown>) {
    setErroAcao(null);
    setAcao(nome);
    try {
      await chamada();
      setMostrarCancelar(false);
      setMotivo('');
      await recarregar();
    } catch (e) {
      setErroAcao(mensagemDeErro(e));
    } finally {
      setAcao(null);
    }
  }

  const c = dados?.contrato;
  const emAviso = c?.status === 'ATIVO' && !!c.cancelamento;
  const comecaDepois = c?.status === 'ATIVO' && c.dataInicio > new Date().toISOString().slice(0, 10);

  return (
    <Tela titulo="Contrato" voltar carregando={carregando} aoAtualizar={recarregar}>
      {carregando && !dados && <Carregando />}
      {erro && <Aviso texto={erro} erro />}
      {c && (
        <>
          <Cartao destaque>
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', gap: 8 }}>
              <Text style={[s.titulo, { flex: 1 }]}>{c.aluno.nome}</Text>
              <Selo texto={emAviso ? 'Em aviso prévio' : STATUS_CONTRATO[c.status]} escuro />
            </View>
            <Linha rotulo="Motorista" valor={c.motorista.nome} />
            <Linha rotulo="Telefone do motorista" valor={c.motorista.telefone} />
            <Linha rotulo="Responsável" valor={c.responsavel.nome} />
            <Linha rotulo="Telefone do responsável" valor={c.responsavel.telefone} />
            <Linha rotulo="Escola" valor={c.escola?.nome} />
            {c.atendimento && (
              <Linha
                rotulo="Horário"
                valor={`aula ${hora(c.atendimento.horarioInicioAula)} · saída ${hora(c.atendimento.horarioSaidaMotorista)}`}
              />
            )}
            <Linha rotulo="Mensalidade" valor={dinheiro(c.valorMensal)} />
            <Linha rotulo={c.status === 'PENDENTE' ? 'Proposta em' : 'Início'} valor={data(c.dataInicio)} />
          </Cartao>

          {comecaDepois && (
            <Aviso texto={`Este contrato começa em ${data(c.dataInicio)}, quando o anterior do aluno encerrar.`} />
          )}

          {c.cancelamento && (
            <Secao titulo="Cancelamento">
              <Cartao>
                <Linha rotulo="Pedido em" valor={data(c.cancelamento.dataSolicitacao)} />
                <Linha rotulo="Encerra em" valor={data(c.cancelamento.dataEncerramentoEfetivo)} />
                <Linha rotulo="Pedido por" valor={c.cancelamento.solicitante === 'MOTORISTA' ? 'Motorista' : 'Responsável'} />
                <Text style={[s.pequeno, { marginTop: 8 }]}>Motivo: {c.cancelamento.motivo}</Text>
              </Cartao>
            </Secao>
          )}

          {erroAcao && <Aviso texto={erroAcao} erro />}

          {ehMotorista && c.status === 'PENDENTE' && (
            <View style={{ gap: 8, marginBottom: 24 }}>
              <Botao
                titulo="Aceitar proposta"
                carregando={acao === 'aceitar'}
                aoTocar={() => executar('aceitar', () => api.post(`/contratos/${c.id}/aceitar`))}
              />
              <Botao
                titulo="Recusar proposta"
                secundario
                carregando={acao === 'recusar'}
                aoTocar={() =>
                  confirmar('Recusar proposta', 'A família será avisada.', () =>
                    executar('recusar', () => api.post(`/contratos/${c.id}/recusar`)),
                  )
                }
              />
            </View>
          )}
          {!ehMotorista && c.status === 'PENDENTE' && <Aviso texto="Aguardando a resposta do motorista." />}

          {dados.mensalidades.length > 0 && (
            <Secao titulo="Mensalidades">
              {dados.mensalidades.map((m) => (
                <Cartao key={m.id} destaque={m.status === 'ATRASADA'}>
                  <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                    <Text style={s.titulo}>{competencia(m.competencia)}</Text>
                    <Selo texto={STATUS_MENSALIDADE[m.status]} escuro={m.status === 'ATRASADA'} />
                  </View>
                  <Text style={s.subtitulo}>
                    {dinheiro(m.valor)} · vence {data(m.vencimento)}
                  </Text>
                  {m.dataPagamento && <Text style={s.pequeno}>Pago em {data(m.dataPagamento)}</Text>}
                  {ehMotorista && m.status !== 'PAGA' && (
                    <View style={{ marginTop: 12 }}>
                      <Botao
                        titulo="Registrar pagamento"
                        secundario
                        carregando={acao === `baixa${m.id}`}
                        aoTocar={() =>
                          confirmar('Registrar pagamento', `Confirmar o pagamento de ${competencia(m.competencia)} hoje?`, () =>
                            executar(`baixa${m.id}`, () => api.post(`/mensalidades/${m.id}/baixa`, {})),
                          )
                        }
                      />
                    </View>
                  )}
                </Cartao>
              ))}
            </Secao>
          )}

          {c.status === 'ATIVO' && !c.cancelamento && (
            <Secao titulo="Cancelar contrato">
              {!mostrarCancelar ? (
                <Botao titulo="Pedir cancelamento" perigo aoTocar={() => setMostrarCancelar(true)} />
              ) : (
                <>
                  <Aviso texto="O contrato continua ativo por 30 dias de aviso prévio e depois é encerrado automaticamente." />
                  <Campo
                    rotulo="Motivo (mínimo 5 caracteres)"
                    value={motivo}
                    onChangeText={setMotivo}
                    multiline
                    style={{ height: 80, borderWidth: 1, borderColor: '#ccc', borderRadius: 8, padding: 12 }}
                  />
                  <View style={{ gap: 8 }}>
                    <Botao
                      titulo="Confirmar cancelamento"
                      perigo
                      desativado={motivo.trim().length < 5}
                      carregando={acao === 'cancelar'}
                      aoTocar={() =>
                        executar('cancelar', () => api.post(`/contratos/${c.id}/cancelamento`, { motivo: motivo.trim() }))
                      }
                    />
                    <Botao titulo="Voltar" secundario aoTocar={() => setMostrarCancelar(false)} />
                  </View>
                </>
              )}
            </Secao>
          )}
        </>
      )}
    </Tela>
  );
}
