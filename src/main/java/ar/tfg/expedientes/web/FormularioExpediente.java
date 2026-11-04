package ar.tfg.expedientes.web;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Datos del formulario de registro de expediente (CU04). */
public class FormularioExpediente {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private String documento;
    private String apellido;
    private String nombre;
    private String email;
    private String telefono;
    private String ascendiente;
    private String paisVia;
    private Long gestorId;

    /** Controla los datos obligatorios (CU04, paso 5). Los del cliente solo si es nuevo. */
    public List<String> validar(boolean clienteNuevo) {
        List<String> errores = new ArrayList<>();
        if (vacio(documento)) {
            errores.add("Ingrese el documento de identidad del cliente.");
        }
        if (clienteNuevo) {
            if (vacio(apellido)) {
                errores.add("Ingrese el apellido del cliente.");
            }
            if (vacio(nombre)) {
                errores.add("Ingrese el nombre del cliente.");
            }
            if (vacio(email) || !EMAIL.matcher(email.trim()).matches()) {
                errores.add("Ingrese un correo electrónico válido para el cliente.");
            }
        }
        if (vacio(ascendiente)) {
            errores.add("Ingrese el apellido y el nombre del ascendiente.");
        }
        if (vacio(paisVia) || !paisVia.contains("|")) {
            errores.add("Seleccione el país y la vía de solicitud.");
        }
        if (gestorId == null) {
            errores.add("Seleccione el gestor asignado.");
        }
        return errores;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    public String pais() {
        return paisVia.substring(0, paisVia.indexOf('|'));
    }

    public String via() {
        return paisVia.substring(paisVia.indexOf('|') + 1);
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento == null ? null : documento.trim();
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getAscendiente() {
        return ascendiente;
    }

    public void setAscendiente(String ascendiente) {
        this.ascendiente = ascendiente;
    }

    public String getPaisVia() {
        return paisVia;
    }

    public void setPaisVia(String paisVia) {
        this.paisVia = paisVia;
    }

    public Long getGestorId() {
        return gestorId;
    }

    public void setGestorId(Long gestorId) {
        this.gestorId = gestorId;
    }
}
