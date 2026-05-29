package com.syncro.pedido.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncro.pedido.dto.request.CambiarEstadoRequest;
import com.syncro.pedido.dto.request.CrearPedidoRequest;
import com.syncro.pedido.dto.request.DireccionRequest;
import com.syncro.pedido.dto.request.ItemPedidoRequest;
import com.syncro.pedido.dto.response.*;
import com.syncro.pedido.exception.PedidoNotFoundException;
import com.syncro.pedido.exception.TransaccionEstadoInvalidaException;
import com.syncro.pedido.model.EstadoPedido;
import com.syncro.pedido.service.PedidoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PedidoController.class)
@DisplayName("PedidoController - Tests de integración con MockMvc")
@ActiveProfiles("test")
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PedidoService pedidoService;

    private PedidoResponse pedidoResponseMock;

    @BeforeEach
    void setUp() {
        EmpresaResumenResponse empresa = EmpresaResumenResponse.builder()
                .id(1L).nombre("PYME Demo SpA").rut("76.543.210-K").build();

        UsuarioResumenResponse usuario = UsuarioResumenResponse.builder()
                .id(1L).nombre("Oriana Solorzano").email("oriana@pyme-demo.cl").build();

        DireccionResponse direccion = DireccionResponse.builder()
                .id(1L).calle("Av. Providencia").numero("1234")
                .ciudad("Santiago").region("Región Metropolitana").pais("Chile").build();

        ItemPedidoResponse item = ItemPedidoResponse.builder()
                .id(1L).sku("ELEC-001").nombre("Audífonos Bluetooth")
                .cantidad(2).precioUnitario(new BigDecimal("29990"))
                .subtotal(new BigDecimal("59980")).build();

        pedidoResponseMock = PedidoResponse.builder()
                .id(1L)
                .empresa(empresa)
                .usuario(usuario)
                .direccion(direccion)
                .estado(EstadoPedido.PENDIENTE)
                .subtotal(new BigDecimal("59980"))
                .costoEnvio(BigDecimal.ZERO)
                .total(new BigDecimal("59980"))
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .items(List.of(item))
                .historialEstados(new ArrayList<>())
                .build();
    }

    // =========================================================================
    // POST /pedidos
    // =========================================================================
    @Test
    @DisplayName("POST /pedidos - con datos válidos debe retornar 201 Created")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void crearPedido_datosValidos_retorna201() throws Exception {
        // Arrange
        CrearPedidoRequest request = buildCrearPedidoRequest();
        when(pedidoService.crearPedido(any(CrearPedidoRequest.class))).thenReturn(pedidoResponseMock);

        // Act & Assert
        mockMvc.perform(post("/pedidos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.empresa.nombre").value("PYME Demo SpA"))
                .andExpect(jsonPath("$.items[0].sku").value("ELEC-001"));
    }

    @Test
    @DisplayName("POST /pedidos - sin autenticación debe retornar 401")
    void crearPedido_sinAutenticacion_retorna401() throws Exception {
        CrearPedidoRequest request = buildCrearPedidoRequest();

        mockMvc.perform(post("/pedidos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /pedidos - con body vacío debe retornar 400 Bad Request")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void crearPedido_bodyVacio_retorna400() throws Exception {
        mockMvc.perform(post("/pedidos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /pedidos - sin items debe retornar 400 Bad Request")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void crearPedido_sinItems_retorna400() throws Exception {
        CrearPedidoRequest request = buildCrearPedidoRequest();
        request.setItems(new ArrayList<>());

        mockMvc.perform(post("/pedidos")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET /pedidos/{id}
    // =========================================================================
    @Test
    @DisplayName("GET /pedidos/{id} - pedido existente debe retornar 200 OK con datos")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void obtenerPedido_idExistente_retorna200() throws Exception {
        when(pedidoService.obtenerPedido(1L)).thenReturn(pedidoResponseMock);

        mockMvc.perform(get("/pedidos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.subtotal").value(59980));
    }

    @Test
    @DisplayName("GET /pedidos/{id} - pedido inexistente debe retornar 404 Not Found")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void obtenerPedido_idInexistente_retorna404() throws Exception {
        when(pedidoService.obtenerPedido(99L)).thenThrow(new PedidoNotFoundException(99L));

        mockMvc.perform(get("/pedidos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // =========================================================================
    // PATCH /pedidos/{id}/estado
    // =========================================================================
    @Test
    @DisplayName("PATCH /pedidos/{id}/estado - cambio válido debe retornar 200 OK")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void cambiarEstado_transicionValida_retorna200() throws Exception {
        PedidoResponse confirmado = PedidoResponse.builder()
                .id(1L)
                .empresa(pedidoResponseMock.getEmpresa())
                .usuario(pedidoResponseMock.getUsuario())
                .direccion(pedidoResponseMock.getDireccion())
                .estado(EstadoPedido.CONFIRMADO)
                .subtotal(new BigDecimal("59980"))
                .costoEnvio(BigDecimal.ZERO)
                .total(new BigDecimal("59980"))
                .fechaCreacion(LocalDateTime.now())
                .items(pedidoResponseMock.getItems())
                .historialEstados(new ArrayList<>())
                .build();

        CambiarEstadoRequest request = new CambiarEstadoRequest(EstadoPedido.CONFIRMADO, "Pago verificado");
        when(pedidoService.cambiarEstado(eq(1L), any())).thenReturn(confirmado);

        mockMvc.perform(patch("/pedidos/1/estado")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    @Test
    @DisplayName("PATCH /pedidos/{id}/estado - transición inválida debe retornar 409 Conflict")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void cambiarEstado_transicionInvalida_retorna409() throws Exception {
        CambiarEstadoRequest request = new CambiarEstadoRequest(EstadoPedido.ENTREGADO, null);
        when(pedidoService.cambiarEstado(eq(1L), any()))
                .thenThrow(new TransaccionEstadoInvalidaException("PENDIENTE", "ENTREGADO"));

        mockMvc.perform(patch("/pedidos/1/estado")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PENDIENTE")));
    }

    @Test
    @DisplayName("PATCH /pedidos/{id}/estado - sin nuevoEstado debe retornar 400")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void cambiarEstado_sinNuevoEstado_retorna400() throws Exception {
        mockMvc.perform(patch("/pedidos/1/estado")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": \"sin estado\"}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET /pedidos/historial/{empresaId}
    // =========================================================================
    @Test
    @DisplayName("GET /pedidos/historial/{empresaId} - retorna lista de pedidos")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void obtenerHistorial_retornaLista() throws Exception {
        PedidoResumenResponse resumen = PedidoResumenResponse.builder()
                .id(1L).empresaNombre("PYME Demo SpA").usuarioNombre("Oriana Solorzano")
                .estado(EstadoPedido.PENDIENTE).total(new BigDecimal("59980"))
                .totalItems(1).fechaCreacion(LocalDateTime.now()).build();

        when(pedidoService.obtenerHistorialPorEmpresa(eq(1L), any(), any(), any()))
                .thenReturn(List.of(resumen));

        mockMvc.perform(get("/pedidos/historial/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("GET /pedidos/historial/{empresaId} - lista vacía retorna 200 con array vacío")
    @WithMockUser(username = "oriana@pyme-demo.cl", roles = "ADMIN")
    void obtenerHistorial_listaVacia_retorna200ConArrayVacio() throws Exception {
        when(pedidoService.obtenerHistorialPorEmpresa(eq(99L), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/pedidos/historial/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private CrearPedidoRequest buildCrearPedidoRequest() {
        DireccionRequest direccion = DireccionRequest.builder()
                .calle("Av. Providencia").numero("1234")
                .ciudad("Santiago").region("Región Metropolitana").pais("Chile").build();

        ItemPedidoRequest item = ItemPedidoRequest.builder()
                .sku("ELEC-001").nombre("Audífonos Bluetooth")
                .cantidad(2).precioUnitario(new BigDecimal("29990")).build();

        return CrearPedidoRequest.builder()
                .empresaId(1L).usuarioId(1L)
                .notas("Entregar en horario de mañana")
                .direccion(direccion)
                .items(new ArrayList<>(List.of(item)))
                .build();
    }
}
