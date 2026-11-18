package com.scoreperu.api.concurrency;

import com.scoreperu.api.dto.ResponseReporteRiesgo;
import com.scoreperu.api.service.ConsultaRucService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@SuppressWarnings("null")
public class ConsultaRucConcurrenciaTest {

    private static final Logger log = LoggerFactory.getLogger(ConsultaRucConcurrenciaTest.class);

    @Autowired
    private ConsultaRucService consultaRucService;

    @Test
    @DisplayName("Prueba de Rendimiento No Funcional: 50 hilos concurrentes (Cache Miss vs Cache Hit)")
    void testConcurrencia50HilosCacheRuc() throws Exception {
        final int TOTAL_HILOS = 50;
        final String RUC_TEST = "20123456786";

        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_HILOS);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(TOTAL_HILOS);

        List<Long> tiemposRespuesta = Collections.synchronizedList(new ArrayList<>());
        List<ResponseReporteRiesgo> respuestas = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errores = Collections.synchronizedList(new ArrayList<>());

        // 1. Petición inicial para calentar la caché (simulando estado frío o primera consulta)
        long tInicioFrio = System.currentTimeMillis();
        ResponseReporteRiesgo respuestaFria = consultaRucService.consultarScoringRuc(RUC_TEST);
        long tFinFrio = System.currentTimeMillis() - tInicioFrio;
        log.info(">>> PETICION 0 (ESTADO FRIO / CACHE MISS): {} ms - RUC: {}, Score: {}",
                tFinFrio, respuestaFria.ruc(), respuestaFria.puntajeRiesgo());

        // 2. Disparo de 50 peticiones simultáneas concurrentes (Cache Hit)
        for (int i = 0; i < TOTAL_HILOS; i++) {
            executor.submit(() -> {
                try {
                    startSignal.await(); // Espera el disparo sincronizado
                    long t0 = System.currentTimeMillis();
                    ResponseReporteRiesgo r = consultaRucService.consultarScoringRuc(RUC_TEST);
                    long latencia = System.currentTimeMillis() - t0;

                    tiemposRespuesta.add(latencia);
                    respuestas.add(r);
                } catch (Throwable e) {
                    errores.add(e);
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Señal de arranque simultáneo para los 50 hilos
        long tInicioTotal = System.currentTimeMillis();
        startSignal.countDown();

        boolean completado = doneSignal.await(10, TimeUnit.SECONDS);
        long tTotalTest = System.currentTimeMillis() - tInicioTotal;
        executor.shutdown();

        assertTrue(completado, "Todas las peticiones concurrentes debieron completar en menos de 10 segundos");
        assertTrue(errores.isEmpty(), "No deben presentarse errores de concurrencia: " + errores);
        assertEquals(TOTAL_HILOS, respuestas.size());

        // Cálculo de Métricas Estadísticas
        List<Long> tiemposOrdenados = tiemposRespuesta.stream().sorted().collect(Collectors.toList());
        long minLatency = tiemposOrdenados.getFirst();
        long maxLatency = tiemposOrdenados.getLast();
        double avgLatency = tiemposOrdenados.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p90 = tiemposOrdenados.get((int) (TOTAL_HILOS * 0.90) - 1);
        long p95 = tiemposOrdenados.get((int) (TOTAL_HILOS * 0.95) - 1);
        long p99 = tiemposOrdenados.get((int) (TOTAL_HILOS * 0.99) - 1);
        double throughput = (TOTAL_HILOS / (double) tTotalTest) * 1000.0;

        System.out.println("==========================================================================");
        System.out.println("  REPORTE DE PRUEBA DE RENDIMIENTO Y CONCURRENCIA - SCORE PERÚ (RNF-03/04)");
        System.out.println("==========================================================================");
        System.out.println(String.format("Petición en Frío (Cache Miss):        %d ms (Incluye sleep 3000ms)", tFinFrio));
        System.out.println(String.format("Hilos Concurrentes:                   %d", TOTAL_HILOS));
        System.out.println(String.format("Tiempo Total de Ejecución de Ráfaga:   %d ms", tTotalTest));
        System.out.println(String.format("Throughput Estimado:                  %.2f req/seg", throughput));
        System.out.println(String.format("Latencia Mínima (Cache Hit):          %d ms", minLatency));
        System.out.println(String.format("Latencia Máxima (Cache Hit):          %d ms", maxLatency));
        System.out.println(String.format("Latencia Media (Average):             %.2f ms", avgLatency));
        System.out.println(String.format("Percentil 90 (P90):                   %d ms", p90));
        System.out.println(String.format("Percentil 95 (P95):                   %d ms", p95));
        System.out.println(String.format("Percentil 99 (P99):                   %d ms", p99));
        System.out.println(String.format("Objetivo SLA (RNF-03 < 2500ms):       %s", (p95 < 2500 ? "CUMPLIDO CON ÉXITO" : "FALLIDO")));
        System.out.println("==========================================================================");

        // Verificación de Requisito No Funcional RNF-03 (< 2.5s)
        assertTrue(p95 < 2500, "El percentil 95 debe ser menor a 2.5 segundos (RNF-03)");
    }
}

