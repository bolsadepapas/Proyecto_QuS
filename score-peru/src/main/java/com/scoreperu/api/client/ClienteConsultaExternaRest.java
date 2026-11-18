package com.scoreperu.api.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Cliente REST real que se conecta a la micro-API 'Api QuS'
 * para obtener datos de SUNAT e INDECOPI, con contingencia automática
 * en caso de que el servicio externo no esté disponible.
 */
@Component
@ConditionalOnProperty(name = "app.cliente.externo.modo", havingValue = "rest")
@SuppressWarnings("null")
public class ClienteConsultaExternaRest implements ClienteConsultaExterna {

    private static final Logger log = LoggerFactory.getLogger(ClienteConsultaExternaRest.class);

    private final RestClient restClient;
    private final ClienteConsultaExternaSimulado fallbackSimulado = new ClienteConsultaExternaSimulado();

    public ClienteConsultaExternaRest(@Value("${app.cliente.externo.url-base:http://localhost:8000}") String baseUrl) {
        String safeBaseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "http://localhost:8000";
        log.info(">>> Conectando Score Perú con Api QuS en: {}", safeBaseUrl);
        this.restClient = RestClient.builder()
                .baseUrl(safeBaseUrl)
                .build();
    }

    @Override
    public DatosExternosRuc obtenerDatosIntegrados(String ruc) {
        log.info("Consultando endpoint paralelo integrado /api/v1/consulta/{} en Api QuS", ruc);
        try {
            ConsultaIntegradaDto dto = restClient.get()
                    .uri("/api/v1/consulta/{ruc}", ruc)
                    .retrieve()
                    .body(ConsultaIntegradaDto.class);

            if (dto == null || dto.contribuyente() == null) {
                log.warn("Respuesta integrada vacía de Api QuS para RUC: {}. Usando contingencia local.", ruc);
                return new DatosExternosRuc(
                        fallbackSimulado.obtenerDatosSunat(ruc),
                        fallbackSimulado.obtenerSancionesIndecopi(ruc)
                );
            }

            ContribuyenteIntegradoDto cDto = dto.contribuyente();
            LocalDate fechaInicio = parsearFecha(cDto.fechaInicioActividades());
            Contribuyente contribuyente = new Contribuyente(
                    cDto.ruc() != null ? cDto.ruc() : ruc,
                    cDto.razonSocial() != null ? cDto.razonSocial() : "CONTRIBUYENTE " + ruc,
                    cDto.estado() != null ? cDto.estado() : "ACTIVO",
                    cDto.condicion() != null ? cDto.condicion() : "HABIDO",
                    cDto.inhabilitado(),
                    fechaInicio != null ? fechaInicio : LocalDate.now().minusYears(2)
            );

            List<Sancion> sanciones = (dto.sanciones() == null) ? Collections.emptyList() :
                    dto.sanciones().stream().map(s -> new Sancion(
                            s.idSancion() != null ? s.idSancion() : "SANC-INDECOPI",
                            s.entidad() != null ? s.entidad() : "INDECOPI",
                            (s.motivo() != null && !s.motivo().isBlank()) ? s.motivo() : "Infracción administrativa",
                            parsearFecha(s.fechaSancion()) != null ? parsearFecha(s.fechaSancion()) : LocalDate.now(),
                            s.montoMultaUit()
                    )).toList();

            return new DatosExternosRuc(contribuyente, sanciones);
        } catch (Exception e) {
            log.warn("Fallo al consultar endpoint integrado en Api QuS para RUC {} ({}). Activando contingencia local.", ruc, e.getMessage());
            return new DatosExternosRuc(
                    fallbackSimulado.obtenerDatosSunat(ruc),
                    fallbackSimulado.obtenerSancionesIndecopi(ruc)
            );
        }
    }

    @Override
    public Contribuyente obtenerDatosSunat(String ruc) {
        return obtenerDatosIntegrados(ruc).contribuyente();
    }

    @Override
    public List<Sancion> obtenerSancionesIndecopi(String ruc) {
        return obtenerDatosIntegrados(ruc).sanciones();
    }

    private LocalDate parsearFecha(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return null;
        try {
            return LocalDate.parse(fechaStr.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return parsearFechaIso(fechaStr);
        }
    }

    private LocalDate parsearFechaIso(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return null;
        try {
            return LocalDate.parse(fechaStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConsultaIntegradaDto(
            String ruc,
            boolean es_valido,
            String tipo_contribuyente,
            ContribuyenteIntegradoDto contribuyente,
            List<SancionIntegradaDto> sanciones
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContribuyenteIntegradoDto(
            String ruc,
            String razonSocial,
            String estado,
            String condicion,
            boolean inhabilitado,
            String fechaInicioActividades
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SancionIntegradaDto(
            String idSancion,
            String entidad,
            String motivo,
            String fechaSancion,
            double montoMultaUit
    ) {}
}
