// Formatos devolvidos pela API (ver docs/CONTRATO_API.md).

export type Endereco = {
  id?: number;
  logradouro: string;
  numero: string;
  complemento?: string | null;
  bairro: string;
  cidade: string;
  uf: string;
  cep: string;
  latitude?: number | string | null;
  longitude?: number | string | null;
};

export type EscolaResumo = { id: number; nome: string };
export type Escola = EscolaResumo & { telefone?: string | null; endereco: Endereco };
export type AlunoResumo = { id: number; nome: string };
export type UsuarioResumo = { id: number; nome: string; telefone: string };

export type Aluno = {
  id: number;
  nome: string;
  dataNascimento: string;
  serie?: string | null;
  parentesco: string;
  escola: EscolaResumo;
  endereco: Endereco;
};

export type Veiculo = { id: number; placa: string; modelo: string; ano: number; capacidade: number; tipo: string };

export type Atendimento = {
  id: number;
  escola: Escola;
  horarioInicioAula: string;
  horarioSaidaMotorista: string;
  horarioSaidaEscola?: string | null;
  valorMensal?: number | string | null;
  tetoChegada: string;
};

export type VeiculoResumo = { modelo: string; tipo: string; capacidade: number };

export type AtendimentoBusca = {
  id: number;
  escola: EscolaResumo;
  cidade: string;
  horarioInicioAula: string;
  horarioSaidaMotorista: string;
  valorMensal?: number | string | null;
  vagasRestantes: number;
  contratosAtivos?: number;
  capacidade?: number;
};

export type MotoristaBusca = {
  id: number;
  nome: string;
  telefone: string;
  veiculo?: VeiculoResumo | null;
  totalClientes?: number;
  atendimentos: AtendimentoBusca[];
};

export type StatusContrato = 'PENDENTE' | 'ATIVO' | 'RECUSADO' | 'ENCERRADO';

export type Contrato = {
  id: number;
  status: StatusContrato;
  valorMensal: number | string;
  dataInicio: string;
  aluno: AlunoResumo;
  responsavel: UsuarioResumo;
  motorista: UsuarioResumo;
  escola?: EscolaResumo | null;
  atendimento?: { id: number; horarioInicioAula: string; horarioSaidaMotorista: string; valorMensal?: number | string | null } | null;
  cancelamento?: { dataSolicitacao: string; dataEncerramentoEfetivo: string; solicitante: string; motivo: string } | null;
  enderecoAluno?: Endereco | null;
};

export type Mensalidade = {
  id: number;
  contratoId: number;
  competencia: string;
  valor: number | string;
  vencimento: string;
  status: 'PENDENTE' | 'PAGA' | 'ATRASADA';
  dataPagamento?: string | null;
  aluno: AlunoResumo;
  responsavel: UsuarioResumo;
  motorista: UsuarioResumo;
};

export type Falta = { id: number; data: string; motivo?: string | null };

export type Parada = {
  ordem: number;
  tipoParada: 'EMBARQUE' | 'DESEMBARQUE';
  aluno?: AlunoResumo | null;
  endereco: Endereco;
  horarioPrevisto?: string | null;
};

export type Rota = {
  id: number;
  data: string;
  tipo: 'IDA' | 'VOLTA';
  status: string;
  atendimentoId?: number | null;
  escola: EscolaResumo;
  veiculo: { id: number; placa: string; modelo: string };
  otimizada: boolean;
  tempoEstimadoMinutos?: number | null;
  distanciaKm?: number | string | null;
  horarioInicio?: string | null;
  horarioFim?: string | null;
  foraDoTeto: boolean;
  minutosAdiantar?: number | null;
  paradas: Parada[];
};

export type Notificacao = {
  id: number;
  tipo: string;
  titulo: string;
  mensagem: string;
  referenciaId?: number | null;
  lida: boolean;
  dataCriacao: string;
};
