import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HistorialItem, HistorialService } from '../../core/services/historial.service';
import { ScoreService } from '../../core/services/score.service';
import {
  extractApiErrorMessage,
  riskClass,
  riskLabel,
  validarRucPeruano,
} from '../../core/utils/ruc-validator';

@Component({
  selector: 'score-search',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="content search-page">
      <div class="search-intro">
        <p class="eyebrow">Transparencia comercial</p>
        <h1>Conoce mejor a una empresa<br class="desktop-break"> antes de contratar.</h1>
        <p>Consulta información de SUNAT e INDECOPI y entiende qué factores influyen en el score.</p>
      </div>
      <form class="search-box" (ngSubmit)="consultar()" novalidate>
        <label class="sr-only" for="ruc">RUC de la empresa</label>
        <input
          id="ruc"
          name="ruc"
          [(ngModel)]="ruc"
          inputmode="numeric"
          maxlength="11"
          autocomplete="off"
          placeholder="Ingresa un RUC de 11 dígitos"
          [disabled]="loading()"
          (input)="searchError.set('')"
        >
        <button class="button button-primary" type="submit" [disabled]="loading()">
          {{ loading() ? 'Consultando…' : 'Consultar empresa' }}
        </button>
      </form>
      <p class="helper">
        Ingresa el RUC de 11 dígitos (inicia con 10, 15, 17 o 20 y cumple Módulo 11) · Ejemplos rápidos:
        <button type="button" class="text-button" (click)="quickSearch('20100190797')">20100190797 (Gloria)</button> ·
        <button type="button" class="text-button" (click)="quickSearch('20100055237')">20100055237 (Alicorp)</button> ·
        <button type="button" class="text-button" (click)="quickSearch('20555666978')">20555666978 (Riesgo Medio)</button> ·
        <button type="button" class="text-button" (click)="quickSearch('20999888781')">20999888781 (Riesgo Alto)</button>
      </p>
      @if (searchError()) {
        <div class="alert alert-error" role="alert">{{ searchError() }}</div>
      }
      @if (loading()) {
        <div class="loading">
          <span class="spinner"></span>
          <span>Consultando fuentes y calculando el score…</span>
        </div>
      }

      <section class="recent-section" id="history">
        <div class="section-heading">
          <div>
            <h2>Consultas recientes</h2>
            <p>Guardadas en este navegador</p>
          </div>
          @if (history().length) {
            <button class="text-button" (click)="clearHistory()">Limpiar historial</button>
          }
        </div>
        @if (history().length) {
          <div class="recent-grid">
            @for (item of history(); track item.ruc) {
              <button class="recent-card" (click)="openRecent(item)">
                <span class="ruc-label">RUC {{ item.ruc }}</span>
                <strong>{{ item.razonSocial || 'Contribuyente' }}</strong>
                <span class="risk-pill" [class]="'risk-pill ' + riskClass(item.nivelRiesgo)">
                  <i></i>{{ riskLabel(item.nivelRiesgo) }} · {{ item.puntajeRiesgo }} pts
                </span>
              </button>
            }
          </div>
        } @else {
          <div class="empty-state">Tus consultas aparecerán aquí después de buscar un RUC.</div>
        }
      </section>

      <div class="trust-grid">
        <div>
          <b>Fuentes identificadas</b>
          <span>La respuesta muestra la información disponible en el API integrado.</span>
        </div>
        <div>
          <b>Score explicable</b>
          <span>Revisa las reglas y penalizaciones devueltas por el motor de negocio.</span>
        </div>
        <div>
          <b>Uso informativo</b>
          <span>Contrasta los datos con las entidades oficiales antes de decidir.</span>
        </div>
      </div>
    </section>
  `,
})
export class SearchComponent {
  private readonly scoreService = inject(ScoreService);
  private readonly historyService = inject(HistorialService);
  private readonly router = inject(Router);

  ruc = '';
  readonly loading = signal(false);
  readonly searchError = signal('');
  readonly history = signal<HistorialItem[]>(this.historyService.obtener());

  readonly riskClass = riskClass;
  readonly riskLabel = riskLabel;

  consultar(): void {
    const validacion = validarRucPeruano(this.ruc);
    if (!validacion.valido) {
      this.searchError.set(validacion.mensaje);
      return;
    }

    const clean = validacion.rucLimpio;
    this.ruc = clean;
    this.searchError.set('');
    this.loading.set(true);

    this.scoreService.consultarRuc(clean).subscribe({
      next: () => {
        this.loading.set(false);
        this.history.set(this.historyService.obtener());
        void this.router.navigate(['/resultado', clean]);
      },
      error: error => {
        this.loading.set(false);
        this.searchError.set(extractApiErrorMessage(error));
      },
    });
  }

  quickSearch(sampleRuc: string): void {
    this.ruc = sampleRuc;
    this.consultar();
  }

  openRecent(item: HistorialItem): void {
    this.ruc = item.ruc;
    void this.router.navigate(['/resultado', item.ruc]);
  }

  clearHistory(): void {
    this.historyService.limpiar();
    this.history.set([]);
  }
}
