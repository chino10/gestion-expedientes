package ar.tfg.expedientes.web;

import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.repositorio.UsuarioRepository;
import ar.tfg.expedientes.seguridad.PoliticaContrasenas;
import ar.tfg.expedientes.seguridad.TokenActivacion;
import ar.tfg.expedientes.servicio.AccionTrazabilidad;
import ar.tfg.expedientes.servicio.TrazabilidadService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Activación de la cuenta mediante el enlace de un solo uso (política 2) y definición de la contraseña. */
@Controller
public class ActivacionController {

    private final TokenActivacion tokens;
    private final PoliticaContrasenas politica;
    private final PasswordEncoder codificador;
    private final UsuarioRepository usuarios;
    private final TrazabilidadService trazabilidad;

    public ActivacionController(TokenActivacion tokens, PoliticaContrasenas politica, PasswordEncoder codificador,
                                UsuarioRepository usuarios, TrazabilidadService trazabilidad) {
        this.tokens = tokens;
        this.politica = politica;
        this.codificador = codificador;
        this.usuarios = usuarios;
        this.trazabilidad = trazabilidad;
    }

    @GetMapping("/activar")
    public String formulario(@RequestParam(required = false) String token, Model modelo) {
        Optional<Usuario> usuario = token == null ? Optional.empty() : tokens.verificar(token);
        modelo.addAttribute("token", token);
        modelo.addAttribute("valido", usuario.isPresent());
        usuario.ifPresent(u -> modelo.addAttribute("email", u.getEmail()));
        return "activar";
    }

    @PostMapping("/activar")
    @Transactional
    public String activar(@RequestParam String token, @RequestParam String clave,
                          @RequestParam String confirmacion, Model modelo) {
        Optional<Usuario> encontrado = tokens.verificar(token);
        if (encontrado.isEmpty()) {
            modelo.addAttribute("valido", false);
            return "activar";
        }
        Usuario usuario = encontrado.get();
        List<String> errores = new ArrayList<>(politica.validar(clave));
        if (!clave.equals(confirmacion)) {
            errores.add("La confirmación no coincide con la contraseña.");
        }
        if (!errores.isEmpty()) {
            modelo.addAttribute("token", token);
            modelo.addAttribute("valido", true);
            modelo.addAttribute("email", usuario.getEmail());
            modelo.addAttribute("errores", errores);
            return "activar";
        }
        usuario.cambiarClave(codificador.encode(clave));
        usuario.reiniciarIntentos();
        usuarios.guardar(usuario);
        trazabilidad.registrarEvento(usuario, null, AccionTrazabilidad.ACTIVACION_CUENTA,
                "El usuario definió su contraseña mediante el enlace de activación.");
        return "redirect:/login?activada";
    }
}
