package ar.tfg.expedientes.validacion;

import ar.tfg.expedientes.dominio.Expediente;

import java.util.List;

/**
 * Reglas de validación documental de un país (patrón Strategy). Para incorporar un país
 * se agrega una implementación, sin modificar el resto del sistema.
 */
public interface ReglaValidacionPais {

    /** País al que se aplican las reglas, tal como figura en los requisitos. */
    String pais();

    /** Devuelve un hallazgo por cada requisito asociado al expediente. */
    List<Hallazgo> validar(Expediente expediente);
}
