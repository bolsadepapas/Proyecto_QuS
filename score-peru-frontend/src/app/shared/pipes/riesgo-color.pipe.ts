import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'riesgoColor',
  standalone: true,
})
export class RiesgoColorPipe implements PipeTransform {
  transform(nivel: 'BAJO' | 'MEDIO' | 'ALTO' | string): string {
    switch (nivel) {
      case 'BAJO':
        return 'var(--green)';
      case 'MEDIO':
        return 'var(--amber)';
      case 'ALTO':
        return 'var(--red)';
      default:
        return 'var(--green)';
    }
  }
}

@Pipe({
  name: 'riesgoClass',
  standalone: true,
})
export class RiesgoClassPipe implements PipeTransform {
  transform(nivel: 'BAJO' | 'MEDIO' | 'ALTO' | string): string {
    switch (nivel) {
      case 'BAJO':
        return 'low';
      case 'MEDIO':
        return 'mid';
      case 'ALTO':
        return 'high';
      default:
        return 'low';
    }
  }
}

