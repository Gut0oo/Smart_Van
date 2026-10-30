# Contrato da API — Smart Van

> Fonte de verdade da interface entre back-end e app. Agentes **não mudam** este contrato por conta própria:
> se algo precisar mudar, expliquem no relatório da etapa.

## Convenções

- Base: `http://localhost:8080` (sem context path). JSON em UTF-8. Datas `YYYY-MM-DD`, horários `HH:mm`, timestamps ISO-8601 UTC.
- Fuso de negócio: `America/Sao_Paulo` (bean `Clock`). "Hoje" = `LocalDate.now(clock)`.
- Identificação pelo header **`X-Usuario-Id`**: o login confere e-mail e senha e devolve o usuário; o app guarda o `id`
  e o envia nesse header em cada requisição. Toda rota exige o header, exceto as públicas:
  `POST /usuarios` (cadastro), `POST /auth/login`, `GET /escolas`, `GET /actuator/health`.
- **Atenção: o header não prova a identidade de quem chama.** Quem enviar o id de outra pessoa é tratado como ela.
  Este mecanismo é **só para uso acadêmico e não serve para produção**. As próximas etapas seguem este padrão.
- Papéis: `RESPONSAVEL`, `MOTORISTA`, `ADMINISTRADOR` (tipo do usuário). Rota de outro papel → 403.
- Dinheiro: número decimal com 2 casas (`350.00`).
- Listas não são paginadas (volume acadêmico).

### Erros (todas as respostas de erro)

```json
{ "status": 409, "codigo": "SEM_VAGAS", "mensagem": "O motorista não tem vagas disponíveis.",
  "campos": { "email": "já cadastrado" }, "timestamp": "2026-10-27T12:00:00Z" }
```

| HTTP | Quando | `codigo` |
|---|---|---|
| 400 | Bean Validation (`campos` preenchido) | `VALIDACAO` |
| 401 | Header `X-Usuario-Id` ausente, inválido ou de usuário inexistente / login errado | `NAO_AUTENTICADO`, `CREDENCIAIS_INVALIDAS` |
| 403 | Papel errado ou recurso de outra pessoa / conta bloqueada | `SEM_PERMISSAO`, `CONTA_BLOQUEADA` |
| 404 | Id inexistente | `NAO_ENCONTRADO` |
| 409 | Conflito de estado / duplicidade | ver cada endpoint |
| 422 | Regra de negócio não atendida | ver cada endpoint |

Exceção padrão no código: `br.com.smartvan.exception.RegraNegocioException(HttpStatus, codigo, mensagem)`.

### Objetos reutilizados

```ts
Endereco      { logradouro, numero, complemento?, bairro, cidade, uf, cep, latitude?, longitude? }
EnderecoResp  = Endereco & { id }
EscolaResumo  { id, nome }
Escola        { id, nome, telefone?, endereco: EnderecoResp }
AlunoResumo   { id, nome }
UsuarioResumo { id, nome, telefone }
```

`cep` só dígitos (8). `uf` 2 letras maiúsculas. Ao salvar endereço sem latitude/longitude, o back-end chama
`MapsProvider.geocodificar` e grava as coordenadas se vierem.

---

## 1. Login e cadastros mínimos (etapas 2 e 3)

| Método | Rota | Papel | Corpo → Resposta |
|---|---|---|---|
| POST | `/usuarios` | público | cadastro **original do projeto, sem mudança** (`UsuarioController`) → **201** |
| POST | `/auth/login` | público | `{ email, senha }` → `{ usuario: Usuario }` |
| GET | `/auth/me` | logado | → `Usuario` |
| GET | `/escolas?cidade=&nome=` | público | → `Escola[]` (filtros `contains`, sem acento/caixa não precisa) |
| POST | `/responsavel/alunos` | RESPONSAVEL | `{ nome, dataNascimento, serie?, escolaId, parentesco, endereco: Endereco }` → **201** `Aluno` |
| GET | `/responsavel/alunos` | RESPONSAVEL | → `Aluno[]` |
| POST | `/motorista/veiculos` | MOTORISTA | `{ placa, modelo, ano, capacidade (1..60), tipo }` → **201** `Veiculo` |
| GET | `/motorista/veiculos` | MOTORISTA | → `Veiculo[]` |
| PUT | `/motorista/escolas` | MOTORISTA | `{ escolaIds: number[] }` → `EscolaResumo[]` |
| GET | `/motorista/escolas` | MOTORISTA | → `Escola[]` |
| PUT | `/motorista/endereco-partida` | MOTORISTA | `Endereco` → `EnderecoResp` |
| GET | `/motorista/perfil` | MOTORISTA | → `{ usuario: Usuario, enderecoPartida?: EnderecoResp, veiculos: Veiculo[], escolas: EscolaResumo[] }` |

