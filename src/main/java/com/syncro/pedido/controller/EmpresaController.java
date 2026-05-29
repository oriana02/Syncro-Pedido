package com.syncro.pedido.controller;

import com.syncro.pedido.dto.request.EmpresaRequest;
import com.syncro.pedido.dto.response.EmpresaResponse;
import com.syncro.pedido.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
@Slf4j
public class EmpresaController {

    private final EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<EmpresaResponse> crear(
            @Valid @RequestBody EmpresaRequest request) {

        log.info("POST /empresas - nombre={}", request.getNombre());
        EmpresaResponse response = empresaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
