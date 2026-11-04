package ar.tfg.expedientes.servicio;

import java.util.Map;

/**
 * Indicadores de seguimiento de un período (CU12).
 *
 * @param expedientes            expedientes iniciados en el período
 * @param requisitos             requisitos asociados a esos expedientes
 * @param requisitosFaltantes    requisitos sin documento cargado
 * @param inconsistencias        versiones cargadas en el período con resultado inconsistente
 * @param revisiones             documentos revisados (aprobados u observados) en el período
 * @param promedioRevisionHoras  tiempo promedio entre la carga y la revisión, en horas
 * @param cambiosDeEstado        cambios de estado registrados en el período, por estado destino
 */
public record Indicadores(int expedientes, int requisitos, int requisitosFaltantes, long inconsistencias,
                          int revisiones, Double promedioRevisionHoras, Map<String, Long> cambiosDeEstado) {

    public double proporcionFaltantes() {
        return requisitos == 0 ? 0 : 100.0 * requisitosFaltantes / requisitos;
    }

    public long totalCambiosDeEstado() {
        return cambiosDeEstado.values().stream().mapToLong(Long::longValue).sum();
    }
}
