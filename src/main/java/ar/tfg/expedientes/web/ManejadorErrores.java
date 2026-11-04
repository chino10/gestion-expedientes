package ar.tfg.expedientes.web;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@ControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(AccesoDenegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accesoDenegado(AccesoDenegadoException excepcion, Model modelo) {
        modelo.addAttribute("mensaje", excepcion.getMessage());
        modelo.addAttribute("actual", DatosComunesVista.usuarioActual());
        return "acceso-denegado";
    }

    @ExceptionHandler({NoSuchElementException.class, NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(Model modelo) {
        modelo.addAttribute("mensaje", "El recurso solicitado no existe.");
        modelo.addAttribute("actual", DatosComunesVista.usuarioActual());
        return "acceso-denegado";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public String archivoGrande(Model modelo) {
        modelo.addAttribute("mensaje", "El archivo supera el tamaño máximo de 10 MB.");
        modelo.addAttribute("actual", DatosComunesVista.usuarioActual());
        return "acceso-denegado";
    }
}
