package com.syncro.pedido.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncro.pedido.dto.request.EmpresaRequest;
import com.syncro.pedido.dto.response.EmpresaResponse;
import com.syncro.pedido.security.JwtUtil;
import com.syncro.pedido.service.EmpresaService;
import com.syncro.pedido.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = EmpresaController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@DisplayName("EmpresaController - Tests de integración con MockMvc")
@ActiveProfiles("test")
class EmpresaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmpresaService empresaService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private EmpresaResponse buildEmpresaResponse() {
        return EmpresaResponse.builder()
                .id(1L).nombre("PYME Demo SpA").rut("76.543.210-K")
                .email("admin@pyme-demo.cl").telefono("+56912345678")
                .activo(true).fechaCreacion(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("POST /empresas - datos válidos retorna 201 Created")
    @WithMockUser
    void crear_datosValidos_retorna201() throws Exception {
        when(empresaService.crear(any(EmpresaRequest.class))).thenReturn(buildEmpresaResponse());

        mockMvc.perform(post("/empresas")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new EmpresaRequest("PYME Demo SpA", "76.543.210-K", "admin@pyme-demo.cl", "+56912345678"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("PYME Demo SpA"))
                .andExpect(jsonPath("$.rut").value("76.543.210-K"));
    }

    @Test
    @DisplayName("POST /empresas - RUT duplicado retorna 400")
    @WithMockUser
    void crear_rutDuplicado_retorna400() throws Exception {
        when(empresaService.crear(any())).thenThrow(
                new IllegalArgumentException("Ya existe una empresa con el RUT: 76.543.210-K"));

        mockMvc.perform(post("/empresas")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new EmpresaRequest("PYME Demo SpA", "76.543.210-K", "admin@pyme-demo.cl", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("76.543.210-K")));
    }

    @Test
    @DisplayName("POST /empresas - sin nombre retorna 400")
    @WithMockUser
    void crear_sinNombre_retorna400() throws Exception {
        mockMvc.perform(post("/empresas")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rut\":\"76.543.210-K\",\"email\":\"admin@pyme-demo.cl\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /empresas - email inválido retorna 400")
    @WithMockUser
    void crear_emailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/empresas")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new EmpresaRequest("PYME", "76.543.210-K", "noesemail", null))))
                .andExpect(status().isBadRequest());
    }
}
