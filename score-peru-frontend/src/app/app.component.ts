import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { ScoreService } from './core/services/score.service';
import { downloadDashboardCsv } from './core/utils/ruc-validator';

@Component({
  selector: 'score-peru-app',
  standalone: true,
  imports: [RouterLink, RouterOutlet],
  template: `
    <div class="app-shell">
      <aside class="sidebar">
        <a class="brand" routerLink="/">
          <span class="brand-mark">SP</span>
          <span>Score Perú</span>
        </a>
        <nav aria-label="Navegación principal">
          <a routerLink="/" [class.active]="page() === 'search' || page() === 'result' || page() === 'report'">
            <span>⌕</span>Consultar empresa
          </a>
          <a routerLink="/dashboard" [class.active]="page() === 'dashboard'">
            <span>⌁</span>Dashboard nacional
          </a>
          <button type="button" class="nav-button" (click)="showHistory()">
            <span>◷</span>Consultas recientes
          </button>
          <button type="button" class="nav-button" (click)="aboutOpen.set(true)">
            <span>ⓘ</span>Sobre los datos
          </button>
        </nav>
        <div class="sidebar-note">Información pública para decisiones comerciales más informadas.</div>
      </aside>

      <main class="main-area">
        <header class="topbar">
          <div class="breadcrumb">
            {{
              page() === 'dashboard'
                ? 'Dashboard nacional'
                : page() === 'result'
                  ? 'Consulta / Resultado'
                  : page() === 'report'
                    ? 'Vista previa del reporte'
                    : 'Consulta comercial'
            }}
          </div>
          <div class="top-actions">
            @if (page() === 'result' || page() === 'report') {
              <button class="button button-quiet" (click)="newSearch()">Nueva consulta</button>
              <button class="button button-primary" (click)="printReport()">↓ Descargar PDF</button>
            }
            @if (page() === 'dashboard') {
              <button class="button button-quiet" (click)="exportDashboard()">↓ Exportar resumen</button>
            }
          </div>
        </header>

        <router-outlet />
      </main>
    </div>

    @if (aboutOpen()) {
      <div class="modal-backdrop" (click)="aboutOpen.set(false)" (keydown.escape)="aboutOpen.set(false)">
        <section
          class="about-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="about-title"
          (click)="$event.stopPropagation()"
        >
          <button class="modal-close" aria-label="Cerrar" (click)="aboutOpen.set(false)">×</button>
          <p class="eyebrow">Sobre Score Perú</p>
          <h2 id="about-title">Información para decidir con más contexto</h2>
          <p>
            Score Perú consolida los datos públicos que entrega el servicio integrado y presenta las reglas de scoring aplicadas por el backend.
          </p>
          <ul>
            <li>El puntaje va de 0 a 100 según el motor de negocio.</li>
            <li>Un nivel BAJO corresponde a 80–100, MEDIO a 50–79 y ALTO a menos de 50.</li>
            <li>Los datos pueden estar limitados cuando la respuesta proviene de caché.</li>
          </ul>
          <p>Verifica siempre la información ante SUNAT, INDECOPI u otra entidad competente.</p>
          <button class="button button-primary" (click)="aboutOpen.set(false)">Entendido</button>
        </section>
      </div>
    }
  `,
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly scoreService = inject(ScoreService);
  private readonly router = inject(Router);
  private subscription?: Subscription;

  readonly page = signal<'search' | 'result' | 'dashboard' | 'report'>('search');
  readonly aboutOpen = signal(false);

  ngOnInit(): void {
    this.subscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.syncRoute());
    this.syncRoute();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  newSearch(): void {
    this.scoreService.currentReport.set(null);
    void this.router.navigate(['/']);
  }

  printReport(): void {
    const ruc = this.scoreService.currentReport()?.ruc || this.routeRuc();
    if (!ruc) return;
    if (this.page() === 'report') {
      window.print();
      return;
    }
    void this.router.navigate(['/reporte', ruc]);
  }

  showHistory(): void {
    void this.router.navigate(['/']).then(() => {
      setTimeout(() => document.getElementById('history')?.scrollIntoView({ behavior: 'smooth' }), 100);
    });
  }

  exportDashboard(): void {
    const data = this.scoreService.currentDashboard();
    if (data) {
      downloadDashboardCsv(data);
      return;
    }
    this.scoreService.obtenerDashboardStats().subscribe({
      next: stats => downloadDashboardCsv(stats),
    });
  }

  private syncRoute(): void {
    const path = this.router.url;
    if (path.startsWith('/dashboard')) {
      this.page.set('dashboard');
      return;
    }
    if (path.startsWith('/reporte/')) {
      this.page.set('report');
      return;
    }
    if (path.startsWith('/resultado/')) {
      this.page.set('result');
      return;
    }
    this.page.set('search');
  }

  private routeRuc(): string {
    return this.router.url.split(/[?#]/)[0].split('/').filter(Boolean).at(-1) ?? '';
  }
}
