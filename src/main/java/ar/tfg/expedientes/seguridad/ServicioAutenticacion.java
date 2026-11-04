package ar.tfg.expedientes.seguridad;

import ar.tfg.expedientes.config.PropiedadesApp;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.repositorio.UsuarioRepository;
import ar.tfg.expedientes.servicio.AccionTrazabilidad;
import ar.tfg.expedientes.servicio.TrazabilidadService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesión (CU01): busca al usuario por su correo, verifica la contraseña cifrada,
 * cuenta los intentos fallidos y bloquea la cuenta al alcanzar el máximo (políticas 5 y 6).
 * El mensaje de error nunca indica si el dato incorrecto es el correo o la contraseña.
 */
@Component
public class ServicioAutenticacion implements AuthenticationProvider {

    static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final TrazabilidadService trazabilidad;
    private final int maximoIntentos;
    private final String hashFicticio;

    public ServicioAutenticacion(UsuarioRepository usuarios, PasswordEncoder codificador,
                                 TrazabilidadService trazabilidad, PropiedadesApp propiedades) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.trazabilidad = trazabilidad;
        this.maximoIntentos = propiedades.maxIntentosFallidos();
        // Se usa para que la respuesta demore lo mismo exista o no el correo ingresado.
        this.hashFicticio = codificador.encode("contrasena-ficticia-para-igualar-tiempos");
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationException.class)
    public Authentication authenticate(Authentication solicitud) {
        String email = solicitud.getName() == null ? "" : solicitud.getName().trim();
        String clave = solicitud.getCredentials() == null ? "" : solicitud.getCredentials().toString();

        Usuario usuario = usuarios.buscarPorEmail(email).orElse(null);
        if (usuario == null || !usuario.isActivo()) {
            codificador.matches(clave, hashFicticio);
            throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
        }
        if (usuario.estaBloqueado(maximoIntentos)) {
            throw new LockedException("Cuenta bloqueada.");
        }
        if (!usuario.verificarClave(clave, codificador)) {
            usuario.registrarIntentoFallido();
            usuarios.guardar(usuario);
            if (usuario.estaBloqueado(maximoIntentos)) {
                trazabilidad.registrarEvento(usuario, null, AccionTrazabilidad.BLOQUEO_CUENTA,
                        "Cuenta bloqueada tras " + maximoIntentos + " intentos fallidos consecutivos.");
                throw new LockedException("Cuenta bloqueada.");
            }
            throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
        }
        usuario.reiniciarIntentos();
        usuarios.guardar(usuario);
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> tipo) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
    }
}
