package ar.tfg.expedientes.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantalla de inicio de sesión (CU01). La verificación de las credenciales la realiza
 * {@link ar.tfg.expedientes.seguridad.ServicioAutenticacion} al recibir POST /login.
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }
}
