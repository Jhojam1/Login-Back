package com.proyecto.auth.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.auth.config.LambdaProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprueba el JSON exacto que recibe la funcion Lambda.
 *
 * <p>Es la frontera entre dos equipos: si los nombres de los campos no coinciden con
 * los que lee la funcion en Python, el correo nunca sale y el error aparece lejos de
 * su causa. Esta prueba fija el contrato para que un cambio accidental se note aqui.</p>
 */
class LambdaPayloadTest {

    @Test
    @DisplayName("El JSON enviado usa los campos 'correo' y 'codigo'")
    void elPayloadUsaLosCamposAcordados() {
        LambdaClient cliente = mock(LambdaClient.class);
        when(cliente.invoke(any(InvokeRequest.class)))
                .thenReturn(InvokeResponse.builder().statusCode(200).build());

        LambdaProperties propiedades = new LambdaProperties();
        var sender = new LambdaVerificationCodeSender(cliente, propiedades, new ObjectMapper());

        sender.send("jhojam@correo.com", "483920");

        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(cliente).invoke(captor.capture());

        InvokeRequest peticion = captor.getValue();
        String payload = peticion.payload().asString(StandardCharsets.UTF_8);

        assertThat(payload).isEqualTo("{\"correo\":\"jhojam@correo.com\",\"codigo\":\"483920\"}");
        assertThat(peticion.functionName())
                .isEqualTo("arn:aws:lambda:us-east-2:366101997342:function:lambda-envio-codigos");
        assertThat(peticion.invocationTypeAsString()).isEqualTo("RequestResponse");
    }

    @Test
    @DisplayName("Si la funcion devuelve un error, el envio se da por fallido")
    void unErrorDeLaFuncionSePropaga() {
        LambdaClient cliente = mock(LambdaClient.class);
        when(cliente.invoke(any(InvokeRequest.class)))
                .thenReturn(InvokeResponse.builder()
                        .statusCode(200)
                        .functionError("Unhandled")
                        .payload(SdkBytes.fromUtf8String("{\"errorMessage\":\"boom\"}"))
                        .build());

        var sender = new LambdaVerificationCodeSender(
                cliente, new LambdaProperties(), new ObjectMapper());

        assertThat(org.assertj.core.api.Assertions
                .catchThrowable(() -> sender.send("a@b.com", "123456")))
                .hasMessageContaining("No se pudo enviar el codigo");
    }
}
