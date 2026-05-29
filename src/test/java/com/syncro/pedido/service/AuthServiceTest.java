package com.syncro.pedido.service;

import com.syncro.pedido.dto.request.LoginRequest;
import com.syncro.pedido.dto.request.RegisterRequest;
import com.syncro.pedido.dto.response.AuthResponse;
import com.syncro.pedido.model.Empresa;
import com.syncro.pedido.model.Rol;
import com.syncro.pedido.model.Usuario;
import com.syncro.pedido.repository.EmpresaRepository;
import com.syncro.pedido.repository.UsuarioRepository;
import com.syncro.pedido.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests Unitarios")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Empresa empresaMock;
    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        empresaMock = Empresa.builder()
                .id(1L)
                .nombre("PYME Demo SpA")
                .rut("76.543.210-K")
                .email("admin@pyme-demo.cl")
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();

        usuarioMock = Usuario.builder()
                .id(1L)
                .nombre("Oriana Solorzano")
                .email("oriana@pyme-demo.cl")
                .password("$2a$12$hashedpassword")
                .empresa(empresaMock)
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
    }

    // =========================================================================
    // register
    // =========================================================================

    @Test
    @DisplayName("register - datos válidos retorna AuthResponse con token")
    void register_conDatosValidos_retornaAuthResponse() {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Sofía Gómez")
                .email("sofia@pyme-demo.cl")
                .password("Admin1234!")
                .empresaId(1L)
                .rol(Rol.OPERADOR)
                .build();

        when(usuarioRepository.existsByEmail("sofia@pyme-demo.cl")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(passwordEncoder.encode("Admin1234!")).thenReturn("$2a$12$encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(jwtUtil.generateToken(any())).thenReturn("token.jwt.firmado");
        when(jwtUtil.getExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("token.jwt.firmado");
        assertThat(response.getTipo()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("sofia@pyme-demo.cl");
        assertThat(response.getRol()).isEqualTo("OPERADOR");
        assertThat(response.getEmpresaId()).isEqualTo(1L);
        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
        verify(passwordEncoder).encode("Admin1234!");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("register - sin rol asigna OPERADOR por defecto")
    void register_sinRol_asignaOperadorPorDefecto() {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Nuevo")
                .email("nuevo@pyme.cl")
                .password("Pass1234!")
                .empresaId(1L)
                .rol(null)
                .build();

        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(passwordEncoder.encode(any())).thenReturn("$hash");
        when(jwtUtil.generateToken(any())).thenReturn("tok");
        when(jwtUtil.getExpiration()).thenReturn(86400000L);
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.getRol()).isEqualTo("OPERADOR");
    }

    @Test
    @DisplayName("register - email duplicado lanza IllegalArgumentException")
    void register_emailDuplicado_lanzaExcepcion() {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Dup").email("oriana@pyme-demo.cl")
                .password("Pass!").empresaId(1L).build();

        when(usuarioRepository.existsByEmail("oriana@pyme-demo.cl")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oriana@pyme-demo.cl");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - empresa inexistente lanza IllegalArgumentException")
    void register_empresaNoExiste_lanzaExcepcion() {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Sin empresa").email("sin@empresa.cl")
                .password("Pass!").empresaId(99L).build();

        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - contraseña nunca se guarda en texto plano")
    void register_contrasena_siempreGuardaCifrada() {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Test").email("test@pyme.cl")
                .password("MiPassword123!").empresaId(1L).rol(Rol.OPERADOR).build();

        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresaMock));
        when(passwordEncoder.encode("MiPassword123!")).thenReturn("$2a$12$hash_seguro");
        when(jwtUtil.generateToken(any())).thenReturn("tok");
        when(jwtUtil.getExpiration()).thenReturn(86400000L);
        when(usuarioRepository.save(argThat(u ->
                !u.getPassword().equals("MiPassword123!")
        ))).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> authService.register(request)).doesNotThrowAnyException();
        verify(passwordEncoder, times(1)).encode("MiPassword123!");
    }

    // =========================================================================
    // login
    // =========================================================================

    @Test
    @DisplayName("login - credenciales válidas retorna AuthResponse con token")
    void login_credencialesValidas_retornaAuthResponse() {
        LoginRequest request = new LoginRequest("oriana@pyme-demo.cl", "Admin1234!");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(usuarioRepository.findByEmail("oriana@pyme-demo.cl")).thenReturn(Optional.of(usuarioMock));
        when(jwtUtil.generateToken(usuarioMock)).thenReturn("token.valido.jwt");
        when(jwtUtil.getExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("token.valido.jwt");
        assertThat(response.getTipo()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("oriana@pyme-demo.cl");
        assertThat(response.getRol()).isEqualTo("ADMIN");
        assertThat(response.getEmpresaId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("login - credenciales inválidas lanza BadCredentialsException")
    void login_credencialesInvalidas_lanzaExcepcion() {
        LoginRequest request = new LoginRequest("oriana@pyme-demo.cl", "wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("login - respuesta incluye expiresIn configurado")
    void login_exitoso_incluyeExpiresIn() {
        LoginRequest request = new LoginRequest("oriana@pyme-demo.cl", "Admin1234!");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(usuarioRepository.findByEmail("oriana@pyme-demo.cl")).thenReturn(Optional.of(usuarioMock));
        when(jwtUtil.generateToken(any())).thenReturn("tok");
        when(jwtUtil.getExpiration()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
    }
}