package com.proyecto.auth.config;

import com.proyecto.auth.domain.entity.MenuItem;
import com.proyecto.auth.domain.entity.Permission;
import com.proyecto.auth.domain.entity.Role;
import com.proyecto.auth.domain.entity.User;
import com.proyecto.auth.domain.enums.PermissionName;
import com.proyecto.auth.domain.enums.RoleName;
import com.proyecto.auth.repository.MenuItemRepository;
import com.proyecto.auth.repository.PermissionRepository;
import com.proyecto.auth.repository.RoleRepository;
import com.proyecto.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Siembra la base de datos al arrancar la aplicacion.
 *
 * <p>Crea los permisos, los roles con sus permisos y un usuario administrador inicial,
 * de forma <b>idempotente</b>: puede ejecutarse mil veces sin duplicar nada, porque
 * siempre comprueba antes si el registro ya existe. Esto es imprescindible cuando la
 * aplicacion se reinicia en EC2 o corre en varias instancias.</p>
 *
 * <p>Sin este sembrador no habria forma de entrar por primera vez al sistema: el
 * registro publico solo concede {@code ROLE_USER}, nunca permisos de administrador.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminSeedProperties.class)
public class DataSeeder implements ApplicationRunner {

    /** Textos descriptivos de cada permiso, para que el panel no muestre solo codigos. */
    private static final Map<PermissionName, String> PERMISSION_DESCRIPTIONS = Map.ofEntries(
            Map.entry(PermissionName.USER_READ, "Consultar usuarios"),
            Map.entry(PermissionName.USER_CREATE, "Crear usuarios"),
            Map.entry(PermissionName.USER_UPDATE, "Modificar usuarios"),
            Map.entry(PermissionName.USER_DELETE, "Eliminar usuarios"),
            Map.entry(PermissionName.ROLE_READ, "Consultar roles"),
            Map.entry(PermissionName.ROLE_WRITE, "Crear y modificar roles"),
            Map.entry(PermissionName.ROLE_ASSIGN, "Asignar roles a usuarios"),
            Map.entry(PermissionName.PERMISSION_READ, "Consultar el catalogo de permisos"),
            Map.entry(PermissionName.PERMISSION_WRITE, "Crear permisos de interfaz"),
            Map.entry(PermissionName.MENU_WRITE, "Gestionar el menu de navegacion"),
            Map.entry(PermissionName.FILE_UPLOAD, "Subir archivos al almacenamiento S3"),
            Map.entry(PermissionName.FILE_READ, "Listar y descargar archivos de S3"),
            Map.entry(PermissionName.FILE_DELETE, "Eliminar archivos de S3"));

    /** Textos descriptivos de cada rol. */
    private static final Map<RoleName, String> ROLE_DESCRIPTIONS = Map.of(
            RoleName.ROLE_ADMIN, "Administrador con control total del sistema",
            RoleName.ROLE_MANAGER, "Gestor de usuarios y archivos",
            RoleName.ROLE_USER, "Usuario estandar con acceso basico");

    private final PermissionRepository permissionRepository;
    private final MenuItemRepository menuItemRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties adminProperties;

