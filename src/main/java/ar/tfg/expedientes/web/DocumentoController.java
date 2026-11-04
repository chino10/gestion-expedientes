package ar.tfg.expedientes.web;

import ar.tfg.expedientes.dominio.Documento;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.ReglaDeNegocioException;
import ar.tfg.expedientes.dominio.Requisito;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.dominio.VersionDocumento;
import ar.tfg.expedientes.repositorio.DocumentoRepository;
import ar.tfg.expedientes.repositorio.ExpedienteRepository;
import ar.tfg.expedientes.repositorio.VersionDocumentoRepository;
import ar.tfg.expedientes.seguridad.UsuarioAutenticado;
import ar.tfg.expedientes.servicio.AccionTrazabilidad;
import ar.tfg.expedientes.servicio.AlmacenamientoArchivos;
import ar.tfg.expedientes.servicio.TrazabilidadService;
import ar.tfg.expedientes.validacion.Hallazgo;
import ar.tfg.expedientes.validacion.ResultadoRequisito;
import ar.tfg.expedientes.validacion.ValidadorDocumental;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Carga de documentos (CU05) con validación preliminar (CU06) y descarga de versiones.
 */
@Controller
public class DocumentoController {

    private final ExpedienteRepository expedientes;
    private final DocumentoRepository documentos;
    private final VersionDocumentoRepository versiones;
    private final ValidadorDocumental validador;
    private final TrazabilidadService trazabilidad;
    private final AlmacenamientoArchivos almacenamiento;
    private final Permisos permisos;
    private final Clock reloj;

    public DocumentoController(ExpedienteRepository expedientes, DocumentoRepository documentos,
                               VersionDocumentoRepository versiones, ValidadorDocumental validador,
                               TrazabilidadService trazabilidad, AlmacenamientoArchivos almacenamiento,
                               Permisos permisos, Clock reloj) {
        this.expedientes = expedientes;
        this.documentos = documentos;
        this.versiones = versiones;
        this.validador = validador;
        this.trazabilidad = trazabilidad;
        this.almacenamiento = almacenamiento;
        this.permisos = permisos;
        this.reloj = reloj;
    }

    @GetMapping("/expedientes/{id}/documentos/nuevo")
    @Transactional(readOnly = true)
    public String pantallaCarga(@PathVariable Long id, @RequestParam(required = false) Long requisito,
                                @AuthenticationPrincipal UsuarioAutenticado actual, Model modelo,
                                RedirectAttributes redireccion) {
        Usuario usuario = permisos.usuario(actual);
        Expediente expediente = expedientes.findById(id).orElseThrow();
        permisos.exigirCarga(usuario, expediente);
        if (!permisos.puedeCargar(usuario, expediente)) {
            redireccion.addFlashAttribute("error", "El expediente está en estado "
                    + expediente.getEstado().getNombre() + " y no admite la carga de documentos.");
            return "redirect:/expedientes/" + id;
        }
        modelo.addAttribute("expediente", expediente);
        modelo.addAttribute("seleccionado", requisito);
        return "documento-carga";
    }

