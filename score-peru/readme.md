# Documentación de Refactorización - Score Perú (Fase 2)

Este documento detalla el informe técnico de entrega correspondiente a la **Fase 2 (Mejora y Refactorización)** del backend del sistema **Score Perú**, desarrollado sobre Spring Boot 3 y Java 21.

---

##  Modelo de Reglas y Algoritmo de Scoring (RF02)
El algoritmo para **RF02** es un motor de reglas con puntaje ponderado (**scoring con base inicial de 100 puntos**):
- **Puntaje Inicial**: Todas las empresas inician con **100 pts** (Score Óptimo / Riesgo **BAJO**).
- **Penalizaciones (Descuento de Puntos)**: Cada regla evalúa una condición sobre los datos del contribuyente y, si se cumple, **descuenta puntos de score**.
- **Puntaje Final (0 a 100)**: `Score = max(100 - penalizaciones_acumuladas)`.
  * **0 puntos**: Representa la **peor de las empresas** (Riesgo **ALTO**).
  * **Score $\ge$ 80**: Nivel de Riesgo **BAJO**.
  * **50 $\le$ Score $<$ 80**: Nivel de Riesgo **MEDIO**.
  * **Score $<$ 50**: Nivel de Riesgo **ALTO**.

---

## 1. Matriz de Priorización de Hallazgos de Diseño

| ID | Hallazgo de Diseño | Impacto | Esfuerzo | Prioridad | Justificación Técnica |
|---|---|---|---|---|---|
| **H1** | **Acoplamiento rígido de reglas en MotorScoring** | **Alto** | **Medio** | **Prioridad 1** | El cálculo de scoring dependía de sentencias condicionales (`if-else`) monolíticas. Cada nueva regla requería modificar la clase principal, violando Open/Closed (OCP) y elevando el riesgo de regresión. |
| **H2** | **Dependencia directa de APIs externas en ConsultaRucService sin interfaces** | **Alto** | **Bajo** | **Prioridad 2** | `ConsultaRucService` dependía fuertemente de integraciones directas sin abstracción. Esto impedía realizar pruebas unitarias aisladas sin conexión a red o terceros (SUNAT/INDECOPI), violando DIP. |
| **H3** | **Violación de SRP en entidad ReporteRiesgo al incluir generación de PDF** | **Medio** | **Bajo** | **Prioridad 3** | El modelo/DTO contenía lógica de renderizado y exportación de archivos binarios, mezclando el dominio de datos con la capa de presentación. |
| **H4** | **Acoplamiento de la estrategia de caché TTL en consultas SQL** | **Medio** | **Medio** | **Prioridad 4** | La lógica de expiración y persistencia temporal estaba acoplada a consultas en la base de datos en vez de abstracciones declarativas (`@Cacheable`, Redis/Caffeine). |
| **H5** | **Construcción manual de DTOs de error en Controllers** | **Bajo** | **Bajo** | **Prioridad 5** | Se instanciaban respuestas de error directamente en bloques `try-catch` dentro de cada endpoint en vez de centralizarlas en `@RestControllerAdvice`. |

---

## 2. Refactorización y Rediseño Aplicado

### Hallazgo 1 (H1): Aplicación del Patrón Strategy y Principio Open/Closed (OCP)
- **Problema previo (Antes)**: `MotorScoringService` evaluaba reglas mediante `if-else` cableados.
- **Solución aplicada (Después)**: Se implementó el patrón **Strategy** desacoplando cada regla en un `@Component` que implementa `ReglaScoring`. `MotorScoringService` recibe `List<ReglaScoring>` y delega la ejecución dinámica.
- **Beneficio OCP**: El sistema queda abierto para extensión (crear un nuevo `@Component implements ReglaScoring`) y cerrado a modificaciones en el core del motor.

**Interfaz de Estrategia (`ReglaScoring.java`):**
```java
package com.scoreperu.api.scoring;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import java.util.List;

public interface ReglaScoring {
    boolean aplica(Contribuyente contribuyente, List<Sancion> sanciones);
    int getPuntosPenalizacion();
    String getNombreRegla();
}
```

**Motor de Scoring Dinámico (`MotorScoringService.java`):**
```java
package com.scoreperu.api.scoring;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class MotorScoringService {

    public static final int SCORE_BASE_INICIAL = 100;
    private final List<ReglaScoring> reglas;

    public MotorScoringService(List<ReglaScoring> reglas) {
        this.reglas = (reglas != null) ? reglas : List.of();
    }

    public ResultadoScoring evaluar(Contribuyente contribuyente, List<Sancion> sanciones) {
        int totalPenalizaciones = 0;
        List<String> reglasActivadas = new ArrayList<>();

        for (ReglaScoring regla : reglas) {
            if (regla.aplica(contribuyente, sanciones)) {
                totalPenalizaciones += regla.getPuntosPenalizacion();
                reglasActivadas.add(regla.getNombreRegla());
            }
        }

        int scoreFinal = Math.max(0, Math.min(100, SCORE_BASE_INICIAL - totalPenalizaciones));
        String nivelRiesgo = determinarNivelRiesgo(scoreFinal);

        return new ResultadoScoring(scoreFinal, nivelRiesgo, reglasActivadas);
    }

    public String determinarNivelRiesgo(int score) {
        if (score >= 80) return "BAJO";
        if (score >= 50) return "MEDIO";
        return "ALTO";
    }

    public record ResultadoScoring(
        int scoreFinal,
        String nivelRiesgo,
        List<String> reglasActivadas
    ) {}
}
```

---

### Hallazgo 2 (H2): Inversión de Dependencias (DIP) y Testabilidad Aislada
- **Problema previo (Antes)**: Invocación directa de APIs de terceros en la lógica de negocio.
- **Solución aplicada (Después)**: Contrato `ClienteConsultaExterna` desacoplando el dominio.
- **Beneficio DIP**: Mocks rápidos y aislados con Mockito en pruebas unitarias.

**Interfaz de Abstracción Externa (`ClienteConsultaExterna.java`):**
```java
package com.scoreperu.api.client;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import java.util.List;

public interface ClienteConsultaExterna {
    Contribuyente obtenerDatosSunat(String ruc);
    List<Sancion> obtenerSancionesIndecopi(String ruc);
}
```

---

## 3. Comparativa "Antes y Después" mediante Escenario de Cambio

### Definición del Escenario de Cambio
**Requerimiento Normativo:** Incorporar la **Regla N° 9: "Omisiones Tributarias"** (-15 puntos de penalización si mantiene declaraciones omitidas ante SUNAT).

### Matriz Comparativa

| Criterio de Evaluación | Diseño Anterior (Monolítico if-else) | Diseño Refactorizado (Strategy + Componentes) |
|---|---|---|
| **Archivos modificados vs. creados** |  **1 archivo modificado:** Modificación intrusiva de `MotorScoringService.java`. | **1 archivo nuevo creado / 0 modificados:** Creación de `ReglaOmisionesTributarias.java` (`@Component implements ReglaScoring`). |
| **Cumplimiento de OCP** |La clase central debía ser reabierta y alterada. |  Abierto para extensión e inmutable en el motor. |
| **Riesgo de regresión** | Riesgo de alterar el orden de evaluación o cálculo de otras reglas. |  Reglas existentes aisladas e intactas. |
| **Impacto en pruebas unitarias** | Reescritura de tests unitarios de `MotorScoringServiceTest`. |  Creación exclusiva de `ReglaOmisionesTributariasTest.java`. |

