package com.syncro.pedido.service;

import com.syncro.pedido.model.Usuario;
import com.syncro.pedido.model.Empresa;
import com.syncro.pedido.model.Rol;
import com.syncro.pedido.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl - Tests Unitarios")
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        Empresa empresa = Empresa.builder()
                .id(1L).nombre("PYME Demo").rut("76.543.210-K")
                .email("admin@demo.cl").activo(true)
                .fechaCreacion(LocalDateTime.now()).build();

        usuarioMock = Usuario.builder()
                .id(1L).nombre("Oriana Solorzano")
                .email("oriana@pyme-demo.cl").password("$2a$12$hash")
                .empresa(empresa).rol(Rol.ADMIN).activo(true).build();
    }

    @Test
    @DisplayName("loadUserByUsername - email existente retorna UserDetails")
    void loadUserByUsername_emailExistente_retornaUserDetails() {
        when(usuarioRepository.findByEmail("oriana@pyme-demo.cl")).thenReturn(Optional.of(usuarioMock));

        UserDetails resultado = userDetailsService.loadUserByUsername("oriana@pyme-demo.cl");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getUsername()).isEqualTo("oriana@pyme-demo.cl");
        assertThat(resultado.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername - email inexistente lanza UsernameNotFoundException")
    void loadUserByUsername_emailInexistente_lanzaExcepcion() {
        when(usuarioRepository.findByEmail("noexiste@demo.cl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("noexiste@demo.cl"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("noexiste@demo.cl");
    }

    @Test
    @DisplayName("loadUserByUsername - usuario inactivo retorna isEnabled false")
    void loadUserByUsername_usuarioInactivo_retornaDeshabilitado() {
        usuarioMock.setActivo(false);
        when(usuarioRepository.findByEmail("oriana@pyme-demo.cl")).thenReturn(Optional.of(usuarioMock));

        UserDetails resultado = userDetailsService.loadUserByUsername("oriana@pyme-demo.cl");

        assertThat(resultado.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("loadUserByUsername - retorna authorities con rol correcto")
    void loadUserByUsername_retornaAuthoritiesConRol() {
        when(usuarioRepository.findByEmail("oriana@pyme-demo.cl")).thenReturn(Optional.of(usuarioMock));

        UserDetails resultado = userDetailsService.loadUserByUsername("oriana@pyme-demo.cl");

        assertThat(resultado.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
