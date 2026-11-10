// Conversões de data, hora, dinheiro e endereço entre a API e a tela.

import type { Endereco } from './tipos';

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

// 2026-11-03 -> 03/11/2026
export function data(iso?: string | null) {
  if (!iso) return '—';
  const [a, m, d] = iso.slice(0, 10).split('-');
  return `${d}/${m}/${a}`;
}

// 03/11/2026 -> 2026-11-03 (null se inválida)
export function dataParaApi(texto: string) {
  const m = texto.trim().match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
  if (!m) return null;
  const iso = `${m[3]}-${m[2]}-${m[1]}`;
  const d = new Date(`${iso}T12:00:00`);
  return Number.isNaN(d.getTime()) || d.getDate() !== Number(m[1]) ? null : iso;
}

// 2026-11-01 -> nov/2026
export function competencia(iso: string) {
  const [a, m] = iso.split('-');
  return `${MESES[Number(m) - 1]}/${a}`;
}

// 07:30:00 -> 07:30
export function hora(h?: string | null) {
  return h ? h.slice(0, 5) : '—';
}

export function horaValida(texto: string) {
  return /^([01]\d|2[0-3]):[0-5]\d$/.test(texto.trim());
}

export function dinheiro(v: number | string | null | undefined) {
  const n = Number(v ?? 0);
  return `R$ ${n.toFixed(2).replace('.', ',')}`;
}

// Data local (fuso do aparelho) em YYYY-MM-DD, deslocada em dias.
export function hojeIso(deslocamentoDias = 0) {
  const d = new Date();
  d.setDate(d.getDate() + deslocamentoDias);
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

export function dataHora(iso: string) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const p = (n: number) => String(n).padStart(2, '0');
  return `${p(d.getDate())}/${p(d.getMonth() + 1)} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

export const STATUS_CONTRATO: Record<string, string> = {
  PENDENTE: 'Aguardando motorista',
  ATIVO: 'Ativo',
  RECUSADO: 'Recusado',
  ENCERRADO: 'Encerrado',
};

export const STATUS_MENSALIDADE: Record<string, string> = {
  PENDENTE: 'Pendente',
  PAGA: 'Paga',
  ATRASADA: 'Atrasada',
};

// Rua Augusta, 500 (ap 12) · Consolação, São Paulo/SP
export function endereco(e?: Endereco | null) {
  if (!e) return '—';
  const complemento = e.complemento ? ` (${e.complemento})` : '';
  return `${e.logradouro}, ${e.numero}${complemento} · ${e.bairro}, ${e.cidade}/${e.uf}`;
}
