package ar.tfg.expedientes;

import ar.tfg.expedientes.seguridad.PoliticaContrasenas;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Política 3 del apartado Seguridad. */
class PoliticaContrasenasTest {

    private final PoliticaContrasenas politica = new PoliticaContrasenas();

    @Test
    void rechazaContrasenasCortas() {
        assertThat(politica.validar("corta123")).anyMatch(e -> e.contains("al menos 15"));
    }

    @Test
    void rechazaContrasenasFrecuentesYLargas() {
        assertThat(politica.validar("password")).anyMatch(e -> e.contains("uso más frecuente"));
        assertThat(politica.validar("a".repeat(65))).anyMatch(e -> e.contains("64"));
        // 30 caracteres de 3 bytes = 90 bytes: supera el límite de 72 bytes de BCrypt.
        assertThat(politica.validar("€".repeat(30))).anyMatch(e -> e.contains("72 bytes"));
    }

    @Test
    void aceptaFrasesSinReglasDeComposicion() {
        assertThat(politica.validar("una frase larga con espacios")).isEmpty();
    }
}
