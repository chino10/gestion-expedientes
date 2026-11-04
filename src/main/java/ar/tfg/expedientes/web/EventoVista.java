package ar.tfg.expedientes.web;

import ar.tfg.expedientes.dominio.EventoTrazabilidad;
import ar.tfg.expedientes.servicio.AccionTrazabilidad;

import java.time.LocalDateTime;
import java.util.List;

/** Evento del historial tal como se muestra en la consulta del expediente (CU11). */
public record EventoVista(LocalDateTime fechaHora, String usuario, String rol, String accion, String detalle) {

    public static List<EventoVista> de(List<EventoTrazabilidad> eventos) {
        return eventos.stream().map(ev -> new EventoVista(ev.getFechaHora(), ev.getUsuario().getNombre(),
                ev.getUsuario().getRol().getEtiqueta(), AccionTrazabilidad.etiquetaDe(ev.getAccion()),
                ev.getDetalle())).toList();
    }
}
