package com.scoreperu.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
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
    @DisplayName("GET /api/v1/consulta/{ruc} con RUC par debe retornar score 100 y riesgo BAJO")
    void testGetRucParScore100RiesgoBajo() throws Exception {
        mockMvc.perform(get("/api/v1/consulta/20123456780"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("20123456780"))
                .andExpect(jsonPath("$.puntajeRiesgo").value(100))
                .andExpect(jsonPath("$.nivelRiesgo").value("BAJO"))
                .andExpect(jsonPath("$.reglasActivadas", empty()));
    }

    @Test
    @DisplayName("GET /api/v1/consulta/{ruc} con RUC impar debe descontar puntos por sanción")
    void testGetRucImparConSancion() throws Exception {
        mockMvc.perform(get("/api/v1/consulta/10123456781"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("10123456781"))
                .andExpect(jsonPath("$.puntajeRiesgo").value(90))
                .andExpect(jsonPath("$.nivelRiesgo").value("BAJO"))
                .andExpect(jsonPath("$.reglasActivadas", hasItem(containsString("R03"))));
    }

    @Test
    @DisplayName("POST /api/v1/consulta con RUC válido debe procesar correctamente")
    void testPostRucValido() throws Exception {
        String jsonRequest = "{\"ruc\":\"20600012342\"}";

        mockMvc.perform(post("/api/v1/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("20600012342"))
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
    @DisplayName("GET /api/v1/consulta/{ruc} con RUC que no inicia con 10 o 20 debe retornar 400 Bad Request")
    void testGetRucInvalidoPrefijo() throws Exception {
        mockMvc.perform(get("/api/v1/consulta/15123456789"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje", containsString("iniciar con los dígitos '10' o '20'")));
    }
}
