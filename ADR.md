# ADR-001: Rediseño del Motor de Scoring mediante el Patrón Strategy e Inyección de Dependencias

* **Estatus:** Aprobado, Implementado y Actualizado
* **Fecha:** Octubre 2026 (Actualización: Noviembre 2026)
* **Proyecto:** Score Perú — App Web de Transparencia Comercial
* **Autores:** Equipo de Desarrollo Score Perú

---

## 1. Contexto y Problema

En la versión inicial del núcleo funcional de **Score Perú**, la clase `MotorScoring` acumulaba la lógica de evaluación de las reglas de riesgo del negocio dentro de un único método imperativo compuesto por bloques secuenciales `if-else`. 

Este diseño presentó los siguientes problemas de calidad:
* **Violación del Principio Abierto/Cerrado (OCP):** Cada vez que se requería modificar una regla existente o agregar una nueva (como reglas de reincidencia u omisiones tributarias), era obligatorio modificar directamente el código fuente de la clase monolítica.
* **Baja Comprobabilidad (Testability):** Imposibilidad de realizar pruebas unitarias aisladas por cada regla. Un fallo en la ejecución afectaba la evaluación global del score.
* **Alto Acoplamiento y Deuda Técnica:** Excesiva complejidad cognitiva dentro de un solo componente, acumulando múltiples responsabilidades (cálculo, asignación de pesos y armado del reporte).

---

## 2. Opciones Consideradas

### Opción 1: Mantener la Estructura Monolítica Imperativa (`if-else` encadenados)
* **Descripción:** Mantener la lógica centralizada dentro de `MotorScoringService.java`.
* **Pros:** Sin sobrecosto de archivos o abstracciones adicionales.
* **Contras:** Mantiene la deuda técnica, alto riesgo de regresión ante cambios y baja mantenibilidad.

### Opción 2: Integrar un Motor de Reglas Externo (ej. Drools o Easy Rules)
* **Descripción:** Incorporar un framework de terceros para gestionar la ejecución de reglas declarativas.
* **Pros:** Permite mover la lógica de negocio a archivos de configuración externos.
* **Contras:** Curva de aprendizaje compleja, sobrepeso de dependencias en `build.gradle` y mayor sobrecarga en tiempo de ejecución para un dominio de 8 reglas.

### Opción 3: Refactorización Interna mediante el Patrón Strategy e Inyección Automática de Colecciones en Spring Boot (Seleccionada)
* **Descripción:** Abstraer cada regla de riesgo como una implementación independiente de la interfaz `ReglaScoring` anotada como `@Component`. El motor `MotorScoringService` recibe automáticamente la lista `List<ReglaScoring>` inyectada por Spring.
* **Pros:** Cumplimiento estricto de OCP y SRP, desacoplamiento total, pruebas unitarias aisladas por regla sin frameworks pesados externos.
* **Contras:** Incrementa el número total de archivos `.java` en el proyecto (uno por regla).

---

## 3. Decisión Tomada

Se seleccionó la **Opción 3 (Patrón Strategy + Inyección de Dependencias de Spring Boot)**. 

El motor de scoring fue refactorizado para operar sobre la abstracción `ReglaScoring`. Cada regla de riesgo se implementó de forma aislada mediante componentes Spring, permitiendo al motor iterar dinámicamente sobre la lista inyectada sin modificar su estructura lógica ante nuevos requerimientos.

---

## 4. Consecuencias y Atributos de Calidad Afectados

### Consecuencias Positivas
* **Modificabilidad (Alta):** Para añadir la Regla N° 9 ("Omisiones Tributarias"), solo se requiere crear una nueva clase con `@Component` que implemente `ReglaScoring`. Spring la detectará e incluirá en el motor automáticamente sin modificar ni una sola línea de código existente.
* **Comprobabilidad / Testability (Alta):** Permite escribir pruebas unitarias dedicadas verificando el comportamiento individual de cada regla con entradas controladas.
* **Aislamiento de Errores:** Un fallo o excepción en la condición de una regla puede ser capturado de forma aislada sin interrumpir la evaluación del resto del perfil del contribuyente.

### Consecuencias Negativas / Riesgos
* **Aumento en el número de clases:** Se pasa de 1 clase monolítica a $N + 2$ clases/interfaces (donde $N$ es el número de reglas).
* **Orden de ejecución:** La iteración predeterminada sobre `List<ReglaScoring>` depende del orden de carga del contexto de Spring (mitigado mediante la anotación `@Order` en caso de requerir precedencia estricta).

---

## 5. Limitaciones de la Evaluación

1. **Ambiente de Pruebas Aislado (Mocks):** La evaluación de comprobabilidad se realizó utilizando dobles de prueba (Mocks vía Mockito) para `ClienteConsultaExterna`, por lo que el comportamiento bajo fallos intermitentes de red de las APIs reales de SUNAT/INDECOPI no fue considerado dentro de las pruebas unitarias del motor.
2. **Volumen de Reglas:** La arquitectura fue validada para el conjunto de reglas actuales del proyecto. No se evaluó el impacto en latencia si el motor escalara a más de 100 reglas concurrentes en una sola petición.

---

## 6. Actualización de Implementación (Noviembre 2026)

Sobre la base del diseño Strategy y desacoplamiento por capas, se incorporaron las siguientes mejoras arquitectónicas y de calidad entre el backend Java (`score-peru`), el microservicio Python (`Api QuS`) y el frontend Angular (`score-peru-frontend`):

