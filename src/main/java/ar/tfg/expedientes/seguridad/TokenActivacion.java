package ar.tfg.expedientes.seguridad;

import ar.tfg.expedientes.config.PropiedadesApp;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.repositorio.UsuarioRepository;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Enlace de activación de cuenta (política 2): vence a las 24 horas y es de un solo uso.
 * El token se firma con HMAC-SHA256 incluyendo el hash de contraseña vigente, por lo que
 * deja de ser válido en cuanto el usuario define su contraseña, sin guardar datos adicionales.
 */
@Component
public class TokenActivacion {

    static final Duration VIGENCIA = Duration.ofHours(24);

    private final byte[] secreto;
    private final UsuarioRepository usuarios;
    private final Clock reloj;

    public TokenActivacion(PropiedadesApp propiedades, UsuarioRepository usuarios, Clock reloj) {
        this.secreto = propiedades.tokenSecreto().getBytes(StandardCharsets.UTF_8);
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    public String generar(Usuario usuario) {
        long vence = Instant.now(reloj).plus(VIGENCIA).getEpochSecond();
        String datos = usuario.getId() + ":" + vence;
        String token = datos + ":" + firmar(datos, usuario.getPasswordHash());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Usuario> verificar(String token) {
        try {
            String[] partes = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8).split(":");
            if (partes.length != 3) {
                return Optional.empty();
            }
            long id = Long.parseLong(partes[0]);
            long vence = Long.parseLong(partes[1]);
            if (Instant.now(reloj).getEpochSecond() > vence) {
                return Optional.empty();
            }
            return usuarios.findById(id)
                    .filter(Usuario::isActivo)
                    .filter(u -> MessageDigest.isEqual(
                            firmar(partes[0] + ":" + partes[1], u.getPasswordHash()).getBytes(StandardCharsets.UTF_8),
                            partes[2].getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String firmar(String datos, String hashContrasena) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            byte[] firma = mac.doFinal((datos + ":" + hashContrasena).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(firma);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }
}
