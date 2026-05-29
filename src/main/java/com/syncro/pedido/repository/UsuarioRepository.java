package com.syncro.pedido.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncro.pedido.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Usuario.
 *
 * Spring Security necesita este repositorio para cargar el usuario durante la
 * autenticación JWT. El flujo es: 
 * 1. El cliente envía el token JWT en el header
 * Authorization 
 * 2. JwtAuthFilter extrae el email del token 
 * 3. Llama a
 * findByEmail() para cargar el usuario desde la BD 
 * 4. Verifica que el token sea
 * válido para ese usuario
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su email. Es el método principal que usa Spring
     * Security para autenticar. Devuelve Optional porque el usuario puede no
     * existir (en ese caso, el login fallará con 401).
     *
     * Spring traduce este nombre a: SELECT * FROM usuario WHERE email = ?
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Verifica si ya existe un usuario registrado con ese email. Se usa en el
     * registro para evitar emails duplicados antes de intentar guardar en la
     * BD.
     *
     * Spring traduce este nombre a: SELECT COUNT(*) > 0 FROM usuario WHERE
     * email = ?
     */
    boolean existsByEmail(String email);

    /**
     * Busca todos los usuarios activos de una empresa. Útil para que un ADMIN
     * vea quién trabaja en su organización.
     *
     * Spring traduce este nombre a: SELECT * FROM usuario WHERE empresa_id = ?
     * AND activo = ?
     */
    List<Usuario> findByEmpresa_IdAndActivo(Long empresaId, Boolean activo);

    /**
     * Busca todos los usuarios de una empresa sin filtrar por estado. Usado en
     * reportes administrativos.
     *
     * Spring traduce este nombre a: SELECT * FROM usuario WHERE empresa_id = ?
     */
    List<Usuario> findByEmpresa_Id(Long empresaId);

}
