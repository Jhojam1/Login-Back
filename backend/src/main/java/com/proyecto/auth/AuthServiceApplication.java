package com.proyecto.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del servicio de autenticacion.
 *
 * <p>Arranca el servidor web embebido (Tomcat), el contexto de Spring y la conexion a
 * la base de datos. En el servidor EC2 este mismo programa se ejecuta como un servicio
 * de systemd mediante el JAR generado por Maven.</p>
 */
@SpringBootApplication
public class AuthServiceApplication {

    /**
     * Lanza la aplicacion.
     *
     * @param args argumentos de linea de comandos, por ejemplo
     *             {@code --spring.profiles.active=prod}
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
