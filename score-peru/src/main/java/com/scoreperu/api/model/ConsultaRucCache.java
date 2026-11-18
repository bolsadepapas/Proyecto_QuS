package com.scoreperu.api.model;

import com.scoreperu.api.dto.SancionDto;
import com.scoreperu.api.model.converter.SancionListConverter;
import com.scoreperu.api.model.converter.StringListConverter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consultas_ruc_cache", indexes = {
    @Index(name = "idx_consulta_cache_ruc", columnList = "ruc", unique = true)
})
public class ConsultaRucCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 255)
    private String razonSocial;

    @Column(nullable = false, length = 50)
    private String estado;

    @Column(nullable = false, length = 50)
    private String condicion;

    @Column(name = "puntaje_riesgo", nullable = false)
    private int puntajeRiesgo;

    @Column(name = "nivel_riesgo", nullable = false, length = 20)
    private String nivelRiesgo;

    @Convert(converter = StringListConverter.class)
    @Column(name = "reglas_activadas", length = 2000)
    private List<String> reglasActivadas = new ArrayList<>();

    @Convert(converter = SancionListConverter.class)
    @Column(name = "sanciones_json", length = 4000)
    private List<SancionDto> sanciones = new ArrayList<>();

    @Column(name = "fecha_inicio_actividades")
    private LocalDate fechaInicioActividades;

    @Column(name = "inhabilitado", nullable = false)
    private boolean inhabilitado;

    @Column(name = "fecha_consulta", nullable = false)
    private LocalDateTime fechaConsulta;

    public ConsultaRucCache() {
    }

    public ConsultaRucCache(String ruc, String razonSocial, String estado, String condicion,
                             int puntajeRiesgo, String nivelRiesgo, List<String> reglasActivadas,
                             LocalDateTime fechaConsulta) {
        this(ruc, razonSocial, estado, condicion, puntajeRiesgo, nivelRiesgo,
                reglasActivadas, List.of(), null, false, fechaConsulta);
    }

    public ConsultaRucCache(String ruc, String razonSocial, String estado, String condicion,
                             int puntajeRiesgo, String nivelRiesgo, List<String> reglasActivadas,
                             List<SancionDto> sanciones, LocalDate fechaInicioActividades,
                             boolean inhabilitado, LocalDateTime fechaConsulta) {
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.estado = estado;
        this.condicion = condicion;
        this.puntajeRiesgo = puntajeRiesgo;
        this.nivelRiesgo = nivelRiesgo;
        this.reglasActivadas = (reglasActivadas != null) ? reglasActivadas : new ArrayList<>();
        this.sanciones = (sanciones != null) ? sanciones : new ArrayList<>();
        this.fechaInicioActividades = fechaInicioActividades;
        this.inhabilitado = inhabilitado;
        this.fechaConsulta = fechaConsulta;
    }

    /**
     * Regla de Negocio RFO-02: Valida si la consulta almacenada ha superado la ventana de vigencia (TTL).
     * @param diasTtl Días máximos permitidos antes de expirar (6 días según RFO-02).
     * @return true si tiene 6 o más días de antigüedad, false si aún es válido.
     */
    public boolean isExpirado(int diasTtl) {
        if (this.fechaConsulta == null) {
            return true;
        }
        return ChronoUnit.DAYS.between(this.fechaConsulta, LocalDateTime.now()) >= diasTtl;
    }

    public void actualizar(String razonSocial, String estado, String condicion,
                           int puntajeRiesgo, String nivelRiesgo, List<String> reglasActivadas) {
        actualizar(razonSocial, estado, condicion, puntajeRiesgo, nivelRiesgo,
                reglasActivadas, List.of(), null, false);
    }

    public void actualizar(String razonSocial, String estado, String condicion,
                           int puntajeRiesgo, String nivelRiesgo, List<String> reglasActivadas,
                           List<SancionDto> sanciones, LocalDate fechaInicioActividades, boolean inhabilitado) {
        this.razonSocial = razonSocial;
        this.estado = estado;
        this.condicion = condicion;
        this.puntajeRiesgo = puntajeRiesgo;
        this.nivelRiesgo = nivelRiesgo;
        this.reglasActivadas = (reglasActivadas != null) ? reglasActivadas : new ArrayList<>();
        this.sanciones = (sanciones != null) ? sanciones : new ArrayList<>();
        this.fechaInicioActividades = fechaInicioActividades;
        this.inhabilitado = inhabilitado;
        this.fechaConsulta = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCondicion() {
        return condicion;
    }

    public void setCondicion(String condicion) {
        this.condicion = condicion;
    }

    public int getPuntajeRiesgo() {
        return puntajeRiesgo;
    }

    public void setPuntajeRiesgo(int puntajeRiesgo) {
        this.puntajeRiesgo = puntajeRiesgo;
    }

    public String getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(String nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public List<String> getReglasActivadas() {
        return reglasActivadas;
    }

    public void setReglasActivadas(List<String> reglasActivadas) {
        this.reglasActivadas = reglasActivadas;
    }

    public List<SancionDto> getSanciones() {
        return sanciones;
    }

    public void setSanciones(List<SancionDto> sanciones) {
        this.sanciones = sanciones;
    }

    public LocalDate getFechaInicioActividades() {
        return fechaInicioActividades;
    }

    public void setFechaInicioActividades(LocalDate fechaInicioActividades) {
        this.fechaInicioActividades = fechaInicioActividades;
    }

    public boolean isInhabilitado() {
        return inhabilitado;
    }

    public void setInhabilitado(boolean inhabilitado) {
        this.inhabilitado = inhabilitado;
    }

    public LocalDateTime getFechaConsulta() {
        return fechaConsulta;
    }

    public void setFechaConsulta(LocalDateTime fechaConsulta) {
        this.fechaConsulta = fechaConsulta;
    }
}
