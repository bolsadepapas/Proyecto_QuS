package com.scoreperu.api.service;

import com.scoreperu.api.client.ClienteConsultaExterna;
import com.scoreperu.api.dto.ResponseReporteRiesgo;
import com.scoreperu.api.exception.RucInvalidoException;
import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.scoring.MotorScoringService;
import com.scoreperu.api.scoring.ReglaScoring;
import com.scoreperu.api.scoring.rules.ReglaCondicionDomicilio;
import com.scoreperu.api.scoring.rules.ReglaEstadoNoActivo;
import com.scoreperu.api.scoring.rules.ReglaInhabilitacionVigente;
import com.scoreperu.api.scoring.rules.ReglaSancionIndecopi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaRucServiceTest {

    @Mock
    private ClienteConsultaExterna clienteConsultaExterna;

    private ConsultaRucService consultaRucService;

    @BeforeEach
    void setUp() {
        List<ReglaScoring> reglas = List.of(
                new ReglaEstadoNoActivo(),
                new ReglaCondicionDomicilio(),
                new ReglaSancionIndecopi(),
                new ReglaInhabilitacionVigente()
        );
        MotorScoringService motorScoringService = new MotorScoringService(reglas);
        consultaRucService = new ConsultaRucService(clienteConsultaExterna, motorScoringService);
    }

    @Test
    @DisplayName("Empresa sin observaciones debe iniciar con 100 puntos y Nivel de Riesgo BAJO")
    void testEmpresaOptimaScore100RiesgoBajo() {
        String ruc = "20123456780";
        Contribuyente mockContribuyente = new Contribuyente(ruc, "EMPRESA EXCELENTE S.A.C.", "ACTIVO", "HABIDO");

        when(clienteConsultaExterna.obtenerDatosSunat(ruc)).thenReturn(mockContribuyente);
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc)).thenReturn(Collections.emptyList());

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(ruc, resultado.ruc());
        assertEquals(100, resultado.puntajeRiesgo());
        assertEquals("BAJO", resultado.nivelRiesgo());
        assertTrue(resultado.reglasActivadas().isEmpty());

        verify(clienteConsultaExterna, times(1)).obtenerDatosSunat(ruc);
        verify(clienteConsultaExterna, times(1)).obtenerSancionesIndecopi(ruc);
    }

    @Test
    @DisplayName("Empresa con sanción reciente descuenta 10 puntos (Score 90, Riesgo BAJO)")
    void testEmpresaConSancionRecienteDescuenta10() {
        String ruc = "10456789123";
        Contribuyente mockContribuyente = new Contribuyente(ruc, "JUAN PEREZ", "ACTIVO", "HABIDO");
        List<Sancion> mockSanciones = List.of(
                new Sancion("SANC-01", "INDECOPI", "Infracción Libro de Reclamaciones", LocalDate.now().minusMonths(2))
        );

        when(clienteConsultaExterna.obtenerDatosSunat(ruc)).thenReturn(mockContribuyente);
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc)).thenReturn(mockSanciones);

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(90, resultado.puntajeRiesgo());
        assertEquals("BAJO", resultado.nivelRiesgo());
        assertEquals(1, resultado.reglasActivadas().size());
    }

    @Test
    @DisplayName("Empresa con estado NO ACTIVO descuenta 40 puntos (Score 60, Riesgo MEDIO)")
    void testEmpresaNoActivaDescuenta40() {
        String ruc = "20999888776";
        Contribuyente mockContribuyente = new Contribuyente(ruc, "EMPRESA SUSPENDIDA S.A.C.", "SUSPENDIDO", "HABIDO");

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
        String ruc = "20999888771";
        // NO ACTIVO (-40), NO HABIDO (-25), Inhabilitado (-30)
        Contribuyente mockContribuyente = new Contribuyente(
                ruc, "EMPRESA DEFICIENTE S.R.L.", "BAJA DEFINITIVA", "NO HABIDO", true, LocalDate.now().minusYears(1)
        );
        // Sanción reciente (-10) -> Total penalizaciones = 105 -> Score clamped a 0
        List<Sancion> mockSanciones = List.of(
                new Sancion("SANC-02", "INDECOPI", "Cláusula abusiva", LocalDate.now().minusMonths(1))
        );

        when(clienteConsultaExterna.obtenerDatosSunat(ruc)).thenReturn(mockContribuyente);
        when(clienteConsultaExterna.obtenerSancionesIndecopi(ruc)).thenReturn(mockSanciones);

        ResponseReporteRiesgo resultado = consultaRucService.consultarScoringRuc(ruc);

        assertNotNull(resultado);
        assertEquals(0, resultado.puntajeRiesgo());
        assertEquals("ALTO", resultado.nivelRiesgo());
        assertEquals(4, resultado.reglasActivadas().size());
    }

    @Test
    @DisplayName("Debe lanzar RucInvalidoException si el RUC no cumple formato")
    void testValidacionRucInvalido() {
        assertThrows(RucInvalidoException.class, () -> consultaRucService.consultarScoringRuc("12345"));
        assertThrows(RucInvalidoException.class, () -> consultaRucService.consultarScoringRuc("15123456789"));
        assertThrows(RucInvalidoException.class, () -> consultaRucService.consultarScoringRuc(""));
        assertThrows(RucInvalidoException.class, () -> consultaRucService.consultarScoringRuc(null));

        verifyNoInteractions(clienteConsultaExterna);
    }
}
