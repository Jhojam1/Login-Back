package com.proyecto.auth.service.notification;

/**
 * Envia a una persona el codigo de verificacion de su inicio de sesion.
 *
 * <p>Es una interfaz con dos implementaciones intercambiables: una que llama a la
 * funcion Lambda de AWS y otra que solo escribe el codigo en el log para desarrollo.
 * El servicio de autenticacion no sabe cual esta activa, asi que probar en local no
 * exige tocar ni una linea de su codigo.</p>
 */
public interface VerificationCodeSender {

    /**
     * Hace llegar el codigo a su destinatario.
     *
     * @param email correo de la persona
     * @param code  codigo recien generado, en claro
     * @throws com.proyecto.auth.exception.BusinessException si el envio falla
     */
    void send(String email, String code);
}
