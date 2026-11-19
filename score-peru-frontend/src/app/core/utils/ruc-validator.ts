import { HttpErrorResponse } from '@angular/common/http';
import { DashboardStats, ReporteRiesgo } from '../models/reporte-riesgo.model';

const PREFIJOS_VALIDOS = ['10', '15', '17', '20'] as const;
const PONDERADORES_MODULO_11 = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2] as const;

export interface ResultadoValidacionRuc {
  valido: boolean;
  rucLimpio: string;
  mensaje: string;
}

export function verificarDigitoControlModulo11(ruc: string): boolean {
  const limpio = (ruc ?? '').trim();
  if (!/^\d{11}$/.test(limpio)) {
    return false;
  }
  let suma = 0;
  for (let i = 0; i < 10; i++) {
    suma += Number(limpio[i]) * PONDERADORES_MODULO_11[i];
  }
  const resto = suma % 11;
  const complemento = 11 - resto;
  const digitoEsperado = complemento === 10 ? 0 : complemento === 11 ? 1 : complemento;
  return Number(limpio[10]) === digitoEsperado;
}

export function validarRucPeruano(ruc: string): ResultadoValidacionRuc {
  const rucLimpio = (ruc ?? '').replace(/\s/g, '');
  if (!rucLimpio) {
    return {
      valido: false,
      rucLimpio,
      mensaje: 'El RUC es obligatorio y no puede estar vacío.',
    };
  }
  if (!/^\d{11}$/.test(rucLimpio)) {
    return {
      valido: false,
      rucLimpio,
      mensaje: 'Ingresa un RUC válido de exactamente 11 dígitos numéricos.',
    };
  }
  const prefijo = rucLimpio.slice(0, 2);
  if (!PREFIJOS_VALIDOS.includes(prefijo as (typeof PREFIJOS_VALIDOS)[number])) {
    return {
      valido: false,
      rucLimpio,
      mensaje: "El RUC debe iniciar con los dígitos '10', '15', '17' o '20'.",
    };
  }
  if (!verificarDigitoControlModulo11(rucLimpio)) {
    return {
      valido: false,
      rucLimpio,
      mensaje: 'El dígito verificador del RUC es inválido según el algoritmo Módulo 11 de SUNAT.',
    };
  }
  return { valido: true, rucLimpio, mensaje: '' };
}

export function normalizeText(value: string | null | undefined): string {
  return (value ?? '').trim().toLocaleUpperCase('es-PE');
}

export function riskClass(level: string | null | undefined): string {
  return ({ BAJO: 'low', MEDIO: 'mid', ALTO: 'high' } as Record<string, string>)[normalizeText(level)] ?? 'unknown';
}

export function riskLabel(level: string | null | undefined): string {
  return ({ BAJO: 'bajo', MEDIO: 'medio', ALTO: 'alto' } as Record<string, string>)[normalizeText(level)] ?? 'no disponible';
}

export function companyInitials(name: string | null | undefined): string {
  return (name || 'SP')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(part => part[0])
    .join('')
    .toLocaleUpperCase('es-PE');
}

export function clampScore(score: number): number {
  return Math.min(100, Math.max(0, Number(score) || 0));
}

export function gaugeAngle(score: number): string {
  const clamped = clampScore(score);
  // Si el puntaje es 0 (riesgo crítico), mostramos un arco mínimo de 18deg en rojo para que sea visible
  const effectiveScore = clamped === 0 ? 5 : clamped;
  return `${effectiveScore * 3.6}deg`;
}

export function gaugeColor(score: number, nivelRiesgo?: string | null): string {
  const clamped = clampScore(score);
  const norm = normalizeText(nivelRiesgo);
  if (norm === 'BAJO' || clamped >= 80) {
    return 'var(--green)';
  }
  if (norm === 'MEDIO' || (clamped >= 50 && clamped < 80)) {
    return '#d08a14';
  }
  return 'var(--red)';
}

export function scoreHeading(data: ReporteRiesgo): string {
  if (data.nivelRiesgo === 'ALTO') return 'Se detectaron factores de riesgo';
  if (data.nivelRiesgo === 'MEDIO') return 'Hay aspectos que conviene revisar';
  return 'No se detectaron alertas relevantes';
}

export function isCacheLimited(data: ReporteRiesgo): boolean {
  return !data.fechaConsulta && !(data.detalleReglas?.length) && !(data.sanciones?.length);
}

export function formatSanctionAmount(value: number): string {
  return Number.isFinite(value) && value > 0
    ? `${value.toLocaleString('es-PE')} UIT`
    : 'Monto no informado';
}

export function extractApiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as { mensaje?: string; message?: string } | null;
    if (body?.mensaje) return body.mensaje;
    if (error.status === 0) {
      return 'No se pudo conectar con el API. Confirma que el backend esté activo en http://localhost:8080.';
    }
    return body?.message || `El API respondió con un error (${error.status}).`;
  }
  return 'Ocurrió un error inesperado. Intenta nuevamente.';
}

export function downloadDashboardCsv(data: DashboardStats): void {
  const rows: unknown[][] = [
    ['Indicador', 'Valor'],
    ['Empresas analizadas', data.totalEmpresasAnalizadas],
    ['Empresas con hallazgos', data.empresasConHallazgos],
    ['Riesgo alto (%)', data.porcentajeRiesgoAlto],
    ['Score promedio', data.scorePromedio],
    ['Nivel promedio', data.nivelRiesgoPromedio],
    ...Object.entries(data.distribucionRiesgo ?? {}).map(([key, value]) => [`Distribución ${key}`, value]),
    ...Object.entries(data.reglasTopActivadas ?? {}).map(([key, value]) => [`Regla ${key}`, value]),
  ];
  const csv = rows
    .map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(','))
    .join('\n');
  const link = document.createElement('a');
  link.href = URL.createObjectURL(new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' }));
  link.download = 'score-peru-dashboard.csv';
  link.click();
  URL.revokeObjectURL(link.href);
}
