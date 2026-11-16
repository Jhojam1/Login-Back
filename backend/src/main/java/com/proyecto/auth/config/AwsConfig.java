package com.proyecto.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

import java.time.Duration;

/**
 * Cliente de AWS Lambda.
 *
 * <p>Solo se crea si {@code app.lambda.enabled=true}; en desarrollo no existe y la
 * aplicacion arranca sin necesidad de credenciales de AWS.</p>
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.lambda.enabled", havingValue = "true")
public class AwsConfig {

    private final LambdaProperties lambdaProperties;

    /**
     * Construye el cliente que invoca la funcion.
     *
     * <p>Las credenciales las resuelve {@code DefaultCredentialsProvider}, que busca
     * por orden en variables de entorno, en {@code ~/.aws/credentials} y, por ultimo,
     * en el rol IAM de la instancia EC2. Esa ultima via es la que se usa en el
     * servidor: <b>no hay que guardar ninguna clave de AWS en el proyecto</b>, basta
     * con asignarle a la instancia un rol con permiso {@code lambda:InvokeFunction}.</p>
     *
     * @return el cliente listo para usarse
     */
    @Bean
    public LambdaClient lambdaClient() {
        Duration timeout = Duration.ofSeconds(lambdaProperties.getTimeoutSeconds());

        return LambdaClient.builder()
                .region(Region.of(lambdaProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClient(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(5))
                        .socketTimeout(timeout)
                        .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        // Un login no puede quedarse esperando indefinidamente: si la
                        // funcion no responde, es mejor fallar rapido y avisar.
                        .apiCallTimeout(timeout)
                        .build())
                .build();
    }
}
