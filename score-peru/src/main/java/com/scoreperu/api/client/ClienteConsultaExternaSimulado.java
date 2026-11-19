package com.scoreperu.api.client;

import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.cliente.externo.modo", havingValue = "simulado", matchIfMissing = true)
public class ClienteConsultaExternaSimulado implements ClienteConsultaExterna {

    private static final Logger log = LoggerFactory.getLogger(ClienteConsultaExternaSimulado.class);

    private static final Map<String, Contribuyente> CATALOGO_CONTRIBUYENTES = Map.of(
            "20100190797", new Contribuyente("20100190797", "LECHE GLORIA SOCIEDAD ANONIMA - GLORIA S.A.", "ACTIVO", "HABIDO", false, LocalDate.of(1942, 5, 4)),
            "20100055237", new Contribuyente("20100055237", "ALICORP S.A.A.", "ACTIVO", "HABIDO", false, LocalDate.of(1956, 7, 16)),
            "20100043140", new Contribuyente("20100043140", "SCOTIABANK PERU S.A.A.", "ACTIVO", "HABIDO", false, LocalDate.of(1943, 5, 2)),
            "20100047218", new Contribuyente("20100047218", "BANCO DE CREDITO DEL PERU", "ACTIVO", "HABIDO", false, LocalDate.of(1889, 4, 9)),
            "20100053455", new Contribuyente("20100053455", "BANCO INTERNACIONAL DEL PERU S.A.A. - INTERBANK", "ACTIVO", "HABIDO", false, LocalDate.of(1897, 5, 1)),
            "20100130204", new Contribuyente("20100130204", "BANCO BBVA PERU", "ACTIVO", "HABIDO", false, LocalDate.of(1951, 10, 1)),
            "20100030595", new Contribuyente("20100030595", "BANCO DE LA NACION", "ACTIVO", "HABIDO", false, LocalDate.of(1966, 1, 27)),
            "20100017491", new Contribuyente("20100017491", "TELEFONICA DEL PERU S.A.A.", "ACTIVO", "HABIDO", false, LocalDate.of(1920, 6, 25)),
            "20555666978", new Contribuyente("20555666978", "INVERSIONES COMERCIALES ANDINAS S.A.C.", "SUSPENSION TEMPORAL", "HABIDO", false, LocalDate.of(2018, 3, 12)),
            "20999888781", new Contribuyente("20999888781", "CONSTRUCTORA E INMOBILIARIA DEL SUR S.R.L.", "BAJA DEFINITIVA", "NO HABIDO", true, LocalDate.of(2015, 8, 20))
    );

    @Value("${app.cliente.externo.latencia-ms:300}")
    private long latenciaMs = 300;

    @Override
    public Contribuyente obtenerDatosSunat(String ruc) {
        simularLatenciaRedExterna();

        if (CATALOGO_CONTRIBUYENTES.containsKey(ruc)) {
            return CATALOGO_CONTRIBUYENTES.get(ruc);
        }

        String razonSocial = ruc.startsWith("20")
                ? "CORPORACION PERUANA DE SERVICIOS S.A.C."
                : "JUAN PEREZ ROJAS";
        String estado = "ACTIVO";
        String condicion = "HABIDO";
        return new Contribuyente(ruc, razonSocial, estado, condicion);
    }

