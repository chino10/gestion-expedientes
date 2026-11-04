package ar.tfg.expedientes.dominio;

import java.util.List;

/**
 * Estado del ciclo de vida del expediente (patrón State). Cada subclase define las
 * transiciones que admite y si permite la carga de documentos.
 */
public abstract class EstadoExpediente {

    private final String codigo;
    private final String nombre;

    protected EstadoExpediente(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    /** Indica si desde este estado se puede pasar al estado destino. */
    public abstract boolean validarTransicion(EstadoExpediente destino);

    /** Indica si en este estado se pueden cargar documentos. */
    public abstract boolean permiteCarga();

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    /** Secuencia completa de estados, en el orden en que se muestran al usuario. */
    public static List<EstadoExpediente> secuencia() {
        return List.of(Iniciado.INSTANCIA, EnRevision.INSTANCIA, Completo.INSTANCIA,
                Presentado.INSTANCIA, Resuelto.INSTANCIA);
    }

    public static EstadoExpediente desdeCodigo(String codigo) {
        return secuencia().stream()
                .filter(e -> e.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado desconocido: " + codigo));
    }

    /** Estados a los que se puede pasar desde este. */
    public List<EstadoExpediente> destinosPermitidos() {
        return secuencia().stream().filter(this::validarTransicion).toList();
    }

    /** Posición del estado en la secuencia (0 = Iniciado). */
    public int posicion() {
        return secuencia().indexOf(this);
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof EstadoExpediente e && e.codigo.equals(codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
