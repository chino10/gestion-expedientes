package ar.tfg.expedientes.servicio;

import ar.tfg.expedientes.dominio.EventoTrazabilidad;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.repositorio.EventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Registro de eventos de trazabilidad (CU10). Se ejecuta dentro de la transacción de la
 * operación que lo invoca: si el evento no se puede guardar, la operación se revierte (curso 4a).
 */
@Service
public class TrazabilidadService {

    private final EventoRepository eventos;
    private final Clock reloj;

    public TrazabilidadService(EventoRepository eventos, Clock reloj) {
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public EventoTrazabilidad registrarEvento(Usuario usuario, Expediente expediente,
                                             AccionTrazabilidad accion, String detalle) {
        LocalDateTime fechaHora = obtenerFechaHora();
        return eventos.guardar(new EventoTrazabilidad(expediente, usuario, accion.name(), detalle, fechaHora));
    }

    /**
     * Registra un intento de acceso sin permiso (CU11, curso 2a) en una transacción propia,
     * para que el evento se conserve aunque la operación solicitada se rechace.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventoTrazabilidad registrarIntentoDenegado(Usuario usuario, Expediente expediente, String detalle) {
        return eventos.guardar(new EventoTrazabilidad(expediente, usuario, AccionTrazabilidad.ACCESO_DENEGADO.name(),
                detalle, obtenerFechaHora()));
    }

    private LocalDateTime obtenerFechaHora() {
        return LocalDateTime.now(reloj);
    }
}
