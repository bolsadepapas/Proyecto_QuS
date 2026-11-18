import { Injectable } from '@angular/core';

export interface HistorialItem {
  ruc: string;
  razonSocial: string;
  nivelRiesgo: 'BAJO' | 'MEDIO' | 'ALTO';
  puntajeRiesgo: number;
  fecha: string;
}

@Injectable({ providedIn: 'root' })
export class HistorialService {
  private readonly storageKey = 'score-peru-historial';
  private readonly maxItems = 10;

  guardar(item: HistorialItem): void {
    const historial = this.obtener();
    const index = historial.findIndex(h => h.ruc === item.ruc);
    if (index !== -1) {
      historial.splice(index, 1);
    }
    historial.unshift(item);
    if (historial.length > this.maxItems) {
      historial.length = this.maxItems;
    }
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(historial));
    } catch {
      // El historial es una comodidad local y no debe interrumpir una consulta.
    }
  }

  obtener(): HistorialItem[] {
    let raw: string | null;
    try {
      raw = localStorage.getItem(this.storageKey);
    } catch {
      return [];
    }
    if (!raw) {
      return [];
    }
    try {
      return JSON.parse(raw) as HistorialItem[];
    } catch {
      return [];
    }
  }

  limpiar(): void {
    try {
      localStorage.removeItem(this.storageKey);
    } catch {
      // No afecta a la disponibilidad del reporte.
    }
  }
}