    @Override
    public List<Sancion> obtenerSancionesIndecopi(String ruc) {
        if ("20100055237".equals(ruc) || "20555666978".equals(ruc)) {
            return Collections.emptyList();
        }

        if ("20100190797".equals(ruc)) {
            return List.of(new Sancion(
                    "2482-2024/CC2",
                    "INDECOPI",
                    "ELABORACION DE BEBIDAS - INFORMACION",
                    LocalDate.now().minusMonths(3),
                    3.0
            ));
        }

        if ("20100043140".equals(ruc)) {
            return List.of(
                    new Sancion("3142-2024/SPC-INDECOPI", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - OPERACIONES NO RECONOCIDAS / FALTA DE MEDIDAS DE SEGURIDAD", LocalDate.now().minusMonths(1), 18.5),
                    new Sancion("2890-2024/CC1", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - COBROS INDEBIDOS Y COMISIONES NO PACTADAS", LocalDate.now().minusMonths(2), 12.0),
                    new Sancion("2615-2024/CC2", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - FALTA DE IDONEIDAD EN BLOQUEO DE TARJETA DE CREDITO", LocalDate.now().minusMonths(3), 10.4),
                    new Sancion("2190-2024/CC1", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - FALTA DE ATENCION A RECLAMOS Y REQUERIMIENTOS DE INFORMACION", LocalDate.now().minusMonths(4), 8.6),
                    new Sancion("1844-2024/SPC-INDECOPI", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - REPORTE INDEBIDO ANTE CENTRALES DE RIESGO (CEP)", LocalDate.now().minusMonths(5), 7.5),
                    new Sancion("1520-2024/CC2", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - METODOS COMERCIALES COERCITIVOS Y COBRANZA ABUSIVA", LocalDate.now().minusMonths(6), 6.5),
                    new Sancion("1198-2024/CC1", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - INCUMPLIMIENTO DE MEDIDA CORRECTIVA", LocalDate.now().minusMonths(7), 5.0),
                    new Sancion("0985-2024/CC2", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - MODIFICACION UNILATERAL DE CONDICIONES CONTRACTUALES", LocalDate.now().minusMonths(8), 4.2),
                    new Sancion("0654-2024/CC1", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - NEGATIVA INJUSTIFICADA DE CONSTANCIA DE NO ADEUDO", LocalDate.now().minusMonths(9), 3.5),
                    new Sancion("0312-2024/CC2", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - DEFECTOS EN EL LIBRO DE RECLAMACIONES VIRTUAL", LocalDate.now().minusMonths(10), 2.8)
            );
        }

        if ("20100047218".equals(ruc) || "20100053455".equals(ruc) || "20100130204".equals(ruc) || "20100030595".equals(ruc)) {
            return List.of(
                    new Sancion("2945-2024/SPC-INDECOPI", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - OPERACIONES FRAUDULENTAS NO MONITOREADAS", LocalDate.now().minusMonths(1), 22.0),
                    new Sancion("2510-2024/CC1", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - CONSUMOS NO RECONOCIDOS EN BANCA MOVIL", LocalDate.now().minusMonths(2), 15.5),
                    new Sancion("2108-2024/CC2", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - FALTA DE INFORMACION EN TASAS Y SEGUROS", LocalDate.now().minusMonths(3), 14.0),
                    new Sancion("1670-2024/CC1", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - RETENCION INDEBIDA DE FONDOS", LocalDate.now().minusMonths(4), 9.8),
                    new Sancion("1205-2024/CC2", "INDECOPI", "SISTEMA FINANCIERO BANCARIO - ATENCION EXTEMPORANEA DE RECLAMOS", LocalDate.now().minusMonths(5), 6.2)
            );
        }

        if ("20999888781".equals(ruc)) {
            return List.of(
                    new Sancion("0891-2024/CC1", "INDECOPI", "CONSTRUCCION E INMOBILIARIOS - INCUMPLIMIENTO DE ENTREGA DE INMUEBLE Y DEFECTOS ESTRUCTURALES", LocalDate.now().minusMonths(1), 15.0),
                    new Sancion("1104-2024/SPC", "INDECOPI", "CONSTRUCCION E INMOBILIARIOS - CLAUSULAS ABUSIVAS Y NEGATIVA DE DEVOLUCION DE INICIAL", LocalDate.now().minusMonths(2), 12.5)
            );
        }

        int ultimoDigito = Character.getNumericValue(ruc.charAt(ruc.length() - 1));
        boolean esImpar = (ultimoDigito % 2 != 0);

        if (esImpar) {
            return List.of(new Sancion(
                    "SANC-2024-001",
                    "INDECOPI",
                    "Infracción al Código de Protección al Consumidor",
                    LocalDate.now().minusMonths(2),
                    2.5
            ));
        }
        return Collections.emptyList();
    }

    private void simularLatenciaRedExterna() {
        if (latenciaMs <= 0) {
            return;
        }
        try {
            log.info("Simulando latencia de red externa SUNAT/INDECOPI ({} ms)...", latenciaMs);
            Thread.sleep(latenciaMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Simulación de latencia interrumpida", e);
        }
    }
}
