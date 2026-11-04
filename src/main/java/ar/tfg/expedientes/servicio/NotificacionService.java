package ar.tfg.expedientes.servicio;

import ar.tfg.expedientes.dominio.Cliente;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Observacion;
import ar.tfg.expedientes.dominio.Requisito;
import ar.tfg.expedientes.dominio.Rol;
import ar.tfg.expedientes.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Notificaciones por correo electrónico. En el prototipo el envío se simula: cada correo
 * se escribe en el registro (log) de la aplicación con su destinatario, asunto y cuerpo.
 * En el sistema completo se reemplaza por el envío mediante el servidor SMTP de la
 * arquitectura de despliegue.
 */
@Service
public class NotificacionService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionService.class);

    private final UsuarioRepository usuarios;

    public NotificacionService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public void enviarAcceso(Cliente cliente, Expediente expediente, List<Requisito> requisitos, String enlaceActivacion) {
        String listado = requisitos.stream()
                .map(r -> "  - " + r.etiqueta()
                        + (r.isRequiereApostilla() ? " (con apostilla)" : "")
                        + (r.isRequiereTraduccion() ? " (con traducción)" : ""))
                .collect(Collectors.joining("\n"));
        String acceso = enlaceActivacion == null
                ? "Puede ingresar al sistema con su usuario habitual."
                : "Para definir su contraseña ingrese, dentro de las próximas 24 horas, a:\n  " + enlaceActivacion;
        enviar(cliente.getUsuario().getEmail(),
                "Expediente " + expediente.getNumero() + " - Acceso y requisitos documentales",
                acceso + "\n\nDocumentación requerida (" + expediente.getPais() + ", " + expediente.getVia() + "):\n" + listado);
    }

    public void notificarObservacion(Expediente expediente, Observacion observacion) {
        enviar(expediente.getCliente().getUsuario().getEmail(),
                "Expediente " + expediente.getNumero() + " - Observaciones documentales",
                "Documento observado: " + observacion.getVersion().getDocumento().getRequisito().etiqueta()
                        + " (versión " + observacion.getVersion().getNumero() + ")\n"
                        + "Observación: " + observacion.getTexto() + "\n"
                        + "Remitente: " + observacion.getGestor().getNombre());
    }

    public void notificarCambioEstado(Expediente expediente) {
        enviar(expediente.getCliente().getUsuario().getEmail(),
                "Expediente " + expediente.getNumero() + " - Nuevo estado: " + expediente.getEstado().getNombre(),
                "Su expediente pasó al estado " + expediente.getEstado().getNombre() + ".");
    }

    public void notificarPaisSinReglas(Expediente expediente) {
        usuarios.buscarActivosPorRol(Rol.ADMINISTRADOR).forEach(admin -> enviar(admin.getEmail(),
                "Expediente " + expediente.getNumero() + " pendiente de validación",
                "No hay reglas de validación configuradas para " + expediente.getPais() + "."));
    }

    private void enviar(String destinatario, String asunto, String cuerpo) {
        LOG.info("[CORREO SIMULADO]\nPara: {}\nAsunto: {}\n{}", destinatario, asunto, cuerpo);
    }
}
