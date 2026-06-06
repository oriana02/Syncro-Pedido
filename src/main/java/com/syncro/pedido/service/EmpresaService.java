package com.syncro.pedido.service;

import com.syncro.pedido.dto.request.EmpresaRequest;
import com.syncro.pedido.dto.response.EmpresaResponse;
import com.syncro.pedido.model.Empresa;
import com.syncro.pedido.repository.EmpresaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaResponse crear(EmpresaRequest request) {

        // Verificar duplicados
        if (empresaRepository.existsByRut(request.getRut())) {
            throw new IllegalArgumentException("Ya existe una empresa con el RUT: " + request.getRut());
        }
        if (empresaRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe una empresa con el email: " + request.getEmail());
        }

        Empresa empresa = Empresa.builder()
                .nombre(request.getNombre())
                .rut(request.getRut())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .build();

        Empresa guardada = empresaRepository.save(empresa);
        log.info("Empresa creada con ID: {}", guardada.getId());

        return toResponse(guardada);
    }

    private EmpresaResponse toResponse(Empresa e) {
        return EmpresaResponse.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .rut(e.getRut())
                .email(e.getEmail())
                .telefono(e.getTelefono())
                .activo(e.getActivo())
                .fechaCreacion(e.getFechaCreacion())
                .build();
    }

}