    /**
     * Punto de entrada que Spring Boot ejecuta una vez la aplicacion esta lista.
     *
     * @param args argumentos de linea de comandos (no se usan)
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPermissions();
        seedRoles();
        seedAdminUser();
        seedMenuItems();
    }

    /**
     * Crea las entradas de menu basicas la primera vez que arranca la aplicacion.
     *
     * <p>Se marcan como {@code systemItem} para que no se puedan eliminar: sin "Inicio"
     * ni "Cuenta" la aplicacion quedaria sin navegacion y habria que arreglarlo por SQL.
     * Si se pueden renombrar, reordenar y ocultar desde el panel.</p>
     *
     * <p>Solo se insertan si faltan, comprobando por ruta. Asi un reinicio no duplica
     * nada ni revierte los cambios que haya hecho un administrador.</p>
     */
    private void seedMenuItems() {
        record DefaultItem(String label, String path, String icon, String permission, int order) {
        }

        List<DefaultItem> defaults = List.of(
                new DefaultItem("Inicio", "/panel", "dashboard", null, 1),
                new DefaultItem("Usuarios", "/panel/usuarios", "users",
                        PermissionName.USER_READ.getAuthority(), 2),
                new DefaultItem("Roles", "/panel/roles", "shield",
                        PermissionName.ROLE_READ.getAuthority(), 3),
                new DefaultItem("Menú", "/panel/menu", "menu",
                        PermissionName.MENU_WRITE.getAuthority(), 4),
                new DefaultItem("Cuenta", "/panel/perfil", "user", null, 5));

        for (DefaultItem item : defaults) {
            var existing = menuItemRepository.findByPath(item.path());

            if (existing.isPresent()) {
                // Bases de datos creadas antes de que existieran los iconos: se les pone
                // el suyo sin tocar nada de lo que el administrador haya cambiado.
                MenuItem current = existing.get();
                if (current.getIcon() == null) {
                    current.setIcon(item.icon());
                    menuItemRepository.save(current);
                }
                continue;
            }

            menuItemRepository.save(MenuItem.builder()
                    .label(item.label())
                    .path(item.path())
                    .icon(item.icon())
                    .requiredPermission(item.permission())
                    .displayOrder(item.order())
                    .visible(true)
                    .systemItem(true)
                    .build());
            log.info("Entrada de menu creada: {}", item.label());
        }
    }

    /**
     * Inserta los permisos del catalogo que todavia no existan en la base de datos.
     */
    private void seedPermissions() {
        for (PermissionName permissionName : PermissionName.values()) {
            if (permissionRepository.existsByName(permissionName.getAuthority())) {
                continue;
            }
            permissionRepository.save(Permission.builder()
                    .name(permissionName.getAuthority())
                    .description(PERMISSION_DESCRIPTIONS.getOrDefault(permissionName, permissionName.name()))
                    .build());
            log.info("Permiso creado: {}", permissionName);
        }
    }

    /**
     * Crea los roles que falten y sincroniza los permisos de todos ellos.
     *
     * <p>La sincronizacion se hace siempre, no solo al crear: asi, si en una version
     * futura se anade un permiso nuevo a {@code ROLE_ADMIN}, el despliegue lo aplica
     * solo, sin ejecutar SQL a mano en el servidor.</p>
     */
    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            Set<String> permissionNames = roleName.getPermissions().stream()
                    .map(PermissionName::getAuthority)
                    .collect(Collectors.toSet());

            Set<Permission> permissions = permissionRepository.findByNameIn(permissionNames);

            Role role = roleRepository.findByName(roleName.getAuthority())
                    .orElseGet(() -> Role.builder()
                            .name(roleName.getAuthority())
                            .description(ROLE_DESCRIPTIONS.get(roleName))
                            .build());

            role.setPermissions(new HashSet<>(permissions));
            roleRepository.save(role);
        }
        log.info("Roles sincronizados: {} roles disponibles", roleRepository.count());
    }

    /**
     * Crea el usuario administrador inicial si todavia no existe ninguno con ese nombre.
     *
     * <p>Si el usuario ya existe no se toca su contrasena, para no revertir un cambio
     * que la persona administradora haya hecho desde la aplicacion.</p>
     */
    private void seedAdminUser() {
        if (!adminProperties.isEnabled()) {
            return;
        }
        if (userRepository.existsByUsername(adminProperties.getUsername())) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe ROLE_ADMIN: la siembra de roles fallo"));

        User admin = User.builder()
                .username(adminProperties.getUsername())
                .email(adminProperties.getEmail())
                .password(passwordEncoder.encode(adminProperties.getPassword()))
                .firstName("Administrador")
                .lastName("del Sistema")
                .enabled(true)
                .build();
        admin.addRole(adminRole);

        userRepository.save(admin);

        log.warn("=================================================================");
        log.warn(" Usuario administrador creado: {}", adminProperties.getUsername());
        log.warn(" Cambia su contrasena en el primer inicio de sesion.");
        log.warn("=================================================================");
    }
}
