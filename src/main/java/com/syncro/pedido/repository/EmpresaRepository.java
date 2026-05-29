package com.syncro.pedido.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncro.pedido.model.Empresa;

import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad Empresa.
 *
 * Las empresas representan a las PYMEs que usan Syncro. 
 * Este repositorio se usa principalmente para: 
 * - Validar que una empresa existe antes de crear un
 * pedido 
 * - Registrar nuevas empresas en el sistema 
 * - Buscar empresa por RUT o email al momento del registro
 */
@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    /**
     * Busca una empresa por su RUT. Se usa durante el registro para evitar
     * empresas duplicadas.
     *
     * Spring traduce este nombre a: SELECT * FROM empresa WHERE rut = ?
     */
    Optional<Empresa> findByRut(String rut);

    /**
     * Busca una empresa por su email de contacto. Se usa durante el registro
     * para evitar emails duplicados.
     *
     * Spring traduce este nombre a: SELECT * FROM empresa WHERE email = ?
     */
    Optional<Empresa> findByEmail(String email);

    /**
     * Verifica si ya existe una empresa registrada con ese RUT. Se llama antes
     * de guardar una nueva empresa para dar un mensaje de error claro al
     * usuario.
     *
     * Spring traduce este nombre a: SELECT COUNT(*) > 0 FROM empresa WHERE rut
     * = ?
     */
    boolean existsByRut(String rut);

    /**
     * Verifica si ya existe una empresa registrada con ese email.
     *
     * Spring traduce este nombre a: SELECT COUNT(*) > 0 FROM empresa WHERE
     * email = ?
     */
    boolean existsByEmail(String email);

}