```ts
Usuario { id, nome, email, cpf, telefone, tipo, status, statusValidacao?: PENDENTE|EM_ANALISE|VALIDADO|REJEITADO }
Aluno   { id, nome, dataNascimento, serie?, parentesco, escola: EscolaResumo, endereco: EnderecoResp }
Veiculo { id, placa, modelo, ano, capacidade, tipo: VAN|MICRO_ONIBUS|CARRO }
```

Regras:
- O cadastro é o do projeto original (`POST /usuarios`): conta nasce `PENDENTE` e motorista com `statusValidacao = PENDENTE`.
  A validação do motorista por administrador está fora do escopo desta entrega.
- Placa duplicada → **409** `PLACA_JA_CADASTRADA`.
- Aluno: grava `alunos`, `alunos_enderecos` (tipo `RESIDENCIAL`, principal) e `responsaveis_alunos` (`status_vinculo ATIVO`, `acompanha_rota true`).
- Login: senha conferida com o `PasswordEncoder` (BCrypt) que o projeto já usa. E-mail ou senha errados → **401**
  `CREDENCIAIS_INVALIDAS` (mesma mensagem para os dois). Conta `BLOQUEADA` → **403** `CONTA_BLOQUEADA`. `PENDENTE` e `ATIVA` entram.
  Login atualiza `ultimo_acesso`.
- Rotas "logado" e com papel: `X-Usuario-Id` ausente, inválido ou de usuário inexistente → **401** `NAO_AUTENTICADO`.
  Conta `BLOQUEADA` → **403** `CONTA_BLOQUEADA`. Papel errado → **403** `SEM_PERMISSAO`. Não há logout: o app só esquece o id.

---

## 2. Buscar motorista + Contratação (etapa 4) — UC05, UC06

| Método | Rota | Papel | Corpo → Resposta |
|---|---|---|---|
| GET | `/motoristas?escolaId=&cidade=&nome=` | RESPONSAVEL | → `MotoristaBusca[]` |
| GET | `/motoristas/{id}` | RESPONSAVEL | → `MotoristaBusca` (perfil T08) |
| POST | `/contratos` | RESPONSAVEL | `{ motoristaId, alunoId, valorMensal }` → **201** `Contrato` (status `PENDENTE`) |
| GET | `/contratos?status=` | logado | → `Contrato[]` do usuário (como responsável ou motorista), mais recentes primeiro |
| GET | `/contratos/{id}` | parte | → `Contrato` |
| POST | `/contratos/{id}/aceitar` | MOTORISTA dono | → `Contrato` |
| POST | `/contratos/{id}/recusar` | MOTORISTA dono | → `Contrato` |

```ts
MotoristaBusca { id, nome, telefone, veiculo?: { modelo, tipo, capacidade }, escolas: EscolaResumo[],
                 cidades: string[], vagasRestantes }
Contrato { id, status, valorMensal, dataInicio, aluno: AlunoResumo, responsavel: UsuarioResumo,
           motorista: UsuarioResumo, escola?: EscolaResumo,
           cancelamento?: { dataSolicitacao, dataEncerramentoEfetivo, solicitante, motivo } }
```

Regras (seção 5 do contexto):
- Busca exige ≥1 aluno do responsável → senão **422** `ALUNO_OBRIGATORIO`.
- Só motoristas `statusValidacao = VALIDADO` (condição na consulta, não filtro em memória).
- Filtros: `escolaId` (atende a escola), `cidade` (cidade de alguma escola atendida), `nome` (contains, case-insensitive). Combinados com AND.
- `vagasRestantes = capacidade − contratos ATIVO do motorista`. Capacidade = **maior** capacidade entre os veículos do motorista (0 se não tem veículo). `veiculo` = esse veículo.
- Proposta: aluno precisa pertencer ao responsável (**403** `SEM_PERMISSAO`); motorista `VALIDADO` (**422** `MOTORISTA_NAO_VALIDADO`); vaga (**409** `SEM_VAGAS`); sem `PENDENTE` igual aluno+motorista (**409** `PROPOSTA_DUPLICADA`); aluno sem contrato `ATIVO` com qualquer motorista (**409** `ALUNO_JA_TEM_CONTRATO`). `dataInicio` = hoje na proposta.
- Aceitar: precisa estar `PENDENTE` (**409** `CONTRATO_NAO_PENDENTE`); rechecar vaga (**409** `SEM_VAGAS`) → `ATIVO`, `dataInicio = hoje`, notifica responsável, publica `ContratoAtivadoEvent`.
- Recusar: precisa estar `PENDENTE` → `RECUSADO`, notifica responsável.
- Criar proposta notifica o motorista.

