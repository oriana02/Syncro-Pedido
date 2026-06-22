package com.syncro.pedido.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.io.Serializable;

/**
 * Representa a los operadores de una empresa que usan Syncro. Implementa
 * UserDetails para integrarse con Spring Security y permitir la autenticación
 * mediante JWT. Tabla: usuario
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Clave primaria generada por la secuencia de MYSQL
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Empresa a la que pertenece este usuario. Un usuario siempre está asociado
     * a una sola empresa.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /**
     * Nombre completo del operador
     */
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Email del usuario, se usa como nombre de usuario para el login
     */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Contraseña cifrada con BCrypt. NUNCA se almacena la contraseña en texto
     * plano. Columna: password_hash
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    /**
     * Rol del usuario dentro del sistema: - ADMIN: acceso total - OPERADOR:
     * gestión de pedidos - BODEGUERO: solo actualización de estados
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Rol rol = Rol.OPERADOR;

    /**
     * Indica si el usuario puede iniciar sesión. false = cuenta desactivada
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /**
     * Fecha en que se creó la cuenta
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // ─── Métodos requeridos por Spring Security ────────────────────────────────
    /**
     * Devuelve el rol del usuario como autoridad de Spring Security
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    /**
     * Spring Security usa el email como identificador único del usuario
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * La cuenta no vence (se controla con el campo activo)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * La cuenta se bloquea solo cuando activo = false
     */
    @Override
    public boolean isAccountNonLocked() {
        return activo;
    }

    /**
     * Las credenciales no vencen (se controla con el token JWT)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * El usuario puede iniciar sesión solo si está activo
     */
    @Override
    public boolean isEnabled() {
        return isAccountNonLocked();
    }

}
