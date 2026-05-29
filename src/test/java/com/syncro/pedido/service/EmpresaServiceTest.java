package com.syncro.pedido.service;

import com.syncro.pedido.dto.request.EmpresaRequest;
import com.syncro.pedido.dto.response.EmpresaResponse;
import com.syncro.pedido.model.Empresa;
import com.syncro.pedido.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaService - Tests Unitarios")
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private EmpresaService empresaService;

    private EmpresaRequest requestValido;

    @BeforeEach
    void setUp() {
        requestValido = new EmpresaRequest(
                "PYME Demo SpA",
                "76.543.210-K",
                "admin@pyme-demo.cl",
                "+56912345678"
        );
    }

    @Test
    @DisplayName("crear - datos válidos retorna EmpresaResponse con todos los campos")
    void crear_datosValidos_retornaEmpresaResponse() {
        Empresa guardada = Empresa.builder()
                .id(1L)
                .nombre("PYME Demo SpA")
                .rut("76.543.210-K")
                .email("admin@pyme-demo.cl")
                .telefono("+56912345678")
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(empresaRepository.existsByRut("76.543.210-K")).thenReturn(false);
        when(empresaRepository.existsByEmail("admin@pyme-demo.cl")).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenReturn(guardada);

        EmpresaResponse response = empresaService.crear(requestValido);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNombre()).isEqualTo("PYME Demo SpA");
        assertThat(response.getRut()).isEqualTo("76.543.210-K");
        assertThat(response.getEmail()).isEqualTo("admin@pyme-demo.cl");
        assertThat(response.getActivo()).isTrue();
    }

    @Test
    @DisplayName("crear - RUT duplicado lanza IllegalArgumentException")
    void crear_rutDuplicado_lanzaExcepcion() {
        when(empresaRepository.existsByRut("76.543.210-K")).thenReturn(true);

        assertThatThrownBy(() -> empresaService.crear(requestValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("76.543.210-K");

        verify(empresaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear - email duplicado lanza IllegalArgumentException")
    void crear_emailDuplicado_lanzaExcepcion() {
        when(empresaRepository.existsByRut("76.543.210-K")).thenReturn(false);
        when(empresaRepository.existsByEmail("admin@pyme-demo.cl")).thenReturn(true);

        assertThatThrownBy(() -> empresaService.crear(requestValido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("admin@pyme-demo.cl");

        verify(empresaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear - llama a save exactamente una vez con datos válidos")
    void crear_llamaAlRepositorioUnaVez() {
        when(empresaRepository.existsByRut(any())).thenReturn(false);
        when(empresaRepository.existsByEmail(any())).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> {
            Empresa e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        empresaService.crear(requestValido);

        verify(empresaRepository, times(1)).save(any(Empresa.class));
    }
}