---

## 3. Gerar rota + Faltas (etapa 7) — UC15

| Método | Rota | Papel | Corpo → Resposta |
|---|---|---|---|
| POST | `/alunos/{alunoId}/faltas` | RESPONSAVEL do aluno | `{ data, motivo? }` → **201** `Falta` |
| GET | `/alunos/{alunoId}/faltas?mes=YYYY-MM` | RESPONSAVEL do aluno | → `Falta[]` |
| DELETE | `/alunos/{alunoId}/faltas/{data}` | RESPONSAVEL do aluno | → **204** |
| POST | `/motorista/rotas/gerar` | MOTORISTA | `{ data, tipo: IDA\|VOLTA, escolaId, veiculoId?, horarioSaida? }` → `Rota` (**201** nova, **200** reaproveitada) |
| GET | `/motorista/rotas?data=` | MOTORISTA | → `Rota[]` do dia (padrão hoje) |
| GET | `/motorista/rotas/{id}` | MOTORISTA dono | → `Rota` |

```ts
Falta { id, data, motivo? }
Rota  { id, data, tipo, status, escola: EscolaResumo, veiculo: { id, placa, modelo }, otimizada,
        tempoEstimadoMinutos?, distanciaKm?, horarioInicio?, horarioFim?, polyline?,
        paradas: { ordem, tipoParada: EMBARQUE|DESEMBARQUE, aluno?: AlunoResumo, endereco: EnderecoResp,
                   horarioPrevisto? }[] }
```

Regras:
- Falta: data não pode ser passada (**422** `DATA_PASSADA`); duplicada → **409** `FALTA_JA_REGISTRADA`.
- Rota usa contratos `ATIVO` do motorista cujo aluno estuda na `escolaId`, menos alunos com falta na `data`.
- Sem alunos → **422** `ROTA_SEM_ALUNOS`. Motorista sem `enderecoPartida` → **422** `ENDERECO_PARTIDA_OBRIGATORIO`. Endereço sem coordenadas que não geocodifica → **422** `ENDERECO_SEM_COORDENADAS`.
- `veiculoId` opcional: se omitido e o motorista tem 1 veículo, usa ele; com 0 → **422** `VEICULO_OBRIGATORIO`; com vários → **422** `VEICULO_AMBIGUO`. Veículo de outro → **403**.
- IDA: partida = `enderecoPartida`, paradas `EMBARQUE` na casa de cada aluno, destino = escola (parada final `DESEMBARQUE`, sem aluno). VOLTA: sai da escola (`EMBARQUE` sem aluno), `DESEMBARQUE` na casa de cada aluno, termina no `enderecoPartida`.
- Reaproveitamento: se existe rota `PLANEJADA` com mesmo motorista+data+tipo+escola **e o mesmo conjunto de alunos**, devolve ela (200). Se o conjunto mudou (ex.: falta nova), apaga a antiga e gera outra.
- Otimização via `MapsProvider.otimizarRota`. Se lançar exceção ou devolver vazio: ordena por vizinho mais próximo (haversine) a partir da partida, `otimizada = false`, `tempoEstimadoMinutos`/`distanciaKm` estimados (haversine × 1,3 a 25 km/h) ou nulos, `polyline` nulo.
- `horarioSaida` padrão: IDA 06:30, VOLTA 12:30. `horarioPrevisto` de cada parada = saída + tempo acumulado.
- `data` passada → **422** `DATA_PASSADA`. Status inicial `PLANEJADA`.
- Depois de salvar: notifica cada responsável de aluno da rota com o horário previsto.
- Testes nunca chamam o Google: usar `FakeMapsProvider`.

---

## 4. Mensalidades (etapa 5) — UC10

