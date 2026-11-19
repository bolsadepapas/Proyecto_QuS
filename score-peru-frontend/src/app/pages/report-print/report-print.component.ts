import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ReporteRiesgo } from '../../core/models/reporte-riesgo.model';
import { ScoreService } from '../../core/services/score.service';
import {
  clampScore,
  companyInitials,
  extractApiErrorMessage,
  formatSanctionAmount,
  gaugeAngle,
  gaugeColor,
  isCacheLimited,
  normalizeText,
  riskClass,
  riskLabel,
  scoreHeading,
} from '../../core/utils/ruc-validator';

@Component({
  selector: 'score-report-print',
  standalone: true,
  imports: [DatePipe],
  template: `
    @if (loading()) {
      <div class="page-loading"><span class="spinner"></span> Preparando vista imprimible del reporte…</div>
    } @else if (loadError()) {
      <div class="content">
        <div class="alert alert-error" role="alert">{{ loadError() }}</div>
        <button class="button button-primary" (click)="retry()">Volver a intentar</button>
      </div>
    } @else {
      @if (report(); as data) {
        <section class="content result-page report-view">
        <div class="result-heading">
          <div class="company-heading">
            <div class="company-avatar">{{ initials(data.razonSocial) }}</div>
            <div>
              <h1>{{ data.razonSocial || 'Razón social no disponible' }}</h1>
              <p>
                RUC {{ data.ruc }}
                <span class="separator">·</span>
                Consultado {{ data.fechaConsulta ? (data.fechaConsulta | date:'d MMM y, HH:mm') : 'fecha no disponible' }}
              </p>
            </div>
          </div>
          <span class="risk-pill risk-large" [class]="'risk-pill risk-large ' + riskClass(data.nivelRiesgo)">
            <i></i>Riesgo {{ riskLabel(data.nivelRiesgo) }}
          </span>
        </div>

        @if (cacheLimited(data)) {
          <div class="notice">
            <b>Detalle limitado por caché</b>
            <span>El API devolvió el score y los datos base, pero no el desglose de reglas ni las sanciones para esta consulta.</span>
          </div>
        }

        <div class="result-grid">
          <div class="result-main-column">
            <article class="panel score-panel">
              <div
                class="score-gauge"
                [class]="'score-gauge ' + riskClass(data.nivelRiesgo)"
                [style.--score-angle]="gaugeAngle(data.puntajeRiesgo)"
                [style.--gauge-color]="gaugeColor(data.puntajeRiesgo, data.nivelRiesgo)"
              >
                <div>
                  <strong [style.color]="gaugeColor(data.puntajeRiesgo, data.nivelRiesgo)">{{ data.puntajeRiesgo }}</strong>
                  <span>de 100 puntos</span>
                </div>
              </div>
              <div class="score-description">
                <p class="eyebrow">Score comercial</p>
                <h2>{{ scoreHeading(data) }}</h2>
                <p>El puntaje y el nivel de riesgo los calcula el motor del API. El front muestra esos valores sin recalcularlos.</p>
                <div class="score-track">
                  <span [style.width.%]="clampScore(data.puntajeRiesgo)"></span>
                </div>
                <div class="scale-labels">
                  <span>Mayor riesgo</span>
                  <span>Menor riesgo</span>
                </div>
              </div>
            </article>

            <article class="panel rules-panel">
              <div class="panel-title">
                <div>
                  <h2>¿Por qué obtuvo este puntaje?</h2>
                  <p>Reglas activadas por el motor de scoring</p>
                </div>
                <span class="count-badge">{{ rules(data).length }}</span>
              </div>
              @if (rules(data).length) {
                @for (rule of rules(data); track rule.codigo + rule.nombre) {
                  <div class="rule-row">
                    <div class="rule-code">{{ rule.codigo || '•' }}</div>
                    <div class="rule-copy">
                      <strong>{{ rule.nombre }}</strong>
                      <span>{{ rule.fuente || 'Fuente no especificada' }}</span>
                    </div>
                    <b class="penalty">−{{ rule.puntosPenalizacion }} pts</b>
                  </div>
                }
              } @else if (data.reglasActivadas.length) {
                @for (rule of data.reglasActivadas; track rule) {
                  <div class="rule-row">
                    <div class="rule-code">•</div>
                    <div class="rule-copy">
                      <strong>{{ rule }}</strong>
                      <span>El API no incluyó el detalle de la regla.</span>
                    </div>
                  </div>
                }
              } @else {
                <div class="empty-inline">No se activaron reglas de penalización.</div>
              }
            </article>

            <article class="panel sanctions-panel">
              <div class="panel-title">
                <div>
                  <h2>Sanciones registradas</h2>
                  <p>Detalle recibido del API de consulta</p>
                </div>
                <span class="count-badge">{{ sanctions(data).length }}</span>
              </div>
              @if (sanctions(data).length) {
                @for (sancion of sanctions(data); track sancion.idSancion + $index) {
                  <div class="sanction-row">
                    <div>
                      <strong>{{ sancion.motivo || 'Motivo no especificado' }}</strong>
                      <span>
                        {{ sancion.entidad || 'Entidad no especificada' }}
                        @if (sancion.fechaSancion) {
                          · {{ sancion.fechaSancion | date:'d MMM y' }}
                        }
                      </span>
                      @if (sancion.idSancion) {
                        <small>Resolución {{ sancion.idSancion }}</small>
                      }
                    </div>
                    <b>{{ sanctionAmount(sancion.montoMultaUit) }}</b>
                  </div>
                }
              } @else {
                <div class="empty-inline">El API no reporta sanciones en el detalle de esta consulta.</div>
              }
            </article>
          </div>

          <div class="result-side-column">
            <article class="panel facts-panel">
              <h2>Datos del contribuyente</h2>
              <div class="facts-grid">
                <div class="fact">
                  <span>Estado</span>
                  <b [class.status-good]="normalize(data.estado) === 'ACTIVO'">{{ data.estado || 'No disponible' }}</b>
                </div>
                <div class="fact">
                  <span>Condición</span>
                  <b [class.status-good]="normalize(data.condicion) === 'HABIDO'" [class.status-bad]="normalize(data.condicion) === 'NO HABIDO'">
                    {{ data.condicion || 'No disponible' }}
                  </b>
                </div>
                <div class="fact">
                  <span>Inicio de actividades</span>
                  <b>{{ data.fechaInicioActividades ? (data.fechaInicioActividades | date:'d MMM y') : 'No disponible' }}</b>
                </div>
                <div class="fact">
                  <span>Inhabilitación vigente</span>
                  <b [class.status-bad]="data.inhabilitado">{{ data.inhabilitado ? 'Sí' : 'No' }}</b>
                </div>
                <div class="fact">
                  <span>Identificador</span>
                  <b>{{ data.ruc }}</b>
                </div>
                <div class="fact">
                  <span>Actualización</span>
                  <b>{{ data.fechaConsulta ? (data.fechaConsulta | date:'d MMM y') : 'No disponible' }}</b>
                </div>
              </div>
            </article>

            <article class="panel source-panel">
              <span class="source-icon">✓</span>
              <div>
                <b>Consulta procesada por Score Perú</b>
                <p>El score, las reglas y el nivel de riesgo corresponden a la respuesta del servicio de negocio.</p>
              </div>
            </article>
          </div>
        </div>

        <p class="disclaimer">
          Este puntaje es informativo y se basa en los datos devueltos por el servicio. No constituye una calificación crediticia ni reemplaza la verificación directa ante las entidades oficiales.
        </p>
      </section>
      }
    }
  `,
})
export class ReportPrintComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly scoreService = inject(ScoreService);
  private paramSub?: Subscription;
  private currentRuc = '';

  readonly report = this.scoreService.currentReport;
  readonly loading = signal(false);
  readonly loadError = signal('');

  readonly initials = companyInitials;
  readonly riskClass = riskClass;
  readonly riskLabel = riskLabel;
  readonly normalize = normalizeText;
  readonly clampScore = clampScore;
  readonly gaugeAngle = gaugeAngle;
  readonly gaugeColor = gaugeColor;
  readonly scoreHeading = scoreHeading;
  readonly cacheLimited = isCacheLimited;
  readonly sanctionAmount = formatSanctionAmount;

  ngOnInit(): void {
    this.paramSub = this.route.paramMap.subscribe(params => {
      const ruc = (params.get('ruc') ?? '').trim();
      this.currentRuc = ruc;
      if (!ruc) {
        void this.router.navigate(['/']);
        return;
      }
      if (this.report()?.ruc === ruc) {
        this.schedulePrint();
        return;
      }
      this.fetchReport(ruc);
    });
  }

  ngOnDestroy(): void {
    this.paramSub?.unsubscribe();
  }

  retry(): void {
    if (this.currentRuc) {
      this.fetchReport(this.currentRuc);
    } else {
      void this.router.navigate(['/']);
    }
  }

  rules(data: ReporteRiesgo) {
    return data.detalleReglas ?? [];
  }

  sanctions(data: ReporteRiesgo) {
    return data.sanciones ?? [];
  }

  private fetchReport(ruc: string): void {
    this.loading.set(true);
    this.loadError.set('');
    this.scoreService.consultarRuc(ruc).subscribe({
      next: () => {
        this.loading.set(false);
        this.schedulePrint();
      },
      error: error => {
        this.loading.set(false);
        this.loadError.set(extractApiErrorMessage(error));
      },
    });
  }

  private schedulePrint(): void {
    setTimeout(() => window.print(), 150);
  }
}
