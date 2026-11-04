package ar.tfg.expedientes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Requisito documental de un país y vía de solicitud, con su período de vigencia.
 */
@Entity
@Table(name = "requisito")
public class Requisito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pais;

    @Column(nullable = false)
    private String via;

    @Column(nullable = false)
    private String documento;

    @Column(nullable = false)
    private String persona;

    @Column(name = "requiere_apostilla", nullable = false)
    private boolean requiereApostilla;

    @Column(name = "requiere_traduccion", nullable = false)
    private boolean requiereTraduccion;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    protected Requisito() {
    }

    public Requisito(String pais, String via, String documento, String persona,
                     boolean requiereApostilla, boolean requiereTraduccion, LocalDate vigenteDesde) {
        this.pais = pais;
        this.via = via;
        this.documento = documento;
        this.persona = persona;
        this.requiereApostilla = requiereApostilla;
        this.requiereTraduccion = requiereTraduccion;
        this.vigenteDesde = vigenteDesde;
    }

    public boolean estaVigente(LocalDate fecha) {
        return !fecha.isBefore(vigenteDesde) && (vigenteHasta == null || !fecha.isAfter(vigenteHasta));
    }

    public void cerrarVigencia(LocalDate fecha) {
        this.vigenteHasta = fecha;
    }

    /** Texto con el que el requisito se muestra en pantallas, observaciones y eventos. */
    public String etiqueta() {
        return documento + " – " + persona;
    }

    public Long getId() {
        return id;
    }

    public String getPais() {
        return pais;
    }

    public String getVia() {
        return via;
    }

    public String getDocumento() {
        return documento;
    }

    public String getPersona() {
        return persona;
    }

    public boolean isRequiereApostilla() {
        return requiereApostilla;
    }

    public boolean isRequiereTraduccion() {
        return requiereTraduccion;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }
}
