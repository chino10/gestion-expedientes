package ar.tfg.expedientes.dominio;

/**
 * Estado inicial del expediente, desde su alta hasta que el gestor inicia la revisión.
 */
public final class Iniciado extends EstadoExpediente {

    public static final Iniciado INSTANCIA = new Iniciado();

    private Iniciado() {
        super("INICIADO", "Iniciado");
    }

    @Override
    public boolean validarTransicion(EstadoExpediente destino) {
        return destino instanceof EnRevision;
    }

    @Override
    public boolean permiteCarga() {
        return true;
    }
}