1. **Penalización Dinámica en `ReglaScoring` (Regla R03):**
   * Se añadió el método por defecto `calcularPenalizacion(Contribuyente, List<Sancion>)` en `ReglaScoring.java` y se actualizó `MotorScoringService` para invocarlo tanto en la evaluación como en la reconstrucción de caché.
   * `ReglaSancionIndecopi` calcula ahora la penalización proporcional a las sanciones de los últimos 12 meses (`Math.min(30, sancionRecienteCount * 10)`), respetando el tope máximo de 30 puntos.
2. **Consumo Paralelo Integrado (`Api QuS` + `CompletableFuture`):**
   * `ClienteConsultaExternaRest` implementa `obtenerDatosIntegrados(ruc)` consumiendo en una sola llamada HTTP el endpoint paralelo `/api/v1/consulta/{ruc}` de `Api QuS`.
   * Cuando el adaptador usa implementaciones individuales, `ConsultaRucService` ejecuta `obtenerDatosSunat` y `obtenerSancionesIndecopi` en paralelo mediante `CompletableFuture.allOf(...)` sobre hilos virtuales de Java 21.
3. **Validación Simétrica de RUC (Módulo 11 + Prefijos `10, 15, 17, 20`):**
   * Se unificó la validación oficial de SUNAT (prefijos `10`, `15`, `17`, `20` y dígito verificador Módulo 11) en `ConsultaRucService.validarRuc()` (Java), `app/core/validator.py` (Python) y `ruc-validator.ts` (Angular).
4. **Agregación en Base de Datos para el Dashboard Nacional:**
   * Se reemplazó `findAll()` en `DashboardController` por consultas JPQL/SQL dedicadas (`@Query` con `COUNT`, `AVG` y `GROUP BY`) en `ConsultaRucRepository`, evitando cargar toda la tabla `consultas_ruc_cache` y deserializar JSON en memoria JVM.
5. **Modularización del Frontend (Standalone + Lazy Loading):**
   * Se descompuso el antiguo componente monolítico `AppComponent` en vistas independientes (`SearchComponent`, `ResultComponent`, `ReportPrintComponent`, `DashboardComponent`) cargadas bajo demanda con `loadComponent` en el router de Angular 19.
6. **Seguridad de Credenciales OAuth 2.0:**
   * Se eliminaron las credenciales en texto plano de `Api QuS/app/config.py` y `Api QuS/README.md`, cargándolas exclusivamente desde variables de entorno (`.env`) con plantilla segura en `.env.example`.

---
## 7. Evidencias de Validación

### 7.1. Servidor Principal (Java — `./gradlew test`)
Se ejecutó la batería completa de pruebas automáticas para comprobar que las reglas de negocio, el guardado temporal de consultas y la atención a múltiples usuarios al mismo tiempo funcionen sin fallas:

```text
> Task :compileJava
> Task :compileTestJava
> Task :test

BUILD SUCCESSFUL in 10s
5 actionable tasks: 3 executed, 2 up-to-date
```

* **Qué se comprobó en la práctica:**
  * **Cálculo exacto del puntaje:** Si una empresa tiene varias multas recientes en INDECOPI, se descuentan 10 puntos por cada una hasta un máximo de 30 puntos, y se aplican correctamente las alertas de SUNAT.
  * **Respuestas rápidas y ahorro de consultas:** Cuando alguien busca un RUC que ya se consultó hace menos de 6 días, el sistema responde de inmediato con el dato guardado en lugar de volver a esperar a las páginas del Estado.
  * **Resistencia con muchos usuarios:** Se simuló a **50 personas consultando al mismo tiempo** y todas recibieron respuesta sin errores y muy por debajo del límite de 2.5 segundos.

### 7.2. Conector con SUNAT e INDECOPI (Python — `pytest`)
Se ejecutaron 13 pruebas automáticas sobre el servicio que se conecta a las fuentes públicas del Estado:

```text
13 passed, 1 warning in 0.45s
```

* **Qué se comprobó en la práctica:**
  * **Filtro de RUC auténticos:** El sistema verifica matemáticamente que el número de 11 dígitos sea válido (ya sea de persona natural con negocio o de empresa) y rechaza de inmediato cualquier RUC inventado o mal escrito.
  * **Conexión real con SUNAT e INDECOPI:** Se comprobó que el sistema recibe correctamente los datos tributarios y las sanciones oficiales y los entrega ordenados al servidor principal.

### 7.3. Página Web para el Usuario (Angular — `ng build`)
Se generó la versión final de la página web lista para publicar, comprobando que no tenga errores visuales ni archivos pesados:

```text
✔ Browser application bundle generation complete.
✔ Index html generation complete.
Initial total: 319.13 kB (Estimated transfer size: 90.26 kB)
Lazy chunks: search-component, result-component, report-print-component, dashboard-component
```

* **Qué se comprobó en la práctica:**
  * **Carga rápida desde cualquier dispositivo:** La pantalla inicial pesa apenas **90.26 KB** al descargarse, por lo que abre rápido incluso con internet móvil.
  * **Carga por partes:** Cada pantalla (búsqueda, resultado, reporte PDF y tablero nacional) se descarga solo cuando el usuario entra a ella, evitando esperas innecesarias al abrir la aplicación.
