package ar.tfg.expedientes.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Documento que cumple un requisito del expediente. Se compone de sus versiones,
 * que nunca se sobrescriben (RNF03).
 */
@Entity
@Table(name = "documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requisito_id", nullable = false)
    private Requisito requisito;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_revision", nullable = false)
    private EstadoRevision estadoRevision;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL)
    @OrderBy("numero")
    private List<VersionDocumento> versiones = new ArrayList<>();

    protected Documento() {
    }

    Documento(Expediente expediente, Requisito requisito) {
        this.expediente = expediente;
        this.requisito = requisito;
        this.estadoRevision = EstadoRevision.PENDIENTE;
    }

    /**
     * Agrega una nueva versión. Las observaciones de versiones anteriores se consideran
     * resueltas por la nueva carga y el documento vuelve a quedar pendiente de revisión.
     */
    public void agregarVersion(VersionDocumento version) {
        version.asignarA(this, versiones.size() + 1);
        versiones.forEach(v -> v.getObservaciones().forEach(Observacion::resolver));
        versiones.add(version);
        estadoRevision = EstadoRevision.PENDIENTE;
    }

    public VersionDocumento versionVigente() {
        return versiones.isEmpty() ? null : versiones.get(versiones.size() - 1);
    }

    public void aprobar() {
        if (versionVigente() == null) {
            throw new ReglaDeNegocioException("No hay una versión cargada para aprobar.");
        }
        versionVigente().getObservaciones().forEach(Observacion::resolver);
        estadoRevision = EstadoRevision.APROBADO;
    }

    public void observar() {
        if (versionVigente() == null) {
            throw new ReglaDeNegocioException("No hay una versión cargada para observar.");
        }
        estadoRevision = EstadoRevision.OBSERVADO;
    }

    public Long getId() {
        return id;
    }

    public Expediente getExpediente() {
        return expediente;
    }

    public Requisito getRequisito() {
        return requisito;
    }

    public EstadoRevision getEstadoRevision() {
        return estadoRevision;
    }

    public List<VersionDocumento> getVersiones() {
        return versiones;
    }
}
