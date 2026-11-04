package ar.tfg.expedientes.dominio;

/**
 * Se lanza cuando una operación viola una regla del negocio. Su mensaje se muestra al usuario.
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
