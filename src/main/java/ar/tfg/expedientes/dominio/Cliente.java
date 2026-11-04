package ar.tfg.expedientes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "documento_identidad", nullable = false, unique = true)
    private String documentoIdentidad;

    private String telefono;

    protected Cliente() {
    }

    public Cliente(Usuario usuario, String apellido, String nombre, String documentoIdentidad, String telefono) {
        this.usuario = usuario;
        this.apellido = apellido.trim();
        this.nombre = nombre.trim();
        this.documentoIdentidad = documentoIdentidad.trim();
        this.telefono = telefono == null ? null : telefono.trim();
    }

    public String nombreCompleto() {
        return apellido + ", " + nombre;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getApellido() {
        return apellido;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public String getTelefono() {
        return telefono;
    }
}
