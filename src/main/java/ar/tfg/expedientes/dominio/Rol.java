package ar.tfg.expedientes.dominio;

/**
 * Perfiles de acceso definidos en la Tabla 20 (Funcionalidades habilitadas por perfil).
 */
public enum Rol {
    ADMINISTRADOR("Administrador"),
    ADMINISTRATIVO("Administrativo"),
    GESTOR("Gestor"),
    CLIENTE("Cliente");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