| Método | Rota | Papel | Corpo → Resposta |
|---|---|---|---|
| GET | `/contratos/{id}/mensalidades` | parte do contrato | → `Mensalidade[]` (competência desc) |
| GET | `/responsavel/mensalidades?status=` | RESPONSAVEL | → `Mensalidade[]` |
| GET | `/motorista/cobrancas?status=&competencia=YYYY-MM` | MOTORISTA | → `Mensalidade[]` |
| POST | `/mensalidades/{id}/baixa` | MOTORISTA dono | `{ dataPagamento? }` → `Mensalidade` |

```ts
Mensalidade { id, contratoId, competencia /*YYYY-MM-01*/, valor, vencimento, status: PENDENTE|PAGA|ATRASADA,
              dataPagamento?, aluno: AlunoResumo, responsavel: UsuarioResumo, motorista: UsuarioResumo }
```

Regras:
- Vencimento = dia `smartvan.mensalidade.dia-vencimento` (padrão **10**) da competência. Se o contrato for ativado depois do dia 10, o vencimento da primeira é hoje + 5 dias.
- Ao receber `ContratoAtivadoEvent`: gera a mensalidade da competência atual (se não existir) e notifica o responsável.
- Agendador diário (`smartvan.agendador.mensalidades.cron`, padrão `0 0 6 * * *`, zona São Paulo): para cada contrato `ATIVO` gera a competência atual se não existir (idempotente pelo unique) e notifica; depois marca `ATRASADA` toda `PENDENTE` com `vencimento < hoje` e notifica responsável.
- Contrato com cancelamento solicitado continua `ATIVO` → continua gerando (regra do cancelamento).
- Baixa: contrato precisa ser do motorista (**403**); já `PAGA` → **409** `MENSALIDADE_JA_PAGA`. Aceita `PENDENTE` ou `ATRASADA` → `PAGA`, `dataPagamento` (padrão hoje; não pode ser futura → **422** `DATA_FUTURA`), grava `auditorias` (acao `BAIXA_MENSALIDADE`), notifica responsável.
- Serviço exposto para outros módulos: `MensalidadeConsultaService.existePendenteOuAtrasada(contratoId)`.

---

## 5. Cancelamento (etapa 6)

| Método | Rota | Papel | Corpo → Resposta |
|---|---|---|---|
| POST | `/contratos/{id}/cancelamento` | parte (RESPONSAVEL ou MOTORISTA) | `{ motivo }` (obrigatório, 5..500) → `Contrato` |

Regras:
- Solicitante precisa ser parte (**403**); contrato `ATIVO` (**409** `CONTRATO_NAO_ATIVO`); sem solicitação anterior (**409** `CANCELAMENTO_JA_SOLICITADO`).
- Grava `dataSolicitacaoCancelamento = hoje`, `dataEncerramentoEfetivo = hoje + 30`, `solicitanteCancelamento` (= tipo do usuário), `motivo`. Status **continua `ATIVO`**. Notifica a outra parte.
- Agendador diário (`smartvan.agendador.cancelamentos.cron`, padrão `0 30 6 * * *`): contratos `ATIVO` com `dataEncerramentoEfetivo <= hoje` → `ENCERRADO`; se `existePendenteOuAtrasada` → notifica as duas partes (tipo `CANCELAMENTO_COM_PENDENCIA`); senão notifica `CONTRATO_ENCERRADO`.

---

## 6. Notificações (etapa 1 — base, todos usam)

| Método | Rota | Papel | Resposta |
|---|---|---|---|
| GET | `/notificacoes?lidas=` | logado | `Notificacao[]` mais recentes primeiro |
| GET | `/notificacoes/nao-lidas/contagem` | logado | `{ total }` |
| POST | `/notificacoes/{id}/lida` | dono | **204** |

```ts
Notificacao { id, tipo, titulo, mensagem, referenciaId?, lida, dataCriacao }
```

Para criar: injetar `NotificacaoService` e chamar `notificar(usuarioId, TipoNotificacao, titulo, mensagem, referenciaId)`.
Tipos (`TipoNotificacao`): `PROPOSTA_RECEBIDA`, `PROPOSTA_ACEITA`, `PROPOSTA_RECUSADA`, `ROTA_GERADA`,
`MENSALIDADE_GERADA`, `MENSALIDADE_ATRASADA`, `MENSALIDADE_PAGA`, `CANCELAMENTO_SOLICITADO`,
`CONTRATO_ENCERRADO`, `CANCELAMENTO_COM_PENDENCIA`.
