package com.syncro.pedido.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncro.pedido.service.PedidoService;
import com.syncro.pedido.dto.request.CambiarEstadoRequest;
import com.syncro.pedido.dto.request.CrearPedidoRequest;
import com.syncro.pedido.dto.response.PedidoResponse;

import java.util.List;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.syncro.pedido.dto.response.PedidoResumenResponse;
import com.syncro.pedido.model.EstadoPedido;

/**
 * Controlador REST para la gestión de pedidos.
 *
 * Todas las rutas requieren token JWT válido en el header: Authorization:
 * Bearer <token>
 *
 * Base URL: /pedidos
 *
 * Endpoints disponibles: POST /pedidos → crear un pedido GET /pedidos/{id} →
 * obtener detalle de un pedido PATCH /pedidos/{id}/estado → cambiar estado de
 * un pedido GET /pedidos/historial/{empresaId} → listar pedidos de una empresa
 */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // =========================================================================
    // POST /pedidos
    // =========================================================================
    /**
     * Crea un nuevo pedido en estado PENDIENTE.
     *
     * Ejemplo de body: { "empresaId": 1, "usuarioId": 2, "notas": "Entregar
     * antes de las 18:00", "direccion": { "calle": "Av. Providencia", "numero":
     * "1234", "ciudad": "Santiago", "region": "Región Metropolitana" },
     * "items": [ { "sku": "ELEC-001", "nombre": "Audífonos BT", "cantidad": 2,
     * "precioUnitario": 29990 } ] }
     */
    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(
            @Valid @RequestBody CrearPedidoRequest request) {
        log.info("POST /pedido - empresa ID={}", request.getEmpresaId());
        PedidoResponse response = pedidoService.crearPedido(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /pedidos/{id}
    // =========================================================================
    /**
     * Obtiene el detalle completo de un pedido por su ID. Incluye ítems,
     * dirección, empresa, usuario e historial de estados
     *
     * Ejemplo: GET /pedidos/15
     */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtenerPedido(
            @PathVariable Long id) {
        log.info("GET /pedidos/{}", id);
        PedidoResponse response = pedidoService.obtenerPedido(id);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PATCH /pedidos/{id}/estado
    // =========================================================================
    /**
     * Cambia el estado de un pedido existente.
     *
     * Solo acepta transiciones válidas según las reglas de negocio: PENDIENTE →
     * CONFIRMADO | CANCELADO CONFIRMADO → EN_PREPARACION | CANCELADO
     * EN_PREPARACION → DESPACHADO | CANCELADO DESPACHADO → EN_RUTA EN_RUTA →
     * ENTREGADO
     *
     * Si la transición no es válida devuelve 409.
     *
     * Respuestas posibles: 200 OK → estado cambiado exitosamente 400 Bad
     * Request → body inválido 401 Unauthorized → token JWT inválido o ausente
     * 404 Not Found → no existe pedido con ese ID 409 Conflict → transición de
     * estado no permitida
     *
     * Ejemplo de body: { "nuevoEstado": "CONFIRMADO", "motivo": "Pago
     * verificado exitosamente" }
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request) {

        log.info("PATCH /pedidos/{}/estado", id, request.getNuevoEstado());
        PedidoResponse response = pedidoService.cambiarEstado(id, request);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /pedidos/historial/{empresaId}
    // =========================================================================
    /**
     * Lista los pedidos de una empresa con filtros opcionales. Devuelve un
     * resumen de cada pedido (ID, fecha, estado, total) sin detalles de ítems
     * ni dirección.
     *
     * Parámetros opcionales: - estado: filtra por estado exacto (ej:
     * ?estado=CONFIRMADO) - desde: filtra desde una fecha (ej:
     * ?desde=2024-01-01T00:00:00) - hasta: filtra hasta una fecha (ej:
     * ?hasta=2024-12-31T23:59:59)
     *
     * Nota: si se envía "estado", se ignoran "desde" y "hasta".
     *
     * Respuestas posibles: 200 OK → lista de pedidos (puede ser vacía) 401
     * Unauthorized → token JWT inválido o ausente
     *
     * Ejemplos: GET /pedidos/historial/1 GET
     * /pedidos/historial/1?estado=CONFIRMADO GET
     * /pedidos/historial/1?desde=2024-01-01T00:00:00&hasta=2024-06-30T23:59:59
     */
    @GetMapping("/historial/{empresaId}")
    public ResponseEntity<List<PedidoResumenResponse>> obtenerHistorial(
            @PathVariable Long empresaId,
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        log.info("GET /pedidos/historial/{} - estado={}, desde={}, hasta={}",
                empresaId, estado, desde, hasta);

        List<PedidoResumenResponse> response
                = pedidoService.obtenerHistorialPorEmpresa(empresaId, estado, desde, hasta);

        return ResponseEntity.ok(response);
    }

}
