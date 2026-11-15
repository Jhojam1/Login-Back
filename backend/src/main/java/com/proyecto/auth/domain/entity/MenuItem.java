package com.proyecto.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entrada del menu de navegacion.
 *
 * <p>Vive en la base de datos y no en el codigo del frontend porque es <b>configuracion,
 * no logica</b>: que secciones aparecen, en que orden y quien las ve es algo que un
 * administrador deberia poder cambiar sin recompilar ni volver a despegar la aplicacion.</p>
 *
 * <p>Esto funciona precisamente porque el filtrado del frontend es generico: compara el
 * permiso exigido por la entrada con la lista de permisos que trae el token, sea cual sea
 * su nombre. Por eso una entrada nueva, con un permiso nuevo, aparece sin tocar una sola
 * linea de React.</p>
 */
@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = {"id", "label", "path"})
public class MenuItem extends Auditable {

    /** Identificador tecnico generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Texto que se muestra en el menu, por ejemplo "Reportes". */
    @Column(name = "label", nullable = false, length = 60)
    private String label;

    /** Ruta de la aplicacion a la que lleva, por ejemplo {@code /panel/reportes}. */
    @Column(name = "path", nullable = false, length = 180)
    private String path;

    /**
     * Nombre del icono que se dibuja junto al texto, por ejemplo {@code users}.
     *
     * <p>Se guarda un identificador corto y no el dibujo: el frontend tiene un catalogo
     * cerrado de iconos y traduce este nombre. Si llega uno desconocido, usa uno neutro.</p>
     */
    @Column(name = "icon", length = 40)
    private String icon;

    /**
     * Nombre del permiso necesario para ver la entrada.
     *
     * <p>Si es {@code null}, la entrada la ve cualquier persona autenticada.</p>
     *
     * <p>Se guarda como texto y no como clave externa a proposito: el menu no deberia
     * romperse por un problema de integridad referencial, y el servicio ya comprueba al
     * guardar que el permiso exista y al borrar un permiso que ninguna entrada lo use.</p>
     */
    @Column(name = "required_permission", length = 60)
    private String requiredPermission;

    /** Posicion en el menu; los numeros mas bajos salen primero. */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    /** Si esta en {@code false}, la entrada no se muestra a nadie sin llegar a borrarla. */
    @Column(name = "visible", nullable = false)
    @Builder.Default
    private boolean visible = true;

    /**
     * Marca las entradas que crea el sistema al arrancar.
     *
     * <p>No se pueden eliminar, porque borrar "Inicio" o "Cuenta" dejaria la aplicacion
     * sin navegacion basica y sin forma evidente de recuperarla desde la propia interfaz.
     * Si, se pueden renombrar, reordenar y ocultar.</p>
     */
    @Column(name = "system_item", nullable = false)
    @Builder.Default
    private boolean systemItem = false;
}
