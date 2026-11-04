package ar.tfg.expedientes.servicio;

/** Acciones que se registran como eventos de trazabilidad (CU10). */
public enum AccionTrazabilidad {
    ALTA_CLIENTE("Alta del cliente"),
    ALTA_EXPEDIENTE("Alta del expediente"),
    CARGA_DOCUMENTO("Carga de documento"),
    APROBACION_DOCUMENTO("Aprobación de documento"),
    OBSERVACION("Observación registrada"),
    CAMBIO_ESTADO("Cambio de estado"),
    ACTIVACION_CUENTA("Activación de cuenta"),
    BLOQUEO_CUENTA("Bloqueo de cuenta"),
    ACCESO_DENEGADO("Acceso denegado");

    private final String etiqueta;

    AccionTrazabilidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static String etiquetaDe(String codigo) {
        try {
            return valueOf(codigo).etiqueta;
        } catch (IllegalArgumentException e) {
            return codigo;
        }
    }
}
