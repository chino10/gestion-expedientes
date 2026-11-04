package ar.tfg.expedientes.web;

import ar.tfg.expedientes.seguridad.UsuarioAutenticado;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Agrega a todas las vistas el usuario que inició sesión ("actual"), que el encabezado usa
 * para mostrar su nombre y las opciones de menú de su rol (CU01, paso 5).
 */
@ControllerAdvice
public class DatosComunesVista {

    @ModelAttribute("actual")
    public UsuarioAutenticado actual() {
        return usuarioActual();
    }

    static UsuarioAutenticado usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario
                ? usuario : null;
    }
}
