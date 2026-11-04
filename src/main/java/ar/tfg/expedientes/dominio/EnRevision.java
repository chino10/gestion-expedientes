package ar.tfg.expedientes.dominio;

/**
 * El gestor revisa la documentación cargada.
 */
public final class EnRevision extends EstadoExpediente {

    public static final EnRevision INSTANCIA = new EnRevision();

    private EnRevision() {
        super("EN_REVISION", "En revisión");
    }

    @Override
    public boolean validarTransicion(EstadoExpediente destino) {
        return destino instanceof Completo;
    }

    @Override
    public boolean permiteCarga() {
        return true;
    }
}
