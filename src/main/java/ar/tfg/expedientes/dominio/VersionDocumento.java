package ar.tfg.expedientes.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Versión de un documento: el archivo cargado, los datos declarados y el resultado
 * de la validación preliminar.
 */
@Entity
@Table(name = "version_documento")
public class VersionDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cargado_por", nullable = false)
    private Usuario cargadoPor;

    @Column(nullable = false)
    private int numero;

    @Column(name = "ruta_archivo", nullable = false)
    private String rutaArchivo;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "apellido_declarado", nullable = false)
    private String apellidoDeclarado;

    @Column(name = "nombre_declarado", nullable = false)
    private String nombreDeclarado;

    @Column(name = "declara_apostilla", nullable = false)
    private boolean declaraApostilla;

    @Column(name = "declara_traduccion", nullable = false)
    private boolean declaraTraduccion;

    @Column(name = "resultado_validacion")
    private String resultadoValidacion;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL)
    @OrderBy("fecha")
    private List<Observacion> observaciones = new ArrayList<>();

    protected VersionDocumento() {
    }

    public VersionDocumento(Usuario cargadoPor, String rutaArchivo, LocalDateTime fechaCarga,
                            String apellidoDeclarado, String nombreDeclarado,
                            boolean declaraApostilla, boolean declaraTraduccion) {
        this.cargadoPor = cargadoPor;
        this.rutaArchivo = rutaArchivo;
        this.fechaCarga = fechaCarga;
        this.apellidoDeclarado = apellidoDeclarado.trim();
        this.nombreDeclarado = nombreDeclarado.trim();
        this.declaraApostilla = declaraApostilla;
        this.declaraTraduccion = declaraTraduccion;
    }

    void asignarA(Documento documento, int numero) {
        this.documento = documento;
        this.numero = numero;
    }

    public void registrarResultado(String resultado) {
        this.resultadoValidacion = resultado;
    }

    /** Tipo del resultado de la validación: VALIDADO, INCONSISTENTE o PENDIENTE. */
    public String tipoResultado() {
        if (resultadoValidacion == null) {
            return "PENDIENTE";
        }
        int separador = resultadoValidacion.indexOf(':');
        return separador < 0 ? resultadoValidacion : resultadoValidacion.substring(0, separador);
    }

    /** Detalle del resultado de la validación, sin el tipo. */
    public String detalleResultado() {
        if (resultadoValidacion == null) {
            return "";
        }
        int separador = resultadoValidacion.indexOf(':');
        return separador < 0 ? "" : resultadoValidacion.substring(separador + 1).trim();
    }

    public Observacion agregarObservacion(Usuario gestor, String texto, LocalDateTime fecha) {
        Observacion observacion = new Observacion(this, gestor, texto, fecha);
        observaciones.add(observacion);
        return observacion;
    }

    /** Nombre original del archivo, sin el prefijo único con que se almacena. */
    public String nombreArchivo() {
        String nombre = rutaArchivo.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        int separador = nombre.indexOf("__");
        return separador >= 0 ? nombre.substring(separador + 2) : nombre;
    }

    public Long getId() {
        return id;
    }

    public Documento getDocumento() {
        return documento;
    }

    public Usuario getCargadoPor() {
        return cargadoPor;
    }

    public int getNumero() {
        return numero;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public String getApellidoDeclarado() {
        return apellidoDeclarado;
    }

    public String getNombreDeclarado() {
        return nombreDeclarado;
    }

    public boolean isDeclaraApostilla() {
        return declaraApostilla;
    }

    public boolean isDeclaraTraduccion() {
        return declaraTraduccion;
    }

    public String getResultadoValidacion() {
        return resultadoValidacion;
    }

    public List<Observacion> getObservaciones() {
        return observaciones;
    }
}
