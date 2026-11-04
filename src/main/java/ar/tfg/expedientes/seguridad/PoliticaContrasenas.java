package ar.tfg.expedientes.seguridad;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Política de contraseñas (política 3 del apartado Seguridad): entre 15 y 64 caracteres,
 * hasta 72 bytes, sin reglas de composición y rechazo de las contraseñas de uso frecuente.
 * La lista de contraseñas frecuentes (10.000 entradas) proviene del proyecto SecLists
 * (archivo Passwords/Common-Credentials/10k-most-common.txt, licencia MIT).
 */
@Component
public class PoliticaContrasenas {

    static final int MINIMO = 15;
    static final int MAXIMO = 64;
    static final int MAXIMO_BYTES = 72;

    private final Set<String> frecuentes;

    public PoliticaContrasenas() {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new ClassPathResource("contrasenas-comunes.txt").getInputStream(), StandardCharsets.UTF_8))) {
            this.frecuentes = lector.lines()
                    .map(l -> l.trim().toLowerCase(Locale.ROOT))
                    .filter(l -> !l.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la lista de contraseñas frecuentes", e);
        }
    }

    public List<String> validar(String clave) {
        List<String> errores = new ArrayList<>();
        String valor = clave == null ? "" : clave;
        int caracteres = valor.codePointCount(0, valor.length());
        if (caracteres < MINIMO) {
            errores.add("La contraseña debe tener al menos " + MINIMO + " caracteres.");
        }
        if (caracteres > MAXIMO) {
            errores.add("La contraseña no puede superar los " + MAXIMO + " caracteres.");
        }
        if (valor.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            errores.add("La contraseña no puede superar los " + MAXIMO_BYTES + " bytes.");
        }
        if (frecuentes.contains(valor.toLowerCase(Locale.ROOT))) {
            errores.add("La contraseña figura entre las de uso más frecuente.");
        }
        return errores;
    }

    int cantidadFrecuentes() {
        return frecuentes.size();
    }
}
