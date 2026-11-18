export interface ReglaActivadaDto {
  codigo: string;
  nombre: string;
  fuente: string;
  puntosPenalizacion: number;
}

export interface SancionDto {
  idSancion: string;
  entidad: string;
  motivo: string;
  fechaSancion: string | null;
  montoMultaUit: number;
}

/** Respuesta del API Java. Los campos de detalle son vacíos en una respuesta de caché antigua. */
export interface ReporteRiesgo {
  ruc: string;
  razonSocial: string;
  estado: string;
  condicion: string;
  puntajeRiesgo: number;
  nivelRiesgo: 'BAJO' | 'MEDIO' | 'ALTO';
  reglasActivadas: string[];
  detalleReglas: ReglaActivadaDto[] | null;
  sanciones: SancionDto[] | null;
  fechaInicioActividades: string | null;
  inhabilitado: boolean;
  fechaConsulta: string | null;
}

export interface DashboardStats {
  totalEmpresasAnalizadas: number;
  empresasConHallazgos: number;
  porcentajeRiesgoAlto: number;
  scorePromedio: number;
  nivelRiesgoPromedio: string;
  distribucionRiesgo: Record<string, number>;
  reglasTopActivadas: Record<string, number>;
}

export interface ApiError {
  status?: number;
  error?: string;
  mensaje?: string;
  timestamp?: string;
}
