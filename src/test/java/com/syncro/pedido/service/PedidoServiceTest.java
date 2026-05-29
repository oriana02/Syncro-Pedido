package com.syncro.pedido.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncro.pedido.dto.request.CambiarEstadoRequest;
import com.syncro.pedido.dto.request.CrearPedidoRequest;
import com.syncro.pedido.dto.request.DireccionRequest;
import com.syncro.pedido.dto.request.ItemPedidoRequest;
import com.syncro.pedido.dto.response.PedidoResponse;
import com.syncro.pedido.dto.response.PedidoResumenResponse;
import com.syncro.pedido.exception.PedidoNotFoundException;
import com.syncro.pedido.exception.TransaccionEstadoInvalidaException;
import com.syncro.pedido.model.*;
import com.syncro.pedido.repository.EmpresaRepository;
import com.syncro.pedido.repository.OutboxEventoRepository;
import com.syncro.pedido.repository.PedidoRepository;
import com.syncro.pedido.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService - Tests Unitarios")
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private OutboxEventoRepository outboxRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private PedidoService pedidoService;

    private Empresa empresaMock;
    private Usuario usuarioMock;
    private Pedido pedidoMock;

    @BeforeEach
    void setUp() {
        empresaMock = Empresa.builder()
                .id(1L).nombre("PYME Demo SpA").rut("76.543.210-K")
                .email("admin@demo.cl").activo(true)
                .fechaCreacion(LocalDateTime.now()).build();

        usuarioMock = Usuario.builder()
                .id(1L).nombre("Oriana Solorzano")
                .email("oriana@pyme-demo.cl").password("$hash")
                .empresa(empresaMock).rol(Rol.ADMIN).activo(true).build();

        DireccionEntrega direccion = DireccionEntrega.builder()
                .id(1L).calle("Av. Providencia").numero("1234")
                .ciudad("Santiago").region("RM").pais("Chile").build();

        ItemPedido item = ItemPedido.builder()
                .id(1L).sku("ELEC-001").nombre("Audífonos BT")
                .cantidad(2).precioUnitario(new BigDecimal("29990")).build();

        pedidoMock = Pedido.builder()
                .id(1L).empresa(empresaMock).usuario(usuarioMock)
                .direccion(direccion).estado(EstadoPedido.PENDIENTE)
                .subtotal(new BigDecimal("59980")).costoEnvio(BigDecimal.ZERO)
                .total(new BigDecimal("59980")).eventoPublicado(false)
                .fechaCreacion(LocalDateTime.now()).fechaActualizacion(LocalDateTime.now())
                .items(new ArrayList<>(List.of(item)))
                .historialEstados(new ArrayList<>()).build();
        item.setPedido(pedidoMock);
    }

    // =========================================================================
    // crearPedido
    // =========================================================================
    @Test
    @DisplayName("crearPedido - datos válidos retorna PedidoResponse con estado PENDIENTE")
    void crearPedido_datosValidos_retornaPedidoConEstadoPendiente() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoMock);

        PedidoResponse response = pedidoService.crearPedido(buildCrearPedidoRequest());

        assertThat(response.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(response.getEmpresa().getId()).isEqualTo(1L);
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    @DisplayName("crearPedido - empresa inexistente lanza IllegalArgumentException")
    void crearPedido_empresaNoExiste_lanzaExcepcion() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        CrearPedidoRequest request = buildCrearPedidoRequest();
        request.setEmpresaId(99L);

        assertThatThrownBy(() -> pedidoService.crearPedido(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("crearPedido - usuario no pertenece a empresa lanza IllegalArgumentException")
    void crearPedido_usuarioDeOtraEmpresa_lanzaExcepcion() {
        Empresa otraEmpresa = Empresa.builder().id(2L).nombre("Otra").rut("11.111.111-1")
                .email("otra@cl").activo(true).fechaCreacion(LocalDateTime.now()).build();
        Usuario usuarioDeOtraEmpresa = Usuario.builder().id(2L).nombre("Otro")
                .email("otro@cl").password("$hash").empresa(otraEmpresa)
                .rol(Rol.OPERADOR).activo(true).build();

        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioDeOtraEmpresa));

        CrearPedidoRequest request = buildCrearPedidoRequest();
        request.setUsuarioId(2L);

        assertThatThrownBy(() -> pedidoService.crearPedido(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("crearPedido - calcula subtotal correctamente")
    void crearPedido_calculaSubtotalCorrecto() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PedidoResponse response = pedidoService.crearPedido(buildCrearPedidoRequest());

        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("59980"));
    }

    // =========================================================================
    // obtenerPedido
    // =========================================================================
    @Test
    @DisplayName("obtenerPedido - id existente retorna PedidoResponse")
    void obtenerPedido_idExistente_retornaResponse() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoMock));

        PedidoResponse response = pedidoService.obtenerPedido(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
    }

    @Test
    @DisplayName("obtenerPedido - id inexistente lanza PedidoNotFoundException")
    void obtenerPedido_idInexistente_lanzaExcepcion() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.obtenerPedido(99L))
                .isInstanceOf(PedidoNotFoundException.class);
    }

    // =========================================================================
    // cambiarEstado
    // =========================================================================
    @Test
    @DisplayName("cambiarEstado - PENDIENTE a CONFIRMADO es transición válida")
    void cambiarEstado_pendienteAConfirmado_retornaPedidoActualizado() throws Exception {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoMock));
        when(outboxRepository.save(any())).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"pedidoId\":1}");
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedidoMock);

        CambiarEstadoRequest request = new CambiarEstadoRequest(EstadoPedido.CONFIRMADO, "Pago ok");
        PedidoResponse response = pedidoService.cambiarEstado(1L, request);

        assertThat(response).isNotNull();
        verify(pedidoRepository, atLeastOnce()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("cambiarEstado - transición inválida lanza TransaccionEstadoInvalidaException")
    void cambiarEstado_transicionInvalida_lanzaExcepcion() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoMock));

        CambiarEstadoRequest request = new CambiarEstadoRequest(EstadoPedido.ENTREGADO, null);

        assertThatThrownBy(() -> pedidoService.cambiarEstado(1L, request))
                .isInstanceOf(TransaccionEstadoInvalidaException.class);
    }

    @Test
    @DisplayName("cambiarEstado - pedido inexistente lanza PedidoNotFoundException")
    void cambiarEstado_pedidoInexistente_lanzaExcepcion() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        CambiarEstadoRequest request = new CambiarEstadoRequest(EstadoPedido.CONFIRMADO, null);

        assertThatThrownBy(() -> pedidoService.cambiarEstado(99L, request))
                .isInstanceOf(PedidoNotFoundException.class);
    }

    // =========================================================================
    // obtenerHistorialPorEmpresa
    // =========================================================================
    @Test
    @DisplayName("obtenerHistorialPorEmpresa - sin filtros retorna lista de pedidos")
    void obtenerHistorialPorEmpresa_sinFiltros_retornaLista() {
        when(pedidoRepository.findByEmpresa_IdOrderByFechaCreacionDesc(1L))
                .thenReturn(List.of(pedidoMock));

        List<PedidoResumenResponse> lista = pedidoService.obtenerHistorialPorEmpresa(1L, null, null, null);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
    }

    @Test
    @DisplayName("obtenerHistorialPorEmpresa - filtrando por estado llama al método correcto")
    void obtenerHistorialPorEmpresa_conFiltroEstado_filtraPorEstado() {
        when(pedidoRepository.findByEmpresa_IdAndEstado(1L, EstadoPedido.PENDIENTE))
                .thenReturn(List.of(pedidoMock));

        List<PedidoResumenResponse> lista = pedidoService.obtenerHistorialPorEmpresa(
                1L, EstadoPedido.PENDIENTE, null, null);

        assertThat(lista).hasSize(1);
        verify(pedidoRepository).findByEmpresa_IdAndEstado(1L, EstadoPedido.PENDIENTE);
    }

    @Test
    @DisplayName("obtenerHistorialPorEmpresa - con rango de fechas filtra correctamente")
    void obtenerHistorialPorEmpresa_conFechas_filtraPorRango() {
        LocalDateTime desde = LocalDateTime.now().minusDays(7);
        LocalDateTime hasta = LocalDateTime.now();

        when(pedidoRepository.findByEmpresa_IdAndFechaCreacionBetween(1L, desde, hasta))
                .thenReturn(List.of(pedidoMock));

        List<PedidoResumenResponse> lista = pedidoService.obtenerHistorialPorEmpresa(1L, null, desde, hasta);

        assertThat(lista).hasSize(1);
        verify(pedidoRepository).findByEmpresa_IdAndFechaCreacionBetween(1L, desde, hasta);
    }

    // =========================================================================
    // Helper
    // =========================================================================
    private CrearPedidoRequest buildCrearPedidoRequest() {
        DireccionRequest dir = DireccionRequest.builder()
                .calle("Av. Providencia").numero("1234")
                .ciudad("Santiago").region("RM").pais("Chile").build();

        ItemPedidoRequest item = ItemPedidoRequest.builder()
                .sku("ELEC-001").nombre("Audífonos BT")
                .cantidad(2).precioUnitario(new BigDecimal("29990")).build();

        return CrearPedidoRequest.builder()
                .empresaId(1L).usuarioId(1L)
                .direccion(dir).items(new ArrayList<>(List.of(item))).build();
    }
}
