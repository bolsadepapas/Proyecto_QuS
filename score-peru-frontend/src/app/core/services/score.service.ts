import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { DashboardStats, ReporteRiesgo } from '../models/reporte-riesgo.model';
import { HistorialService } from './historial.service';

@Injectable({ providedIn: 'root' })
export class ScoreService {
  private readonly http = inject(HttpClient);
  private readonly historialService = inject(HistorialService);
  private readonly baseUrl = (globalThis as { SCORE_PERU_API_URL?: string }).SCORE_PERU_API_URL
    ?? '/api/v1';

  readonly currentReport = signal<ReporteRiesgo | null>(null);
  readonly currentDashboard = signal<DashboardStats | null>(null);

  consultarRuc(ruc: string): Observable<ReporteRiesgo> {
    return this.http.get<ReporteRiesgo>(`${this.baseUrl}/consulta/${encodeURIComponent(ruc)}`).pipe(
      map(response => ({
        ...response,
        reglasActivadas: response.reglasActivadas ?? [],
        detalleReglas: response.detalleReglas ?? [],
        sanciones: response.sanciones ?? [],
      })),
      tap(reporte => {
        this.currentReport.set(reporte);
        this.historialService.guardar({
          ruc: reporte.ruc,
          razonSocial: reporte.razonSocial,
          nivelRiesgo: reporte.nivelRiesgo,
          puntajeRiesgo: reporte.puntajeRiesgo,
          fecha: reporte.fechaConsulta ?? new Date().toISOString(),
        });
      }),
    );
  }

  obtenerDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.baseUrl}/dashboard/stats`).pipe(
      tap(stats => this.currentDashboard.set(stats)),
    );
  }
}
