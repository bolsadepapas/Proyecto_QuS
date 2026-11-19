package com.scoreperu.api.service;

import com.scoreperu.api.client.ClienteConsultaExterna;
import com.scoreperu.api.dto.ResponseReporteRiesgo;
import com.scoreperu.api.exception.RucInvalidoException;
import com.scoreperu.api.model.ConsultaRucCache;
import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.repository.ConsultaRucRepository;
import com.scoreperu.api.scoring.MotorScoringService;
import com.scoreperu.api.scoring.ReglaScoring;
import com.scoreperu.api.scoring.rules.ReglaCondicionDomicilio;
import com.scoreperu.api.scoring.rules.ReglaEstadoNoActivo;
import com.scoreperu.api.scoring.rules.ReglaInhabilitacionVigente;
import com.scoreperu.api.scoring.rules.ReglaMultaSeveraIndecopi;
import com.scoreperu.api.scoring.rules.ReglaReincidenciaIndecopi;
import com.scoreperu.api.scoring.rules.ReglaSancionIndecopi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ConsultaRucServiceTest {

    @Mock
    private ClienteConsultaExterna clienteConsultaExterna;

    @Mock
    private ConsultaRucRepository consultaRucRepository;

    private final Executor directExecutor = Runnable::run;

    private ConsultaRucService consultaRucService;

    @BeforeEach
    void setUp() {
        List<ReglaScoring> reglas = List.of(
                new ReglaEstadoNoActivo(),
                new ReglaCondicionDomicilio(),
                new ReglaSancionIndecopi(),
                new ReglaMultaSeveraIndecopi(),
                new ReglaReincidenciaIndecopi(),
                new ReglaInhabilitacionVigente());
        MotorScoringService motorScoringService = new MotorScoringService(reglas);
        consultaRucService = new ConsultaRucService(
                clienteConsultaExterna,
                motorScoringService,
                consultaRucRepository,
                directExecutor);
        consultaRucService.setTtlDias(6);
    }

    @Test
    @DisplayName("RFO-02: Retorna datos de caché cuando existe y tiene menos de 6 días (CACHE HIT)")
    void testCacheHitNoInvocaClienteExterno() {
        String ruc = "20123456786";
        ConsultaRucCache cacheValido = new ConsultaRucCache(
                ruc,
                "EMPRESA CACHEADA S.A.C.",
                "ACTIVO",
                "HABIDO",
                100,
                "BAJO",
                List.of(),
                LocalDateTime.now().minusDays(2) // 2 días < 6 días TTL
        );

        when(consultaRucRepository.findByRuc(ruc)).thenReturn(Optional.of(cacheValido));

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(ruc, resultado.ruc());
        assertEquals("EMPRESA CACHEADA S.A.C.", resultado.razonSocial());
        assertEquals(100, resultado.puntajeRiesgo());
        assertEquals("BAJO", resultado.nivelRiesgo());
        assertNotNull(resultado.fechaConsulta());

        // Asegurar que NO se invoca a las APIs externas
        verify(clienteConsultaExterna, never()).obtenerDatosIntegrados(any());
        verify(clienteConsultaExterna, never()).obtenerDatosSunat(any());
        verify(clienteConsultaExterna, never()).obtenerSancionesIndecopi(any());
        verify(consultaRucRepository, never()).save(any());
    }

    @Test
    @DisplayName("RFO-02: Invoca cliente externo y guarda en DB cuando no existe en caché (CACHE MISS)")
    void testCacheMissInvocaClienteExternoYGuardaEnDB() {
        String ruc = "20123456786";
        Contribuyente mockContribuyente = new Contribuyente(ruc, "EMPRESA EXCELENTE S.A.C.", "ACTIVO", "HABIDO");

        when(consultaRucRepository.findByRuc(ruc)).thenReturn(Optional.empty());
        when(clienteConsultaExterna.obtenerDatosSunat(ruc)).thenReturn(mockContribuyente);
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc)).thenReturn(Collections.emptyList());

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(100, resultado.puntajeRiesgo());
        assertEquals("BAJO", resultado.nivelRiesgo());

        verify(clienteConsultaExterna, times(1)).obtenerDatosSunat(ruc);
        verify(clienteConsultaExterna, times(1)).obtenerSancionesIndecopi(ruc);

        ArgumentCaptor<ConsultaRucCache> captor = ArgumentCaptor.forClass(ConsultaRucCache.class);
        verify(consultaRucRepository, times(1)).save(captor.capture());
        assertEquals(ruc, captor.getValue().getRuc());
        assertEquals(100, captor.getValue().getPuntajeRiesgo());
    }

    @Test
    @DisplayName("Consume directamente obtenerDatosIntegrados (/api/v1/consulta/{ruc}) en una sola llamada cuando está disponible")
    void testUsaEndpointParaleloIntegradoEnUnaSolaLlamada() {
        String ruc = "20100190797";
        Contribuyente mockContribuyente = new Contribuyente(ruc, "LECHE GLORIA S.A.", "ACTIVO", "HABIDO");
        List<Sancion> mockSanciones = List.of(
                new Sancion("2482-2024/CC2", "INDECOPI", "INFORMACION", LocalDate.now().minusMonths(2), 3.0)
        );

        when(consultaRucRepository.findByRuc(ruc)).thenReturn(Optional.empty());
        when(clienteConsultaExterna.obtenerDatosIntegrados(ruc))
                .thenReturn(new ClienteConsultaExterna.DatosExternosRuc(mockContribuyente, mockSanciones));

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(90, resultado.puntajeRiesgo());
        verify(clienteConsultaExterna, times(1)).obtenerDatosIntegrados(ruc);
        verify(clienteConsultaExterna, never()).obtenerDatosSunat(any());
        verify(clienteConsultaExterna, never()).obtenerSancionesIndecopi(any());
    }

    @Test
    @DisplayName("RFO-02: Refresca datos y actualiza DB cuando la caché superó los 6 días (CACHE EXPIRADO)")
    void testCacheExpiradoRefrescaYActualizaDB() {
        String ruc = "20123456786";
        ConsultaRucCache cacheViejo = new ConsultaRucCache(
                ruc,
                "EMPRESA DESACTUALIZADA S.A.C.",
                "ACTIVO",
                "HABIDO",
                100,
                "BAJO",
                List.of(),
                LocalDateTime.now().minusDays(8) // 8 días > 6 días TTL
        );

        Contribuyente mockContribuyente = new Contribuyente(ruc, "EMPRESA ACTUALIZADA S.A.C.", "ACTIVO", "HABIDO");

        when(consultaRucRepository.findByRuc(ruc)).thenReturn(Optional.of(cacheViejo));
        when(clienteConsultaExterna.obtenerDatosSunat(ruc)).thenReturn(mockContribuyente);
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc)).thenReturn(Collections.emptyList());

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals("EMPRESA ACTUALIZADA S.A.C.", resultado.razonSocial());

        verify(clienteConsultaExterna, times(1)).obtenerDatosSunat(ruc);
        verify(consultaRucRepository, times(1)).save(cacheViejo);
    }

    @Test
    @DisplayName("Empresa con sanción reciente descuenta 10 puntos (Score 90, Riesgo BAJO)")
    void testEmpresaConSancionRecienteDescuenta10() {
        String ruc = "10456789124";
        Contribuyente mockContribuyente = new Contribuyente(ruc, "JUAN PEREZ", "ACTIVO", "HABIDO");
        List<Sancion> mockSanciones = List.of(
                new Sancion("SANC-01", "INDECOPI", "Infracción Libro de Reclamaciones",
                        LocalDate.now().minusMonths(2)));

        when(consultaRucRepository.findByRuc(ruc)).thenReturn(Optional.empty());
        when(clienteConsultaExterna.obtenerDatosSunat(ruc)).thenReturn(mockContribuyente);
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc)).thenReturn(mockSanciones);

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(90, resultado.puntajeRiesgo());
        assertEquals("BAJO", resultado.nivelRiesgo());
        assertEquals(1, resultado.reglasActivadas().size());
    }

    @Test
    @DisplayName("Regla R03 acumula 10 puntos por cada sanción reciente hasta el tope máximo de 30 puntos")
    void testReglaR03AcumulaPorSancionConTope30() {
        String ruc = "20100190797";
        Contribuyente mockContribuyente = new Contribuyente(ruc, "EMPRESA REINCIDENTE S.A.", "ACTIVO", "HABIDO");
        List<Sancion> cuatroSancionesRecientes = List.of(
                new Sancion("SANC-01", "INDECOPI", "Infracción 1", LocalDate.now().minusMonths(1)),
                new Sancion("SANC-02", "INDECOPI", "Infracción 2", LocalDate.now().minusMonths(2)),
                new Sancion("SANC-03", "INDECOPI", "Infracción 3", LocalDate.now().minusMonths(3)),
                new Sancion("SANC-04", "INDECOPI", "Infracción 4", LocalDate.now().minusMonths(4))
        );

        when(consultaRucRepository.findByRuc(ruc)).thenReturn(Optional.empty());
        when(clienteConsultaExterna.obtenerDatosSunat(ruc)).thenReturn(mockContribuyente);
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc)).thenReturn(cuatroSancionesRecientes);

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(70, resultado.puntajeRiesgo());
        assertEquals("MEDIO", resultado.nivelRiesgo());
        assertEquals(30, resultado.detalleReglas().getFirst().puntosPenalizacion());
    }

    @Test
    @DisplayName("Empresa con estado NO ACTIVO descuenta 40 puntos (Score 60, Riesgo MEDIO)")
    void testEmpresaNoActivaDescuenta40() {
        String ruc = "20555666978";
        Contribuyente mockContribuyente = new Contribuyente(ruc, "EMPRESA SUSPENDIDA S.A.C.", "SUSPENDIDO", "HABIDO");

        when(consultaRucRepository.findByRuc(ruc)).thenReturn(Optional.empty());
        when(clienteConsultaExterna.obtenerDatosSunat(ruc)).thenReturn(mockContribuyente);
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc)).thenReturn(Collections.emptyList());

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(60, resultado.puntajeRiesgo());
        assertEquals("MEDIO", resultado.nivelRiesgo());
        assertEquals(1, resultado.reglasActivadas().size());
    }

    @Test
    @DisplayName("La peor de las empresas acumula penalizaciones y queda con 0 puntos (Riesgo ALTO)")
    void testPeorEmpresaScoreCeroRiesgoAlto() {
        String ruc = "20999888781";
        Contribuyente mockContribuyente = new Contribuyente(
                ruc, "EMPRESA DEFICIENTE S.R.L.", "BAJA DEFINITIVA", "NO HABIDO", true, LocalDate.now().minusYears(1));
        List<Sancion> mockSanciones = List.of(
                new Sancion("SANC-02", "INDECOPI", "Cláusula abusiva", LocalDate.now().minusMonths(1)));

        when(consultaRucRepository.findByRuc(ruc)).thenReturn(Optional.empty());
        when(clienteConsultaExterna.obtenerDatosSunat(ruc)).thenReturn(mockContribuyente);
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc)).thenReturn(mockSanciones);

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(0, resultado.puntajeRiesgo());
        assertEquals("ALTO", resultado.nivelRiesgo());
        assertEquals(4, resultado.reglasActivadas().size());
    }

    @Test
    @DisplayName("Debe permitir prefijos 15 y 17 con Módulo 11 válido y rechazar RUCs con formato o dígito verificador inválido")
    void testValidacionRucModulo11YPrefijos() {
        // Prefijos 15 y 17 con Módulo 11 válido deben ser admitidos
        String ruc15Valido = "15123456782";
        when(consultaRucRepository.findByRuc(ruc15Valido)).thenReturn(Optional.empty());
        when(clienteConsultaExterna.obtenerDatosSunat(ruc15Valido))
                .thenReturn(new Contribuyente(ruc15Valido, "PERSONA NATURAL 15", "ACTIVO", "HABIDO"));
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc15Valido)).thenReturn(Collections.emptyList());

        ResponseReporteRiesgo res15 = consultaRucService.consultarScoringRuc(ruc15Valido);
        assertEquals(100, res15.puntajeRiesgo());

        // Casos inválidos (longitud, prefijo no reconocido, Módulo 11 incorrecto)
        assertThrows(RucInvalidoException.class, () -> consultaRucService.consultarScoringRuc("12345"));
        assertThrows(RucInvalidoException.class, () -> consultaRucService.consultarScoringRuc("30100190797"));
        assertThrows(RucInvalidoException.class, () -> consultaRucService.consultarScoringRuc("20100190795")); // Módulo 11 inválido
        assertThrows(RucInvalidoException.class, () -> consultaRucService.consultarScoringRuc(""));
        assertThrows(RucInvalidoException.class, () -> consultaRucService.consultarScoringRuc(null));
    }

    @Test
    @DisplayName("Entidad bancaria con alta carga de sanciones y multas >50 UIT en INDECOPI (Scotiabank 20100043140) obtiene Riesgo ALTO (25 pts)")
    void testScotiabankAltaCargaSancionesYMultasObtieneRiesgoAlto() {
        String ruc = "20100043140";
        Contribuyente scotiabank = new Contribuyente(ruc, "SCOTIABANK PERU S.A.A.", "ACTIVO", "HABIDO", false, LocalDate.of(1943, 5, 2));
        List<Sancion> diezSancionesGraves = List.of(
                new Sancion("3142-2024/SPC", "INDECOPI", "Operaciones no reconocidas", LocalDate.of(2024, 11, 28), 18.5),
                new Sancion("2890-2024/CC1", "INDECOPI", "Cobros indebidos", LocalDate.of(2024, 10, 19), 12.0),
                new Sancion("2615-2024/CC2", "INDECOPI", "Falta de idoneidad", LocalDate.of(2024, 9, 14), 10.4),
                new Sancion("2190-2024/CC1", "INDECOPI", "Falta de atención a reclamos", LocalDate.of(2024, 8, 5), 8.6),
                new Sancion("1844-2024/SPC", "INDECOPI", "Reporte indebido CEP", LocalDate.of(2024, 7, 11), 7.5),
                new Sancion("1520-2024/CC2", "INDECOPI", "Cobranza abusiva", LocalDate.of(2024, 6, 3), 6.5),
                new Sancion("1198-2024/CC1", "INDECOPI", "Incumplimiento medida correctiva", LocalDate.of(2024, 5, 17), 5.0),
                new Sancion("0985-2024/CC2", "INDECOPI", "Modificación contractual", LocalDate.of(2024, 4, 22), 4.2),
                new Sancion("0654-2024/CC1", "INDECOPI", "Constancia no adeudo", LocalDate.of(2024, 3, 10), 3.5),
                new Sancion("0312-2024/CC2", "INDECOPI", "Libro de reclamaciones", LocalDate.of(2024, 2, 8), 2.8)
        );

        when(consultaRucRepository.findByRuc(ruc)).thenReturn(Optional.empty());
        when(clienteConsultaExterna.obtenerDatosIntegrados(ruc))
                .thenReturn(new ClienteConsultaExterna.DatosExternosRuc(scotiabank, diezSancionesGraves));

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals("SCOTIABANK PERU S.A.A.", resultado.razonSocial());
        assertEquals(25, resultado.puntajeRiesgo()); // 100 - 30 (R03) - 25 (R04 >=50 UIT) - 20 (R05 >=10 sanciones)
        assertEquals("ALTO", resultado.nivelRiesgo());
        assertEquals(3, resultado.detalleReglas().size());
    }
}
