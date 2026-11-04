package ar.tfg.expedientes.dominio;

/**
 * El organismo comunicó la resolución; estado final.
 */
public final class Resuelto extends EstadoExpediente {

    public static final Resuelto INSTANCIA = new Resuelto();

    private Resuelto() {
        super("RESUELTO", "Resuelto");
    }

    @Override
    public boolean validarTransicion(EstadoExpediente destino) {
        return false;
    }

    @Override
    public boolean permiteCarga() {
        return false;
    }
}
