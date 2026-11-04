package ar.tfg.expedientes.dominio;

/**
 * La solicitud fue presentada ante el consulado u organismo competente.
 */
public final class Presentado extends EstadoExpediente {

    public static final Presentado INSTANCIA = new Presentado();

    private Presentado() {
        super("PRESENTADO", "Presentado");
    }

    @Override
    public boolean validarTransicion(EstadoExpediente destino) {
        return destino instanceof Resuelto;
    }

    @Override
    public boolean permiteCarga() {
        return false;
    }
}