    @PostMapping("/expedientes/{id}/documentos")
    @Transactional
    public String cargar(@PathVariable Long id, @RequestParam("requisito") Long requisitoId,
                         @RequestParam("archivo") MultipartFile archivo,
                         @RequestParam String apellido, @RequestParam String nombre,
                         @RequestParam(defaultValue = "false") boolean apostilla,
                         @RequestParam(defaultValue = "false") boolean traduccion,
                         @AuthenticationPrincipal UsuarioAutenticado actual,
                         RedirectAttributes redireccion) {
        Usuario usuario = permisos.usuario(actual);
        Expediente expediente = expedientes.findById(id).orElseThrow();
        permisos.exigirCarga(usuario, expediente);
        String volver = "redirect:/expedientes/" + id + "/documentos/nuevo?requisito=" + requisitoId;
        Requisito requisito = expediente.getRequisitos().stream()
                .filter(r -> r.getId().equals(requisitoId)).findFirst().orElse(null);
        String ruta;
        try {
            if (!permisos.puedeCargar(usuario, expediente)) {
                throw new ReglaDeNegocioException("El expediente está en estado "
                        + expediente.getEstado().getNombre() + " y no admite la carga de documentos.");
            }
            if (requisito == null) {
                throw new ReglaDeNegocioException("Seleccione un requisito del expediente.");
            }
            if (apellido.isBlank() || nombre.isBlank()) {
                throw new ReglaDeNegocioException("Indique el apellido y el nombre tal como figuran en el documento.");
            }
            // Paso 3 y curso alternativo 3a: verificación de formato y tamaño.
            ruta = almacenamiento.guardar(expediente.getNumero(), archivo);
        } catch (ReglaDeNegocioException e) {
            redireccion.addFlashAttribute("error", e.getMessage());
            return volver;
        }

        // Paso 4: nueva versión con autor, fecha y hora.
        VersionDocumento version = new VersionDocumento(usuario, ruta, LocalDateTime.now(reloj),
                apellido, nombre, apostilla, traduccion);
        Documento documento = expediente.documentoPara(requisito);
        documento.agregarVersion(version);
        documentos.guardar(documento);

        // Paso 5: validación preliminar (CU06).
        List<Hallazgo> hallazgos = validador.validar(expediente);

        // Paso 6: evento de trazabilidad (CU10).
        trazabilidad.registrarEvento(usuario, expediente, AccionTrazabilidad.CARGA_DOCUMENTO,
                requisito.etiqueta() + " (v" + version.getNumero() + "): resultado " + version.tipoResultado().toLowerCase(Locale.ROOT) + ".");

        // Paso 7: resultado de la validación preliminar.
        redireccion.addFlashAttribute("exito", "Se guardó la versión " + version.getNumero()
                + " de " + requisito.etiqueta() + ".");
        redireccion.addFlashAttribute("resultado", resumen(requisito, hallazgos));
        redireccion.addFlashAttribute("resultadoValido", hallazgos.stream()
                .allMatch(h -> h.resultado() == ResultadoRequisito.VALIDADO));
        return volver;
    }

    /** Mensajes del requisito cargado seguidos de los documentos que faltan en el expediente. */
    private static List<String> resumen(Requisito cargado, List<Hallazgo> hallazgos) {
        List<String> lineas = new ArrayList<>();
        hallazgos.stream()
                .filter(h -> h.requisito().getId().equals(cargado.getId()))
                .forEach(h -> lineas.add((h.resultado() == ResultadoRequisito.VALIDADO ? "✓ " : "✗ ")
                        + cargado.etiqueta() + ": " + h.mensaje()));
        hallazgos.stream()
                .filter(h -> !h.requisito().getId().equals(cargado.getId()))
                .filter(h -> h.resultado() == ResultadoRequisito.INCONSISTENTE)
                .forEach(h -> lineas.add("✗ " + h.requisito().etiqueta() + ": " + h.mensaje()));
        List<String> faltantes = hallazgos.stream()
                .filter(h -> h.resultado() == ResultadoRequisito.FALTANTE)
                .map(h -> h.requisito().etiqueta().toLowerCase(Locale.ROOT))
                .toList();
        if (!faltantes.isEmpty()) {
            lineas.add("✗ Faltan " + faltantes.size() + (faltantes.size() == 1 ? " documento: " : " documentos: ")
                    + faltantes.stream().collect(Collectors.joining("; ")) + ".");
        }
        return lineas;
    }

    /** Descarga o visualización del archivo de una versión, con control de acceso. */
    @GetMapping("/versiones/{id}/archivo")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> archivo(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        Usuario usuario = permisos.usuario(actual);
        VersionDocumento version = versiones.findById(id).orElseThrow();
        permisos.exigirConsulta(usuario, version.getDocumento().getExpediente());
        Path ruta = almacenamiento.ubicar(version.getRutaArchivo());
        String nombre = version.nombreArchivo();
        String extension = nombre.toLowerCase(Locale.ROOT);
        MediaType tipo = extension.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : extension.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nombre, StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(ruta));
    }
}
