import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(),
    provideRouter([
      {
        path: '',
        loadComponent: () =>
          import('./app/pages/search/search.component').then(m => m.SearchComponent),
      },
      {
        path: 'resultado/:ruc',
        loadComponent: () =>
          import('./app/pages/result/result.component').then(m => m.ResultComponent),
      },
      {
        path: 'reporte/:ruc',
        loadComponent: () =>
          import('./app/pages/report-print/report-print.component').then(
            m => m.ReportPrintComponent,
          ),
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./app/pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      { path: '**', redirectTo: '' },
    ]),
  ],
}).catch(error => console.error(error));
