package com.scoreperu.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class ConsultaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/consulta/{ruc} con RUC par debe retornar score 100 y riesgo BAJO de forma asíncrona")
    void testGetRucParScore100RiesgoBajo() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/consulta/20123456786"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("20123456786"))
                .andExpect(jsonPath("$.puntajeRiesgo").value(100))
                .andExpect(jsonPath("$.nivelRiesgo").value("BAJO"))
                .andExpect(jsonPath("$.reglasActivadas", empty()));
    }

    @Test
    @DisplayName("GET /api/v1/consulta/{ruc} con RUC impar debe descontar puntos por sanción de forma asíncrona")
    void testGetRucImparConSancion() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/consulta/10123456781"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("10123456781"))
                .andExpect(jsonPath("$.puntajeRiesgo").value(90))
                .andExpect(jsonPath("$.nivelRiesgo").value("BAJO"))
                .andExpect(jsonPath("$.reglasActivadas", hasItem(containsString("R03"))));
    }

    @Test
    @DisplayName("POST /api/v1/consulta con RUC válido debe procesar correctamente de forma asíncrona")
    void testPostRucValido() throws Exception {
        String jsonRequest = "{\"ruc\":\"20600012348\"}";

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("20600012348"))
                .andExpect(jsonPath("$.puntajeRiesgo").value(100))
                .andExpect(jsonPath("$.nivelRiesgo").value("BAJO"));
    }

    @Test
    @DisplayName("GET /api/v1/consulta/{ruc} con RUC inválido (<11 dígitos) debe retornar 400 Bad Request")
    void testGetRucInvalidoLongitud() throws Exception {
        mockMvc.perform(get("/api/v1/consulta/1012345"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje", containsString("exactamente 11 dígitos")));
    }

    @Test
    @DisplayName("GET /api/v1/consulta/{ruc} con prefijo inválido (ej. 30) o Módulo 11 inválido debe retornar 400 Bad Request")
    void testGetRucInvalidoPrefijoOModulo11() throws Exception {
        mockMvc.perform(get("/api/v1/consulta/30123456789"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje", containsString("iniciar con los dígitos '10', '15', '17' o '20'")));

        mockMvc.perform(get("/api/v1/consulta/20100190795"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje", containsString("Módulo 11")));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/stats debe retornar métricas agregadas del dashboard")
    void testGetDashboardStats() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmpresasAnalizadas", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.distribucionRiesgo").exists());
    }

    @Test
    @DisplayName("GET /api/v1/health debe retornar estado UP")
    void testGetHealth() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UP"))
                .andExpect(jsonPath("$.puerto").value(8080));
    }
}
