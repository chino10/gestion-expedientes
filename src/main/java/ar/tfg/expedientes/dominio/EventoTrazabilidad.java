package ar.tfg.expedientes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Evento de trazabilidad (CU10). La entidad es inmutable: la aplicación no puede
 * modificar ni eliminar eventos (RNF03).
 */
@Entity
@Immutable
@Table(name = "evento_trazabilidad")
public class EventoTrazabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "expediente_id")
    private Expediente expediente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String accion;

    @Column(nullable = false)
    private String detalle;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    protected EventoTrazabilidad() {
    }

    public EventoTrazabilidad(Expediente expediente, Usuario usuario, String accion, String detalle,
                              LocalDateTime fechaHora) {
        this.expediente = expediente;
        this.usuario = usuario;
        this.accion = accion;
        this.detalle = detalle;
        this.fechaHora = fechaHora;
    }

    public Long getId() {
        return id;
    }

    public Expediente getExpediente() {
        return expediente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
