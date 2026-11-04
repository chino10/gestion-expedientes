package ar.tfg.expedientes.web;

import ar.tfg.expedientes.dominio.Documento;
import ar.tfg.expedientes.dominio.EnRevision;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Observacion;
import ar.tfg.expedientes.dominio.ReglaDeNegocioException;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.dominio.VersionDocumento;
import ar.tfg.expedientes.repositorio.DocumentoRepository;
import ar.tfg.expedientes.repositorio.ExpedienteRepository;
import ar.tfg.expedientes.seguridad.UsuarioAutenticado;
import ar.tfg.expedientes.servicio.AccionTrazabilidad;
import ar.tfg.expedientes.servicio.NotificacionService;
import ar.tfg.expedientes.servicio.TrazabilidadService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Revisión documental (CU07) y registro de observaciones (CU08). Desde la misma pantalla
 * el gestor solicita el cambio de estado (CU09).
 */
@Controller
public class RevisionController {

    private final ExpedienteRepository expedientes;
    private final DocumentoRepository documentos;
    private final TrazabilidadService trazabilidad;
    private final NotificacionService notificaciones;
    private final Permisos permisos;
    private final Clock reloj;

    public RevisionController(ExpedienteRepository expedientes, DocumentoRepository documentos,
                              TrazabilidadService trazabilidad, NotificacionService notificaciones,
                              Permisos permisos, Clock reloj) {
        this.expedientes = expedientes;
        this.documentos = documentos;
        this.trazabilidad = trazabilidad;
        this.notificaciones = notificaciones;
        this.permisos = permisos;
        this.reloj = reloj;
    }

    @GetMapping("/expedientes/{id}/revision")
    @Transactional(readOnly = true)
    public String revision(@PathVariable Long id, @RequestParam(required = false) Long documento,
                           @AuthenticationPrincipal UsuarioAutenticado actual, Model modelo) {
        Usuario gestor = permisos.usuario(actual);
        Expediente expediente = expedientes.findById(id).orElseThrow();
        permisos.exigirRevision(gestor, expediente);
        modelo.addAttribute("expediente", expediente);
        modelo.addAttribute("filas", FilaRequisito.de(expediente));
        modelo.addAttribute("enRevision", expediente.getEstado() instanceof EnRevision);
        modelo.addAttribute("destinos", expediente.getEstado().destinosPermitidos());
        modelo.addAttribute("seleccionado", documento == null ? null
                : expediente.getDocumentos().stream().filter(d -> d.getId().equals(documento)).findFirst().orElse(null));
        return "revision";
    }

    @PostMapping("/documentos/{id}/aprobar")
    @Transactional
    public String aprobar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual,
                          RedirectAttributes redireccion) {
        Usuario gestor = permisos.usuario(actual);
        Documento documento = documentos.findById(id).orElseThrow();
        Expediente expediente = documento.getExpediente();
        permisos.exigirRevision(gestor, expediente);
        try {
            exigirEnRevision(expediente);
            documento.aprobar();
        } catch (ReglaDeNegocioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/expedientes/" + expediente.getId() + "/revision?documento=" + id;
        }
        documentos.guardar(documento);
        trazabilidad.registrarEvento(gestor, expediente, AccionTrazabilidad.APROBACION_DOCUMENTO,
                etiquetaVersion(documento) + ".");
        redireccion.addFlashAttribute("exito", "Se aprobó " + etiquetaVersion(documento) + ".");
        return "redirect:/expedientes/" + expediente.getId() + "/revision";
    }

    @PostMapping("/documentos/{id}/observaciones")
    @Transactional
    public String observar(@PathVariable Long id, @RequestParam String texto,
                           @AuthenticationPrincipal UsuarioAutenticado actual, RedirectAttributes redireccion) {
        Usuario gestor = permisos.usuario(actual);
        Documento documento = documentos.findById(id).orElseThrow();
        Expediente expediente = documento.getExpediente();
        permisos.exigirRevision(gestor, expediente);
        Observacion observacion;
        try {
            exigirEnRevision(expediente);
            VersionDocumento vigente = documento.versionVigente();
            if (vigente == null) {
                throw new ReglaDeNegocioException("No hay una versión cargada para observar.");
            }
            observacion = vigente.agregarObservacion(gestor, texto, LocalDateTime.now(reloj));
            documento.observar();
        } catch (ReglaDeNegocioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/expedientes/" + expediente.getId() + "/revision?documento=" + id;
        }
        documentos.guardar(documento);
        trazabilidad.registrarEvento(gestor, expediente, AccionTrazabilidad.OBSERVACION,
                etiquetaVersion(documento) + ": " + observacion.getTexto());
        notificaciones.notificarObservacion(expediente, observacion);
        redireccion.addFlashAttribute("exito", "Se registró la observación y se notificó al cliente.");
        return "redirect:/expedientes/" + expediente.getId() + "/revision";
    }

    private static void exigirEnRevision(Expediente expediente) {
        if (!(expediente.getEstado() instanceof EnRevision)) {
            throw new ReglaDeNegocioException("Para revisar documentos el expediente debe estar En revisión.");
        }
    }

    private static String etiquetaVersion(Documento documento) {
        return documento.getRequisito().etiqueta() + " (v" + documento.versionVigente().getNumero() + ")";
    }
}
