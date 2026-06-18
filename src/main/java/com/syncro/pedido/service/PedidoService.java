package com.syncro.pedido.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.syncro.pedido.repository.PedidoRepository;
import com.syncro.pedido.repository.UsuarioRepository;
import com.syncro.pedido.repository.EmpresaRepository;
import com.syncro.pedido.repository.OutboxEventoRepository;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.syncro.pedido.dto.request.CambiarEstadoRequest;
import com.syncro.pedido.dto.request.CrearPedidoRequest;
import com.syncro.pedido.dto.response.DireccionResponse;
import com.syncro.pedido.dto.response.EmpresaResumenResponse;
import com.syncro.pedido.dto.response.HistorialEstadoResponse;
import com.syncro.pedido.dto.response.ItemPedidoResponse;
import com.syncro.pedido.dto.response.PedidoResponse;
import com.syncro.pedido.dto.response.PedidoResumenResponse;
import com.syncro.pedido.dto.response.UsuarioResumenResponse;
import com.syncro.pedido.event.PedidoCreadoEvent;
import com.syncro.pedido.exception.PedidoNotFoundException;
import com.syncro.pedido.exception.TransaccionEstadoInvalidaException;
import com.syncro.pedido.model.*;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    private final EmpresaRepository empresaRepository;

    private final UsuarioRepository usuarioRepository;

    private final OutboxEventoRepository outboxRepository;

    private final ObjectMapper objectMapper;

    public PedidoService(PedidoRepository pedidoRepository,
            EmpresaRepository empresaRepository,
            UsuarioRepository usuarioRepository,
            OutboxEventoRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.pedidoRepository = pedidoRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES_VALIDAS = Map.of(
            EstadoPedido.PENDIENTE, EnumSet.of(EstadoPedido.CONFIRMADO, EstadoPedido.CANCELADO),
            EstadoPedido.CONFIRMADO, EnumSet.of(EstadoPedido.EN_PREPARACION, EstadoPedido.CANCELADO),
            EstadoPedido.EN_PREPARACION, EnumSet.of(EstadoPedido.DESPACHADO, EstadoPedido.CANCELADO),
            EstadoPedido.DESPACHADO, EnumSet.of(EstadoPedido.EN_RUTA),
            EstadoPedido.EN_RUTA, EnumSet.of(EstadoPedido.ENTREGADO),
            EstadoPedido.ENTREGADO, Collections.emptySet(),
            EstadoPedido.CANCELADO, Collections.emptySet()
    );

    // =========================================================================
    // CREAR PEDIDO
    // =========================================================================
    @Transactional
    public PedidoResponse crearPedido(CrearPedidoRequest request) {
        log.info("Iniciando creación de pedido para empresa ID={}", request.getEmpresaId());

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new IllegalArgumentException(
                "No existe empresa con ID: " + request.getEmpresaId()));

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException(
                "No existe usuario con ID: " + request.getUsuarioId()));

        if (!usuario.getEmpresa().getId().equals(empresa.getId())) {
            throw new IllegalArgumentException(
                    "El usuario ID=" + request.getUsuarioId()
                    + " no pertenece a la empresa ID=" + request.getEmpresaId());
        }

        if (request.getDireccion() == null) {
            throw new IllegalArgumentException("La dirección de entrega es obligatoria");
        }

        DireccionEntrega direccion = DireccionEntrega.builder()
                .calle(request.getDireccion().getCalle())
                .numero(request.getDireccion().getNumero())
                .depto(request.getDireccion().getDepto())
                .ciudad(request.getDireccion().getCiudad())
                .region(request.getDireccion().getRegion())
                .pais(request.getDireccion().getPais() != null ? request.getDireccion().getPais() : "Chile")
                .codigoPostal(request.getDireccion().getCodigoPostal())
                .referencia(request.getDireccion().getReferencia())
                .build();

        Pedido pedido = Pedido.builder()
                .empresa(empresa)
                .usuario(usuario)
                .direccion(direccion)
                .estado(EstadoPedido.PENDIENTE)
                .notas(request.getNotas())
                .referenciaExterna(request.getReferenciaExterna())
                .subtotal(BigDecimal.ZERO)
                .costoEnvio(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .historialEstados(new ArrayList<>())
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        List<ItemPedido> items = request.getItems().stream()
                .map(itemReq -> ItemPedido.builder()
                .pedido(pedido)
                .sku(itemReq.getSku())
                .nombre(itemReq.getNombre())
                .cantidad(itemReq.getCantidad())
                .precioUnitario(itemReq.getPrecioUnitario())
                .build())
                .collect(Collectors.toCollection(ArrayList::new));

        pedido.setItems(items);

        BigDecimal subtotal = items.stream()
                .map(i -> i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pedido.setSubtotal(subtotal);
        pedido.setTotal(subtotal);

        HistorialEstado historialInicial = HistorialEstado.builder()
                .pedido(pedido)
                .estadoAnterior(null)
                .estadoNuevo(EstadoPedido.PENDIENTE)
                .actorId(usuario.getId())
                .actorTipo("USUARIO")
                .motivo("Creación del pedido")
                .fechaCambio(LocalDateTime.now())
                .build();

        pedido.getHistorialEstados().add(historialInicial);

        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido ID={} creado exitosamente para empresa={}", guardado.getId(), empresa.getNombre());

        return mapToResponse(guardado);
    }

    // =========================================================================
    // CAMBIAR ESTADO
    // =========================================================================
    @Transactional
    public PedidoResponse cambiarEstado(Long pedidoId, CambiarEstadoRequest request) {
        log.info("Cambiando estado del pedido ID={} a {}", pedidoId, request.getNuevoEstado());

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNotFoundException(pedidoId));

        EstadoPedido estadoActual = pedido.getEstado();
        EstadoPedido nuevoEstado = request.getNuevoEstado();

        // Validar que la transición esté permitida
        Set<EstadoPedido> permitidas = TRANSICIONES_VALIDAS
                .getOrDefault(estadoActual, EnumSet.noneOf(EstadoPedido.class));

        if (!permitidas.contains(nuevoEstado)) {
            throw new TransaccionEstadoInvalidaException(estadoActual.name(), nuevoEstado.name());
        }

        Long actorId = getActorId();

        // Registrar en historial
        HistorialEstado historial = HistorialEstado.builder()
                .pedido(pedido)
                .estadoAnterior(estadoActual)
                .estadoNuevo(nuevoEstado)
                .actorId(actorId)
                .actorTipo(actorId != null ? "USUARIO" : "SISTEMA")
                .motivo(request.getMotivo())
                .fechaCambio(LocalDateTime.now())
                .build();

        // Actualizar el pedido ANTES del save
        pedido.setEstado(nuevoEstado);
        pedido.setFechaActualizacion(LocalDateTime.now());
        pedido.getHistorialEstados().add(historial);

        if (nuevoEstado == EstadoPedido.CONFIRMADO) {
            pedido.setFechaConfirmacion(LocalDateTime.now());
        } else if (nuevoEstado == EstadoPedido.ENTREGADO) {
            pedido.setFechaEntrega(LocalDateTime.now());
        }

        // ── Outbox: solo cuando pasa a CONFIRMADO por primera vez ──────────
        if (nuevoEstado == EstadoPedido.CONFIRMADO && !pedido.getEventoPublicado()) {

            pedido.setEventoPublicado(true); // marcar antes del save

            PedidoCreadoEvent evento = PedidoCreadoEvent.builder()
                    .pedidoId(pedido.getId())
                    .empresaId(pedido.getEmpresa().getId())
                    .destinatarioNombre(pedido.getUsuario().getNombre())
                    .destinatarioEmail(pedido.getUsuario().getEmail())
                    .destinatarioTel(null)
                    .direccionCalle(pedido.getDireccion().getCalle())
                    .direccionNumero(pedido.getDireccion().getNumero())
                    .direccionDepto(pedido.getDireccion().getDepto())
                    .direccionCiudad(pedido.getDireccion().getCiudad())
                    .direccionRegion(pedido.getDireccion().getRegion())
                    .direccionPais(pedido.getDireccion().getPais())
                    .codigoPostal(pedido.getDireccion().getCodigoPostal())
                    .items(pedido.getItems().stream()
                            .map(item -> PedidoCreadoEvent.ItemEvento.builder()
                            .sku(item.getSku())
                            .nombre(item.getNombre())
                            .cantidad(item.getCantidad())
                            .precioUnitario(item.getPrecioUnitario())
                            .build())
                            .toList())
                    .build();

            try {
                OutboxEvento outbox = OutboxEvento.builder()
                        .pedidoId(pedido.getId())
                        .tipoEvento("pedido.creado")
                        .payload(objectMapper.writeValueAsString(evento))
                        .enviado(false)
                        .intentos(0)
                        .fechaCreacion(LocalDateTime.now())
                        .build();

                outboxRepository.save(outbox);

            } catch (Exception e) {
                log.error("Error serializando evento para outbox, pedido ID={}", pedidoId, e);
                throw new RuntimeException("No se pudo encolar el evento", e);
            }
        }
        // ── Fin bloque Outbox ───────────────────────────────────────────────

        // UN solo save para todo: estado + historial + flag + outbox
        Pedido actualizado = pedidoRepository.save(pedido);

        log.info("Pedido ID={} cambió de {} a {}", pedidoId, estadoActual, nuevoEstado);
        return mapToResponse(actualizado);
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================
    @Transactional(readOnly = true)
    public PedidoResponse obtenerPedido(Long id) {
        log.debug("Consultando pedido ID={}", id);
        return pedidoRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new PedidoNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<PedidoResumenResponse> obtenerHistorialPorEmpresa(
            Long empresaId,
            EstadoPedido estado,
            LocalDateTime desde,
            LocalDateTime hasta) {

        log.debug("Consultando historial de pedidos para empresa ID={}", empresaId);

        List<Pedido> pedidos;

        if (estado != null) {
            pedidos = pedidoRepository.findByEmpresa_IdAndEstado(empresaId, estado);
        } else if (desde != null && hasta != null) {
            pedidos = pedidoRepository.findByEmpresa_IdAndFechaCreacionBetween(empresaId, desde, hasta);
        } else {
            pedidos = pedidoRepository.findByEmpresa_IdOrderByFechaCreacionDesc(empresaId);
        }

        return pedidos.stream().map(this::mapToResumen).toList();
    }

    // =========================================================================
    // MAPPERS
    // =========================================================================
    private PedidoResponse mapToResponse(Pedido pedido) {

        List<ItemPedidoResponse> itemsResponse = pedido.getItems().stream()
                .map(i -> ItemPedidoResponse.builder()
                .id(i.getId())
                .sku(i.getSku())
                .nombre(i.getNombre())
                .cantidad(i.getCantidad())
                .precioUnitario(i.getPrecioUnitario())
                .subtotal(i.getSubtotal())
                .build())
                .toList();

        List<HistorialEstadoResponse> historialResponse = pedido.getHistorialEstados().stream()
                .map(h -> HistorialEstadoResponse.builder()
                .id(h.getId())
                .estadoAnterior(h.getEstadoAnterior())
                .estadoNuevo(h.getEstadoNuevo())
                .fechaCambio(h.getFechaCambio())
                .actorTipo(h.getActorTipo())
                .actorId(h.getActorId())
                .motivo(h.getMotivo())
                .build())
                .toList();

        EmpresaResumenResponse empresaResponse = EmpresaResumenResponse.builder()
                .id(pedido.getEmpresa().getId())
                .nombre(pedido.getEmpresa().getNombre())
                .rut(pedido.getEmpresa().getRut())
                .build();

        UsuarioResumenResponse usuarioResponse = UsuarioResumenResponse.builder()
                .id(pedido.getUsuario().getId())
                .nombre(pedido.getUsuario().getNombre())
                .email(pedido.getUsuario().getEmail())
                .build();

        DireccionResponse direccionResponse = DireccionResponse.builder()
                .id(pedido.getDireccion().getId())
                .calle(pedido.getDireccion().getCalle())
                .numero(pedido.getDireccion().getNumero())
                .depto(pedido.getDireccion().getDepto())
                .ciudad(pedido.getDireccion().getCiudad())
                .region(pedido.getDireccion().getRegion())
                .pais(pedido.getDireccion().getPais())
                .codigoPostal(pedido.getDireccion().getCodigoPostal())
                .referencia(pedido.getDireccion().getReferencia())
                .build();

        return PedidoResponse.builder()
                .id(pedido.getId())
                .empresa(empresaResponse)
                .usuario(usuarioResponse)
                .direccion(direccionResponse)
                .estado(pedido.getEstado())
                .subtotal(pedido.getSubtotal())
                .costoEnvio(pedido.getCostoEnvio())
                .total(pedido.getTotal())
                .notas(pedido.getNotas())
                .referenciaExterna(pedido.getReferenciaExterna())
                .fechaCreacion(pedido.getFechaCreacion())
                .fechaActualizacion(pedido.getFechaActualizacion())
                .fechaConfirmacion(pedido.getFechaConfirmacion())
                .fechaEntrega(pedido.getFechaEntrega())
                .items(itemsResponse)
                .historialEstados(historialResponse)
                .build();
    }

    private PedidoResumenResponse mapToResumen(Pedido pedido) {
        return PedidoResumenResponse.builder()
                .id(pedido.getId())
                .empresaNombre(pedido.getEmpresa().getNombre())
                .usuarioNombre(pedido.getUsuario().getNombre())
                .estado(pedido.getEstado())
                .total(pedido.getTotal())
                .totalItems(pedido.getItems().size())
                .fechaCreacion(pedido.getFechaCreacion())
                .referenciaExterna(pedido.getReferenciaExterna())
                .build();
    }

    // =========================================================================
    // AUXILIARES
    // =========================================================================
    private Long getActorId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }
            if ("anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }

            return usuarioRepository.findByEmail(auth.getName())
                    .map(Usuario::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.info("Error obteniendo usuario autenticado", e);
            return null;
        }
    }
}
