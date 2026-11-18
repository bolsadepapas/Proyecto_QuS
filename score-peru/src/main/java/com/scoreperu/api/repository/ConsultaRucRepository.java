package com.scoreperu.api.repository;

import com.scoreperu.api.model.ConsultaRucCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRucRepository extends JpaRepository<ConsultaRucCache, Long> {

    Optional<ConsultaRucCache> findByRuc(String ruc);

    boolean existsByRuc(String ruc);

    @Query("SELECT COUNT(c) FROM ConsultaRucCache c")
    long contarTotalEmpresas();

    @Query(value = "SELECT COUNT(*) FROM consultas_ruc_cache WHERE reglas_activadas IS NOT NULL AND TRIM(reglas_activadas) <> ''", nativeQuery = true)
    long contarEmpresasConHallazgos();

    @Query("SELECT COALESCE(AVG(c.puntajeRiesgo), 0.0) FROM ConsultaRucCache c")
    Double obtenerScorePromedio();

    @Query("SELECT c.nivelRiesgo, COUNT(c) FROM ConsultaRucCache c GROUP BY c.nivelRiesgo")
    List<Object[]> contarDistribucionPorNivelRiesgo();

    @Query(value = "SELECT reglas_activadas, COUNT(*) FROM consultas_ruc_cache WHERE reglas_activadas IS NOT NULL AND TRIM(reglas_activadas) <> '' GROUP BY reglas_activadas", nativeQuery = true)
    List<Object[]> contarFrecuenciaCombinacionesReglas();
}

