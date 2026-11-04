package ar.tfg.expedientes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Observación del gestor sobre una versión de un documento (CU08). */
@Entity
@Table(name = "observacion")
public class Observacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "version_documento_id", nullable = false)
    private VersionDocumento version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Usuario gestor;

    @Column(nullable = false)
    private String texto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private boolean resuelta;

    protected Observacion() {
    }

    Observacion(VersionDocumento version, Usuario gestor, String texto, LocalDateTime fecha) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaDeNegocioException("La observación no puede estar vacía.");
        }
        this.version = version;
        this.gestor = gestor;
        this.texto = texto.trim();
        this.fecha = fecha;
        this.resuelta = false;
    }

    public void resolver() {
        this.resuelta = true;
    }

    public Long getId() {
        return id;
    }

    public VersionDocumento getVersion() {
        return version;
    }

    public Usuario getGestor() {
        return gestor;
    }

    public String getTexto() {
        return texto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public boolean isResuelta() {
        return resuelta;
    }
}
