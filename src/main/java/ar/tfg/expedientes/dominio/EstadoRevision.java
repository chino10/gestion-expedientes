package ar.tfg.expedientes.dominio;

/** Resultado de la revisión del gestor sobre un documento. */
public enum EstadoRevision {
    PENDIENTE("Pendiente de revisión"),
    APROBADO("Aprobado"),
    OBSERVADO("Observado");

    private final String etiqueta;

    EstadoRevision(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
