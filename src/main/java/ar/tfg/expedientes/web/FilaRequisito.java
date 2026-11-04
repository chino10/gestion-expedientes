package ar.tfg.expedientes.web;

import ar.tfg.expedientes.dominio.Documento;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Requisito;
import ar.tfg.expedientes.dominio.VersionDocumento;

import java.util.List;

/** Fila de las tablas de requisitos de las pantallas de consulta y de revisión. */
public record FilaRequisito(Requisito requisito, Documento documento, VersionDocumento vigente,
                            String resultado, String detalle) {

    public static List<FilaRequisito> de(Expediente expediente) {
        return expediente.getRequisitos().stream().map(r -> {
            Documento documento = expediente.documentoDe(r).orElse(null);
            VersionDocumento vigente = documento == null ? null : documento.versionVigente();
            String resultado = vigente == null ? "FALTANTE" : vigente.tipoResultado();
            String detalle = vigente == null ? "Falta cargar el documento." : vigente.detalleResultado();
            return new FilaRequisito(r, documento, vigente, resultado, detalle);
        }).toList();
    }

    /** Texto del resultado de la validación preliminar con su símbolo. */
    public String resultadoTexto() {
        return switch (resultado) {
            case "VALIDADO" -> "✓ Validado";
            case "INCONSISTENTE" -> "! Inconsistente";
            case "FALTANTE" -> "✗ Faltante";
            default -> "Pendiente de validación";
        };
    }
}
