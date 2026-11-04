package ar.tfg.expedientes.web;

import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.repositorio.ExpedienteRepository;
import ar.tfg.expedientes.seguridad.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/** Menú principal según el rol (CU01, paso 5) con los expedientes que el usuario puede consultar. */
@Controller
public class InicioController {

    private final ExpedienteRepository expedientes;
    private final Permisos permisos;

    public InicioController(ExpedienteRepository expedientes, Permisos permisos) {
        this.expedientes = expedientes;
        this.permisos = permisos;
    }

    @GetMapping("/")
    public String inicio(@AuthenticationPrincipal UsuarioAutenticado actual, Model modelo) {
        Usuario usuario = permisos.usuario(actual);
        List<Expediente> lista = switch (usuario.getRol()) {
            case CLIENTE -> expedientes.buscarPorUsuarioCliente(usuario.getId());
            case GESTOR -> expedientes.buscarPorGestor(usuario.getId());
            case ADMINISTRADOR, ADMINISTRATIVO -> expedientes.buscarTodos();
        };
        modelo.addAttribute("expedientes", lista);
        return "inicio";
    }
}
