// Conexão do app com o back-end. Endereço vem de EXPO_PUBLIC_API_URL (ver .env.example).
const API_URL = (process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080').replace(/\/+$/, '');

export type TipoUsuario = 'RESPONSAVEL' | 'MOTORISTA' | 'ADMINISTRADOR';

export type Usuario = {
  id: number;
  nome: string;
  email: string;
  cpf: string;
  telefone: string;
  tipo: TipoUsuario;
  status: string;
  statusValidacao?: string;
};

// Falha devolvida pela API (status, codigo e mensagem do contrato) ou falta de conexão (status 0).
export class ErroApi extends Error {
  status: number;
  codigo: string;
  campos?: Record<string, string>;

  constructor(status: number, codigo: string, mensagem: string, campos?: Record<string, string>) {
    super(mensagem);
    this.name = 'ErroApi';
    this.status = status;
    this.codigo = codigo;
    this.campos = campos;
  }
}

// Guardado só em memória: fechar o app exige novo login.
let usuarioLogado: Usuario | null = null;

export function getUsuarioLogado() {
  return usuarioLogado;
}

async function requisitar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (corpo !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (usuarioLogado) {
    headers['X-Usuario-Id'] = String(usuarioLogado.id);
  }

  let resposta: Response;
  try {
    resposta = await fetch(`${API_URL}${caminho}`, {
      method: metodo,
      headers,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroApi(0, 'SEM_CONEXAO', `Não foi possível conectar à API em ${API_URL}.`);
  }

  const texto = await resposta.text();
  let dados: any;
  try {
    dados = texto ? JSON.parse(texto) : undefined;
  } catch {
    dados = undefined;
  }

  if (!resposta.ok) {
    throw new ErroApi(
      resposta.status,
      dados?.codigo ?? 'ERRO_INESPERADO',
      dados?.mensagem ?? `A API respondeu com status ${resposta.status}.`,
      dados?.campos,
    );
  }
  return dados as T;
}

export const api = {
  get: <T>(caminho: string) => requisitar<T>('GET', caminho),
  post: <T>(caminho: string, corpo?: unknown) => requisitar<T>('POST', caminho, corpo),
  put: <T>(caminho: string, corpo?: unknown) => requisitar<T>('PUT', caminho, corpo),
  delete: <T>(caminho: string) => requisitar<T>('DELETE', caminho),
};

export async function login(email: string, senha: string) {
  const resposta = await api.post<{ usuario: Usuario }>('/auth/login', { email, senha });
  usuarioLogado = resposta.usuario;
  return resposta.usuario;
}

export function sair() {
  usuarioLogado = null;
}

export function definirUsuario(usuario: Usuario | null) {
  usuarioLogado = usuario;
}

export async function verificarConexao() {
  const resposta = await api.get<{ status: string }>('/actuator/health');
  return resposta.status === 'UP';
}
