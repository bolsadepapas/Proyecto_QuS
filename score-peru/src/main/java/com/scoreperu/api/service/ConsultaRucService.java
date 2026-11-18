package com.scoreperu.api.service;

import com.scoreperu.api.client.ClienteConsultaExterna;
import com.scoreperu.api.dto.ReglaActivadaDto;
import com.scoreperu.api.dto.ResponseReporteRiesgo;
import com.scoreperu.api.dto.SancionDto;
import com.scoreperu.api.exception.RucInvalidoException;
import com.scoreperu.api.model.ConsultaRucCache;
import com.scoreperu.api.model.Contribuyente;
import com.scoreperu.api.model.Sancion;
import com.scoreperu.api.repository.ConsultaRucRepository;
import com.scoreperu.api.scoring.MotorScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@SuppressWarnings("null")
public class ConsultaRucService {

    private static final Logger log = LoggerFactory.getLogger(ConsultaRucService.class);

    private final ClienteConsultaExterna clienteConsultaExterna;
    private final MotorScoringService motorScoringService;
    private final ConsultaRucRepository consultaRucRepository;
    private final Executor taskExecutor;

    @Value("${app.cache.ttl-dias:6}")
    private int ttlDias;

    public ConsultaRucService(ClienteConsultaExterna clienteConsultaExterna,
                              MotorScoringService motorScoringService,
                              ConsultaRucRepository consultaRucRepository,
                              @Qualifier("taskExecutor") Executor taskExecutor) {
        this.clienteConsultaExterna = clienteConsultaExterna;
        this.motorScoringService = motorScoringService;
        this.consultaRucRepository = consultaRucRepository;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Consulta el scoring de un RUC evaluando la persistencia en caché (RFO-02)
     * y retornando de forma asíncrona no bloqueante (RNF-04).
     */
    public CompletableFuture<ResponseReporteRiesgo> consultarScoringRucAsync(String ruc) {
        validarRuc(ruc);
        String rucLimpio = ruc.trim();

        // 1. Verificación inicial en Caché
        Optional<ConsultaRucCache> cacheOpt = consultaRucRepository.findByRuc(rucLimpio);

        if (cacheOpt.isPresent() && !cacheOpt.get().isExpirado(ttlDias)) {
            // CACHE HIT: RNF-03 cumplido (< 2.5s, tiempo real ~15ms)
            ConsultaRucCache cache = cacheOpt.get();
            log.info(">>> CACHE HIT para RUC {} (Registrado: {})", rucLimpio, cache.getFechaConsulta());

            List<String> reglas = cache.getReglasActivadas() != null ? cache.getReglasActivadas() : List.of();
            List<SancionDto> sanciones = cache.getSanciones() != null ? cache.getSanciones() : List.of();
            List<ReglaActivadaDto> detalleReglas = motorScoringService.reconstruirDetalleReglas(reglas, sanciones);

            ResponseReporteRiesgo response = new ResponseReporteRiesgo(
                    cache.getRuc(),
                    cache.getRazonSocial(),
                    cache.getEstado(),
                    cache.getCondicion(),
                    cache.getPuntajeRiesgo(),
                    cache.getNivelRiesgo(),
                    reglas,
                    detalleReglas,
                    sanciones,
                    cache.getFechaInicioActividades(),
                    cache.isInhabilitado(),
                    cache.getFechaConsulta()
            );
            return CompletableFuture.completedFuture(response);
        }

        // 2. CACHE MISS o REGISTRO EXPIRADO (> 6 días)
        log.warn(">>> CACHE MISS / EXPIRADO para RUC {}. Despachando consulta asíncrona...", rucLimpio);

        return CompletableFuture.supplyAsync(
                () -> procesarConsultaExternaYPersistir(rucLimpio, cacheOpt.orElse(null)),
                taskExecutor
        );
    }

    public ResponseReporteRiesgo procesarConsultaExternaYPersistir(String ruc, ConsultaRucCache cacheExistente) {
        // Invocación a clientes externos: usa el endpoint paralelo integrado (/api/v1/consulta/{ruc})
        // si el adaptador lo soporta en una sola llamada HTTP, o paraleliza ambas consultas con CompletableFuture.allOf()
        ClienteConsultaExterna.DatosExternosRuc datosIntegrados = clienteConsultaExterna.obtenerDatosIntegrados(ruc);
        Contribuyente contribuyente;
        List<Sancion> sanciones;

        if (datosIntegrados != null && datosIntegrados.contribuyente() != null) {
            contribuyente = datosIntegrados.contribuyente();
            sanciones = datosIntegrados.sanciones() != null ? datosIntegrados.sanciones() : List.of();
        } else {
            CompletableFuture<Contribuyente> futuroSunat = CompletableFuture.supplyAsync(
                    () -> clienteConsultaExterna.obtenerDatosSunat(ruc), taskExecutor);
            CompletableFuture<List<Sancion>> futuroIndecopi = CompletableFuture.supplyAsync(
                    () -> clienteConsultaExterna.obtenerSancionesIndecopi(ruc), taskExecutor);
            CompletableFuture.allOf(futuroSunat, futuroIndecopi).join();
            contribuyente = futuroSunat.join();
            sanciones = futuroIndecopi.join();
        }

        // Evaluación dinámica mediante el Motor de Scoring (Strategy Pattern)
        MotorScoringService.ResultadoScoring resultadoScoring = motorScoringService.evaluar(contribuyente, sanciones);

        // Mapear sanciones de dominio a DTOs para el frontend y persistencia en caché
        List<SancionDto> sancionesDto = sanciones.stream()
                .map(s -> new SancionDto(
                        s.idSancion(),
                        s.entidad(),
                        s.motivo(),
                        s.fechaSancion(),
                        s.montoMultaUit()
                ))
                .toList();

        LocalDateTime ahora = LocalDateTime.now();

        // Guardado o actualización en la tabla consultas_ruc_cache
        if (cacheExistente != null) {
            cacheExistente.actualizar(
                    contribuyente.razonSocial(),
                    contribuyente.estado(),
                    contribuyente.condicion(),
                    resultadoScoring.scoreFinal(),
                    resultadoScoring.nivelRiesgo(),
                    resultadoScoring.reglasActivadas(),
                    sancionesDto,
                    contribuyente.fechaInicioActividades(),
                    contribuyente.inhabilitado()
            );
            consultaRucRepository.save(cacheExistente);
            log.info(">>> Registro de Caché ACTUALIZADO para RUC: {}", ruc);
        } else {
            ConsultaRucCache nuevoRegistro = new ConsultaRucCache(
                    contribuyente.ruc(),
                    contribuyente.razonSocial(),
                    contribuyente.estado(),
                    contribuyente.condicion(),
                    resultadoScoring.scoreFinal(),
                    resultadoScoring.nivelRiesgo(),
                    resultadoScoring.reglasActivadas(),
                    sancionesDto,
                    contribuyente.fechaInicioActividades(),
                    contribuyente.inhabilitado(),
                    ahora
            );
            consultaRucRepository.save(nuevoRegistro);
            log.info(">>> Nuevo Registro de Caché GUARDADO para RUC: {}", ruc);
        }

        return new ResponseReporteRiesgo(
                contribuyente.ruc(),
                contribuyente.razonSocial(),
                contribuyente.estado(),
                contribuyente.condicion(),
                resultadoScoring.scoreFinal(),
                resultadoScoring.nivelRiesgo(),
                resultadoScoring.reglasActivadas(),
                resultadoScoring.detalleReglas(),
                sancionesDto,
                contribuyente.fechaInicioActividades(),
                contribuyente.inhabilitado(),
                ahora
        );
    }

    /**
     * Versión sincrónica para compatibilidad retroactiva con clientes y tests existentes.
     */
    public ResponseReporteRiesgo consultarScoringRuc(String ruc) {
        return consultarScoringRucAsync(ruc).join();
    }

    public void setTtlDias(int ttlDias) {
        this.ttlDias = ttlDias;
    }

    private static final int[] PONDERADORES_MODULO_11 = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final List<String> PREFIJOS_VALIDOS_RUC = List.of("10", "15", "17", "20");

    private void validarRuc(String ruc) {
        if (ruc == null || ruc.trim().isEmpty()) {
            throw new RucInvalidoException("El RUC es obligatorio y no puede estar vacío.");
        }

        String rucLimpio = ruc.trim();
        if (rucLimpio.length() != 11 || !rucLimpio.matches("\\d{11}")) {
            throw new RucInvalidoException("El RUC debe tener exactamente 11 dígitos numéricos.");
        }

        String prefijo = rucLimpio.substring(0, 2);
        if (!PREFIJOS_VALIDOS_RUC.contains(prefijo)) {
            throw new RucInvalidoException("El RUC debe iniciar con los dígitos '10', '15', '17' o '20'.");
        }

        if (!verificarDigitoControlModulo11(rucLimpio)) {
            throw new RucInvalidoException("El dígito verificador del RUC es inválido según el algoritmo Módulo 11 de SUNAT.");
        }
    }

    public static boolean verificarDigitoControlModulo11(String ruc) {
        if (ruc == null || !ruc.matches("\\d{11}")) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            int digito = Character.getNumericValue(ruc.charAt(i));
            suma += digito * PONDERADORES_MODULO_11[i];
        }
        int resto = suma % 11;
        int complemento = 11 - resto;
        int digitoEsperado;
        if (complemento == 10) {
            digitoEsperado = 0;
        } else if (complemento == 11) {
            digitoEsperado = 1;
        } else {
            digitoEsperado = complemento;
        }
        int digitoVerificador = Character.getNumericValue(ruc.charAt(10));
        return digitoVerificador == digitoEsperado;
    }
}
