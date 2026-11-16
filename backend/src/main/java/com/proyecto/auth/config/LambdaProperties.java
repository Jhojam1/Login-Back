package com.proyecto.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos de la funcion Lambda que envia los codigos por correo, bajo {@code app.lambda}.
 *
 * <p>Los valores vienen ya fijados con los de la funcion del proyecto, asi que no hay
 * que configurar nada para que funcione. Siguen siendo propiedades (y no constantes)
 * para poder cambiarlos sin recompilar si la funcion se mueve o se renombra.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.lambda")
public class LambdaProperties {

    /**
     * Si esta en {@code false}, no se llama a AWS: el codigo se escribe en el log.
     *
     * <p>Activado por defecto. El perfil {@code dev} lo apaga, porque en un portatil
     * normalmente no hay credenciales de AWS configuradas.</p>
     */
    private boolean enabled = true;

    /** ARN completo de la funcion que envia los correos. */
    private String functionName =
            "arn:aws:lambda:us-east-2:366101997342:function:lambda-envio-codigos";

    /**
     * Region donde vive la funcion.
     *
     * <p>No tiene por que coincidir con la del EC2: la funcion esta en {@code us-east-2}
     * aunque el servidor se despliegue en otra region.</p>
     */
    private String region = "us-east-2";

    /** Nombre del campo del JSON donde viaja el correo, tal como lo lee la funcion. */
    private String emailField = "correo";

    /** Nombre del campo del JSON donde viaja el codigo. */
    private String codeField = "codigo";

    /** Segundos de espera antes de dar la llamada por fallida. */
    private int timeoutSeconds = 10;
}
