package com.proyecto.auth.domain.enums;

/**
 * Catálogo de permisos (autorizaciones de grano fino) que existen en el sistema.
 *
 * <p>Un permiso representa una acción concreta sobre un recurso, con el formato
 * {@code RECURSO_ACCION}. Los roles agrupan permisos; los usuarios reciben roles.
 * Usar un enum (en lugar de texto libre) evita errores de tipeo y permite que el
 * compilador detecte permisos inexistentes.</p>
 *
 * <p>En la base de datos se persisten en la tabla {@code permissions} mediante el
 * sembrador de datos inicial, de modo que las consultas siguen siendo dinámicas.</p>
 *
 * <p><b>Este enum lista solo los permisos que el backend verifica.</b> Existe un segundo
 * tipo de permiso, el de <i>solo interfaz</i>, que no aparece aquí: se crea desde el
 * panel, vive únicamente en la base de datos y sirve para que el frontend decida qué
 * mostrar. La diferencia importa: un permiso de solo interfaz no protege nada en el
 * servidor, así que nunca debe usarse para autorizar una operación.</p>
 */
public enum PermissionName {

    /** Listar y ver el detalle de usuarios. */
    USER_READ,
    /** Crear nuevos usuarios desde el panel de administración. */
    USER_CREATE,
    /** Modificar datos de un usuario existente. */
    USER_UPDATE,
    /** Eliminar (o deshabilitar) un usuario. */
    USER_DELETE,

    /** Consultar los roles disponibles. */
    ROLE_READ,
    /** Crear o modificar roles y su conjunto de permisos. */
    ROLE_WRITE,
    /** Asignar o quitar roles a un usuario. */
    ROLE_ASSIGN,

    /** Consultar el catálogo de permisos. */
    PERMISSION_READ,
    /** Crear o eliminar permisos de solo interfaz. */
    PERMISSION_WRITE,

    /** Gestionar las entradas del menú de navegación. */
    MENU_WRITE,

    /** Subir archivos (permiso consumido por el microservicio de S3). */
    FILE_UPLOAD,
    /** Descargar o listar archivos (permiso consumido por el microservicio de S3). */
    FILE_READ,
    /** Borrar archivos (permiso consumido por el microservicio de S3). */
    FILE_DELETE;

    /**
     * Devuelve el nombre del permiso tal como se almacena en la base de datos
     * y como viaja dentro del token JWT.
     *
     * @return identificador textual del permiso, por ejemplo {@code USER_READ}
     */
    public String getAuthority() {
        return name();
    }
}
