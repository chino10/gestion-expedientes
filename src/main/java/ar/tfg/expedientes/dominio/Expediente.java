package ar.tfg.expedientes.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Expediente de solicitud de ciudadanía. Su ciclo de vida se delega en {@link EstadoExpediente}.
 */
@Entity
@Table(name = "expediente")
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Usuario gestor;

    @Column(nullable = false)
    private String pais;

    @Column(nullable = false)
    private String via;

    @Column(nullable = false)
    private String ascendiente;

    @Convert(converter = EstadoExpedienteConverter.class)
    @Column(nullable = false)
    private EstadoExpediente estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_turno")
    private LocalDate fechaTurno;

    private String resolucion;

    @ManyToMany
    @JoinTable(name = "expediente_requisito",
            joinColumns = @JoinColumn(name = "expediente_id"),
            inverseJoinColumns = @JoinColumn(name = "requisito_id"))
    @OrderBy("id")
    private List<Requisito> requisitos = new ArrayList<>();

    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Documento> documentos = new ArrayList<>();

    protected Expediente() {
    }

    public Expediente(String numero, Cliente cliente, Usuario gestor, String pais, String via,
                      String ascendiente, LocalDate fechaInicio) {
        if (gestor.getRol() != Rol.GESTOR) {
            throw new ReglaDeNegocioException("El usuario asignado no tiene el rol Gestor.");
        }
        this.numero = numero;
        this.cliente = cliente;
        this.gestor = gestor;
        this.pais = pais;
        this.via = via;
        this.ascendiente = ascendiente.trim();
        this.fechaInicio = fechaInicio;
        this.estado = Iniciado.INSTANCIA;
    }

    /** Asocia al expediente el listado de requisitos vigente a la fecha de su alta (RF05). */
    public void asociarRequisitos(List<Requisito> lista) {
        if (!requisitos.isEmpty()) {
            throw new ReglaDeNegocioException("El expediente ya tiene requisitos asociados.");
        }
        List<Requisito> vigentes = lista.stream()
                .filter(r -> r.getPais().equals(pais) && r.getVia().equals(via) && r.estaVigente(fechaInicio))
                .sorted(Comparator.comparing(Requisito::getId))
                .toList();
        if (vigentes.isEmpty()) {
            throw new ReglaDeNegocioException("No hay requisitos vigentes configurados para " + pais + " – " + via + ".");
        }
        requisitos.addAll(vigentes);
    }

    /**
     * Cambia el estado del expediente (CU09). La transición la valida el estado actual
     * y, según el destino, se controlan los datos que exige.
     */
    public void avanzarA(EstadoExpediente destino) {
        if (!estado.validarTransicion(destino)) {
            throw new ReglaDeNegocioException("No se permite pasar de " + estado.getNombre()
                    + " a " + destino.getNombre() + ".");
        }
        if (destino instanceof Completo) {
            List<Requisito> pendientes = requisitosPendientes();
            if (!pendientes.isEmpty()) {
                throw new ReglaDeNegocioException("No se puede marcar como Completo. Requisitos sin documento aprobado: "
                        + pendientes.stream().map(Requisito::etiqueta).collect(Collectors.joining("; ")) + ".");
            }
        }
        if (destino instanceof Presentado && fechaTurno == null) {
            throw new ReglaDeNegocioException("Para pasar a Presentado debe registrarse la fecha del turno consular.");
        }
        if (destino instanceof Resuelto && (resolucion == null || resolucion.isBlank())) {
            throw new ReglaDeNegocioException("Para pasar a Resuelto debe registrarse la resolución comunicada.");
        }
        this.estado = destino;
    }

    /** Requisitos que todavía no tienen un documento aprobado por el gestor. */
    public List<Requisito> requisitosPendientes() {
        return requisitos.stream()
                .filter(r -> documentoDe(r).map(d -> d.getEstadoRevision() != EstadoRevision.APROBADO).orElse(true))
                .toList();
    }

    /** Requisitos para los que no se cargó ningún documento. */
    public List<Requisito> requisitosSinDocumento() {
        return requisitos.stream().filter(r -> documentoDe(r).isEmpty()).toList();
    }

    public void registrarTurno(LocalDate fecha) {
        if (fecha == null) {
            throw new ReglaDeNegocioException("Debe indicarse la fecha del turno consular.");
        }
        this.fechaTurno = fecha;
    }

    public void registrarResolucion(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaDeNegocioException("Debe indicarse la resolución comunicada por el organismo.");
        }
        this.resolucion = texto.trim();
    }

    public Optional<Documento> documentoDe(Requisito requisito) {
        return documentos.stream().filter(d -> d.getRequisito().getId().equals(requisito.getId())).findFirst();
    }

    /** Devuelve el documento del requisito o lo crea si es la primera carga. */
    public Documento documentoPara(Requisito requisito) {
        if (requisitos.stream().noneMatch(r -> r.getId().equals(requisito.getId()))) {
            throw new ReglaDeNegocioException("El requisito no pertenece al expediente.");
        }
        return documentoDe(requisito).orElseGet(() -> {
            Documento nuevo = new Documento(this, requisito);
            documentos.add(nuevo);
            return nuevo;
        });
    }

    public boolean permiteCarga() {
        return estado.permiteCarga();
    }

    public List<Observacion> observacionesPendientes() {
        return documentos.stream()
                .flatMap(d -> d.getVersiones().stream())
                .flatMap(v -> v.getObservaciones().stream())
                .filter(o -> !o.isResuelta())
                .toList();
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Usuario getGestor() {
        return gestor;
    }

    public String getPais() {
        return pais;
    }

    public String getVia() {
        return via;
    }

    public String getAscendiente() {
        return ascendiente;
    }

    public EstadoExpediente getEstado() {
        return estado;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaTurno() {
        return fechaTurno;
    }

    public String getResolucion() {
        return resolucion;
    }

    public List<Requisito> getRequisitos() {
        return requisitos;
    }

    public List<Documento> getDocumentos() {
        return documentos;
    }
}
