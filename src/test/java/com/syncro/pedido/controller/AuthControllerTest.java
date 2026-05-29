package com.syncro.pedido.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncro.pedido.dto.request.LoginRequest;
import com.syncro.pedido.dto.request.RegisterRequest;
import com.syncro.pedido.dto.response.AuthResponse;
import com.syncro.pedido.model.Rol;
import com.syncro.pedido.security.JwtUtil;
import com.syncro.pedido.service.AuthService;
import com.syncro.pedido.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    value = AuthController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@DisplayName("AuthController - Tests de integración con MockMvc")
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
                .token("eyJhbGciOiJIUzI1NiJ9.test.signature")
                .tipo("Bearer").email("oriana@pyme-demo.cl")
                .nombre("Oriana Solorzano").rol("ADMIN")
                .empresaId(1L).expiresIn(86400000L).build();
    }

    @Test
    @DisplayName("POST /auth/login - credenciales válidas retorna 200 con token")
    @WithMockUser
    void login_credencialesValidas_retorna200ConToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new LoginRequest("oriana@pyme-demo.cl", "Admin1234!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.email").value("oriana@pyme-demo.cl"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /auth/login - credenciales inválidas retorna 401")
    @WithMockUser
    void login_credencialesInvalidas_retorna401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new LoginRequest("oriana@pyme-demo.cl", "wrongpass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login - body vacío retorna 400")
    @WithMockUser
    void login_bodyVacio_retorna400() throws Exception {
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - email inválido retorna 400")
    @WithMockUser
    void login_emailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new LoginRequest("noesEmail", "pass123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - datos válidos retorna 201 Created con token")
    @WithMockUser
    void register_datosValidos_retorna201ConToken() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Sofía Gómez").email("sofia@pyme-demo.cl")
                .password("Admin1234!").empresaId(1L).rol(Rol.OPERADOR).build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/register - email duplicado retorna 400")
    @WithMockUser
    void register_emailDuplicado_retorna400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Dup").email("oriana@pyme-demo.cl")
                .password("Admin1234!").empresaId(1L).build();

        when(authService.register(any())).thenThrow(
                new IllegalArgumentException("Ya existe un usuario con el email: oriana@pyme-demo.cl"));

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - sin empresaId retorna 400")
    @WithMockUser
    void register_sinEmpresaId_retorna400() throws Exception {
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Test\",\"email\":\"t@t.cl\",\"password\":\"pass123\"}"))
                .andExpect(status().isBadRequest());
    }
}
