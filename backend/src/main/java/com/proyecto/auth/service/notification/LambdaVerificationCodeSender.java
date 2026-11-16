package com.proyecto.auth.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.proyecto.auth.config.LambdaProperties;
import com.proyecto.auth.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.core.exception.SdkException;

import java.nio.charset.StandardCharsets;

/**
 * Envia el codigo invocando la funcion Lambda que otro equipo desplego en AWS.
 *
 * <p>La llamada es sincrona ({@code RequestResponse}): se espera la respuesta para
 * saber si el correo pudo enviarse. Si fuera asincrona, el backend diria "te hemos
 * enviado un codigo" sin tener ni idea de si llego, y la persona se quedaria esperando
 * un correo que quiza nunca salio.</p>
 *
 * <p>Se activa solo con {@code app.lambda.enabled=true}; en caso contrario Spring usa
 * {@link LoggingVerificationCodeSender}.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.lambda.enabled", havingValue = "true")
public class LambdaVerificationCodeSender implements VerificationCodeSender {

    private final LambdaClient lambdaClient;
    private final LambdaProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * @param lambdaClient cliente de AWS ya configurado
     * @param properties   nombre de la funcion y nombres de los campos del JSON
     * @param objectMapper serializador de Jackson que ya usa la aplicacion
     */
    public LambdaVerificationCodeSender(LambdaClient lambdaClient,
                                        LambdaProperties properties,
                                        ObjectMapper objectMapper) {
        this.lambdaClient = lambdaClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Invoca la funcion pasandole el correo y el codigo.
     *
     * @param email correo de la persona
     * @param code  codigo generado
     * @throws BusinessException si AWS rechaza la llamada o la funcion falla
     */
    @Override
    public void send(String email, String code) {
        String payload = buildPayload(email, code);

        try {
            InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                    .functionName(properties.getFunctionName())
                    .invocationType(InvocationType.REQUEST_RESPONSE)
                    .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                    .build());

            // Una Lambda puede responder 200 y aun asi haber fallado por dentro: en ese
            // caso AWS marca functionError y el cuerpo trae la traza de Python. Sin esta
            // comprobacion daríamos por enviado un correo que nunca salio.
            if (response.functionError() != null) {
                String detalle = response.payload().asUtf8String();
                log.error("La funcion Lambda devolvio un error: {}", detalle);
                throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SEND_FAILED",
                        "No se pudo enviar el codigo por correo. Intentalo de nuevo.");
            }

            log.info("Codigo de verificacion enviado a {}", enmascarar(email));

        } catch (BusinessException ex) {
            // Ya es un error nuestro con su mensaje: se deja pasar tal cual.
            throw ex;
        } catch (SdkException ex) {
            // SdkException cubre todo lo que puede salir mal con AWS: credenciales que
            // no se encuentran, red caida, permiso lambda:InvokeFunction ausente, region
            // equivocada o nombre de funcion inexistente. Capturar solo LambdaException
            // dejaba escapar los fallos de credenciales como un error 500 sin explicar.
            log.error("No se pudo invocar la funcion Lambda '{}' en {}: {}",
                    properties.getFunctionName(), properties.getRegion(), ex.getMessage());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SEND_FAILED",
                    "No se pudo enviar el codigo por correo. Intentalo de nuevo.");
        }
    }

    /**
     * Construye el JSON que recibe la funcion.
     *
     * <p>Los nombres de los dos campos salen de la configuracion porque los decide
     * quien escribio la funcion en Python. Si alli se lee {@code event["correo"]},
     * basta cambiar una variable de entorno en lugar de recompilar.</p>
     *
     * @param email correo de la persona
     * @param code  codigo generado
     * @return el cuerpo de la invocacion, por ejemplo {@code {"email":"...","codigo":"123456"}}
     */
    private String buildPayload(String email, String code) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(properties.getEmailField(), email);
        node.put(properties.getCodeField(), code);
        return node.toString();
    }

    /**
     * Oculta la mayor parte del correo para no dejarlo entero en los registros.
     *
     * @param email correo completo
     * @return el correo parcialmente tapado, por ejemplo {@code jh***@correo.com}
     */
    private String enmascarar(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 2) {
            return "***" + email.substring(Math.max(arroba, 0));
        }
        return email.substring(0, 2) + "***" + email.substring(arroba);
    }
}
