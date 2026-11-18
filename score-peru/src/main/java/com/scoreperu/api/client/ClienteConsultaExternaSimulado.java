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
            "20100047218", new Contribuyente("20100047218", "BANCO DE CREDITO DEL PERU", "ACTIVO", "HABIDO", false, LocalDate.of(1889, 4, 9)),
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
        if ("20100190797".equals(ruc)) {
            return List.of(new Sancion(
                    "2482-2024/CC2",
                    "INDECOPI",
                    "ELABORACION DE BEBIDAS - INFORMACION",
                    LocalDate.now().minusMonths(3),
                    3.0
            ));
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
