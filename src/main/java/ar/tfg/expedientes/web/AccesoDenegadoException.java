package ar.tfg.expedientes.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** El usuario no tiene permiso sobre el expediente solicitado. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException() {
        super("No tiene permiso para acceder a este expediente.");
    }
}
