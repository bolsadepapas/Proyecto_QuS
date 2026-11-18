import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { DashboardStats } from '../../core/models/reporte-riesgo.model';
import { ScoreService } from '../../core/services/score.service';
import {
  extractApiErrorMessage,
  riskClass,
  riskLabel,
} from '../../core/utils/ruc-validator';

@Component({
  selector: 'score-dashboard',
  standalone: true,
  imports: [DecimalPipe],
  template: `
    <section class="content dashboard-page">
      <div class="dashboard-heading">
        <div>
          <p class="eyebrow">Panorama nacional</p>
          <h1>Indicadores de riesgo comercial</h1>
          <p>Estadísticas agregadas de las consultas guardadas por el API.</p>
        </div>
        <button class="button button-quiet" (click)="loadDashboard()">↻ Actualizar</button>
      </div>

      @if (dashboardError()) {
        <div class="alert alert-error" role="alert">{{ dashboardError() }}</div>
      }
      @if (dashboardLoading()) {
        <div class="page-loading"><span class="spinner"></span> Cargando estadísticas…</div>
      }
      @if (dashboard(); as stats) {
        <div class="kpi-grid">
          <article class="panel kpi">
            <span>Empresas analizadas</span>
            <strong>{{ stats.totalEmpresasAnalizadas | number }}</strong>
            <small>Registros disponibles en caché</small>
          </article>
          <article class="panel kpi">
            <span>Empresas con hallazgos</span>
            <strong>{{ stats.empresasConHallazgos | number }}</strong>
            <small>Con una o más reglas activadas</small>
          </article>
          <article class="panel kpi">
            <span>Riesgo alto</span>
            <strong>{{ stats.porcentajeRiesgoAlto | number:'1.0-1' }}%</strong>
            <small>Porcentaje sobre las consultas registradas</small>
          </article>
          <article class="panel kpi">
            <span>Score promedio</span>
            <strong>
              {{ stats.scorePromedio | number:'1.0-1' }}
              <em [class]="'risk-pill ' + riskClass(stats.nivelRiesgoPromedio)">
                {{ riskLabel(stats.nivelRiesgoPromedio) }}
              </em>
            </strong>
            <small>Promedio devuelto por el API</small>
          </article>
        </div>

        <div class="dashboard-grid">
          <article class="panel chart-panel">
            <div class="panel-title">
              <div>
                <h2>Distribución por riesgo</h2>
                <p>Empresas según nivel actual</p>
              </div>
            </div>
            @if (distributionRows(stats).length) {
              <div class="distribution-list">
                @for (row of distributionRows(stats); track row.name) {
                  <div class="distribution-row">
                    <div>
                      <span class="risk-dot" [class]="'risk-dot ' + riskClass(row.name)"></span>
                      <b>{{ riskLabel(row.name) }}</b>
                      <span class="distribution-count">{{ row.value | number }}</span>
                    </div>
                    <div class="bar-track">
                      <span
                        [class]="'bar-fill ' + riskClass(row.name)"
                        [style.width.%]="distributionPercent(row.value, stats)"
                      ></span>
                    </div>
                    <small>{{ distributionPercent(row.value, stats) | number:'1.0-1' }}%</small>
                  </div>
                }
              </div>
            } @else {
              <div class="empty-inline">Aún no hay consultas para mostrar.</div>
            }
          </article>

          <article class="panel chart-panel">
            <div class="panel-title">
              <div>
                <h2>Reglas más activadas</h2>
                <p>Frecuencia en consultas guardadas</p>
              </div>
            </div>
            @if (topRules(stats).length) {
              <div class="top-rules">
                @for (row of topRules(stats); track row.name; let i = $index) {
                  <div class="top-rule">
                    <span>{{ (i + 1) | number:'2.0' }}</span>
                    <b>{{ row.name }}</b>
                    <strong>{{ row.value | number }}</strong>
                  </div>
                }
              </div>
            } @else {
              <div class="empty-inline">Aún no hay reglas registradas.</div>
            }
          </article>
        </div>

        <div class="data-note">
          El endpoint actual agrega registros de caché mediante consultas SQL en base de datos (COUNT, AVG y GROUP BY).
        </div>
      }
    </section>
  `,
})
export class DashboardComponent implements OnInit {
  private readonly scoreService = inject(ScoreService);

  readonly dashboard = this.scoreService.currentDashboard;
  readonly dashboardLoading = signal(false);
  readonly dashboardError = signal('');

  readonly riskClass = riskClass;
  readonly riskLabel = riskLabel;

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.dashboardLoading.set(true);
    this.dashboardError.set('');
    this.scoreService.obtenerDashboardStats().subscribe({
      next: () => this.dashboardLoading.set(false),
      error: error => {
        this.dashboardLoading.set(false);
        this.dashboardError.set(extractApiErrorMessage(error));
      },
    });
  }

  distributionRows(stats: DashboardStats) {
    return Object.entries(stats.distribucionRiesgo ?? {})
      .map(([name, value]) => ({ name, value: Number(value) || 0 }))
      .sort(
        (a, b) =>
          ({ ALTO: 0, MEDIO: 1, BAJO: 2 }[a.name as 'ALTO' | 'MEDIO' | 'BAJO'] ?? 3) -
          ({ ALTO: 0, MEDIO: 1, BAJO: 2 }[b.name as 'ALTO' | 'MEDIO' | 'BAJO'] ?? 3),
      );
  }

  distributionPercent(value: number, stats: DashboardStats): number {
    return stats.totalEmpresasAnalizadas > 0 ? (value / stats.totalEmpresasAnalizadas) * 100 : 0;
  }

  topRules(stats: DashboardStats) {
    return Object.entries(stats.reglasTopActivadas ?? {})
      .map(([name, value]) => ({ name, value: Number(value) || 0 }))
      .sort((a, b) => b.value - a.value);
  }
}
