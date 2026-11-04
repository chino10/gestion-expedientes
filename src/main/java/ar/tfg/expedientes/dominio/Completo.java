package ar.tfg.expedientes.dominio;

/**
 * Toda la documentación del expediente fue aprobada. Puede presentarse ante el organismo
 * o volver a En revisión si surge una nueva observación (CU09).
 */
public final class Completo extends EstadoExpediente {

    public static final Completo INSTANCIA = new Completo();

    private Completo() {
        super("COMPLETO", "Completo");
    }

    @Override
    public boolean validarTransicion(EstadoExpediente destino) {
        return destino instanceof Presentado || destino instanceof EnRevision;
    }

    @Override
    public boolean permiteCarga() {
        return false;
    }
}
