package ar.tfg.expedientes;

import ar.tfg.expedientes.dominio.Completo;
import ar.tfg.expedientes.dominio.EnRevision;
import ar.tfg.expedientes.dominio.EstadoExpediente;
import ar.tfg.expedientes.dominio.Iniciado;
import ar.tfg.expedientes.dominio.Presentado;
import ar.tfg.expedientes.dominio.Resuelto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Transiciones del patrón State (CU09, comentarios de la Tabla 16). */
class EstadoExpedienteTest {

    @Test
    void cadaEstadoAdmiteSoloSusTransiciones() {
        assertThat(Iniciado.INSTANCIA.destinosPermitidos()).containsExactly(EnRevision.INSTANCIA);
        assertThat(EnRevision.INSTANCIA.destinosPermitidos()).containsExactly(Completo.INSTANCIA);
        assertThat(Completo.INSTANCIA.destinosPermitidos()).containsExactly(EnRevision.INSTANCIA, Presentado.INSTANCIA);
        assertThat(Presentado.INSTANCIA.destinosPermitidos()).containsExactly(Resuelto.INSTANCIA);
        assertThat(Resuelto.INSTANCIA.destinosPermitidos()).isEmpty();
    }

    @Test
    void soloIniciadoYEnRevisionPermitenCargar() {
        List<Boolean> permite = EstadoExpediente.secuencia().stream().map(EstadoExpediente::permiteCarga).toList();
        assertThat(permite).containsExactly(true, true, false, false, false);
    }

    @Test
    void elCodigoSeConvierteEnElEstado() {
        for (EstadoExpediente estado : EstadoExpediente.secuencia()) {
            assertThat(EstadoExpediente.desdeCodigo(estado.getCodigo())).isSameAs(estado);
        }
    }
}
