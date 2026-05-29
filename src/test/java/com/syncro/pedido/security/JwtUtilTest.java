package com.syncro.pedido.security;

import com.syncro.pedido.model.Empresa;
import com.syncro.pedido.model.Rol;
import com.syncro.pedido.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil - Tests Unitarios")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails usuarioMock;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inyectar valores de configuración directamente sin Spring context
        ReflectionTestUtils.setField(jwtUtil, "secret", "syncro-secret-key-para-test-minimo-32-chars!!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        Empresa empresa = Empresa.builder()
                .id(1L).nombre("PYME Demo").rut("76.543.210-K")
                .email("admin@demo.cl").activo(true)
                .fechaCreacion(LocalDateTime.now()).build();

        usuarioMock = Usuario.builder()
                .id(1L)
                .nombre("Oriana Solorzano")
                .email("oriana@pyme-demo.cl")
                .password("$2a$12$hash")
                .empresa(empresa)
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("generateToken - debe generar un token no nulo y no vacío")
    void generateToken_usuarioValido_retornaTokenNoVacio() {
        String token = jwtUtil.generateToken(usuarioMock);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateToken - el token debe tener formato JWT (3 partes separadas por punto)")
    void generateToken_retornaFormatoJWT() {
        String token = jwtUtil.generateToken(usuarioMock);
        String[] partes = token.split("\\.");
        assertThat(partes).hasSize(3);
    }

    @Test
    @DisplayName("extractUsername - debe extraer el email del token correctamente")
    void extractUsername_tokenValido_retornaEmail() {
        String token = jwtUtil.generateToken(usuarioMock);
        String email = jwtUtil.extractUsername(token);
        assertThat(email).isEqualTo("oriana@pyme-demo.cl");
    }

    @Test
    @DisplayName("validateToken - token válido del mismo usuario debe retornar true")
    void validateToken_tokenValidoMismoUsuario_retornaTrue() {
        String token = jwtUtil.generateToken(usuarioMock);
        boolean valido = jwtUtil.validateToken(token, usuarioMock);
        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("validateToken - token de otro usuario debe retornar false")
    void validateToken_tokenDeOtroUsuario_retornaFalse() {
        Empresa empresa = Empresa.builder()
                .id(2L).nombre("Otra").rut("11.111.111-1").email("otra@cl").activo(true)
                .fechaCreacion(LocalDateTime.now()).build();

        UserDetails otroUsuario = Usuario.builder()
                .id(2L).nombre("Otro").email("otro@empresa.cl")
                .password("hash").empresa(empresa).rol(Rol.OPERADOR).activo(true).build();

        String tokenDeOtro = jwtUtil.generateToken(otroUsuario);
        boolean valido = jwtUtil.validateToken(tokenDeOtro, usuarioMock);

        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired - token recién generado no debe estar expirado")
    void isTokenExpired_tokenReciente_retornaFalse() {
        String token = jwtUtil.generateToken(usuarioMock);
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired - token con expiración en el pasado debe estar expirado")
    void isTokenExpired_tokenExpirado_retornaTrue() {
        JwtUtil jwtUtilExpirado = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtilExpirado, "secret", "syncro-secret-key-para-test-minimo-32-chars!!");
        ReflectionTestUtils.setField(jwtUtilExpirado, "expiration", -1000L);

        String tokenExpirado = jwtUtilExpirado.generateToken(usuarioMock);
        assertThatThrownBy(() -> jwtUtilExpirado.isTokenExpired(tokenExpirado))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("getExpiration - debe retornar el tiempo configurado")
    void getExpiration_retornaValorConfigurado() {
        assertThat(jwtUtil.getExpiration()).isEqualTo(86400000L);
    }
}
