# ADR-001: Rediseño del Motor de Scoring mediante el Patrón Strategy e Inyección de Dependencias

* **Estatus:** Aprobado e Implementado
* **Fecha:** Octubre 2026
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

## 6. Evidencias de Validación

### Ejecución de Pruebas Unitarias
Se validó la refactorización mediante la ejecución del suite de pruebas aisladas con Gradle (`./gradlew test`), obteniendo una tasa de éxito del 100%:

```text
> Task :test
com.scoreperu.api.service.ConsultaRucServiceTest > debeEvaluarRiesgoAltoCuandoContribuyenteNoEstaActivo() PASSED

BUILD SUCCESSFUL in 822ms
4 actionable tasks: 4 up-to-date