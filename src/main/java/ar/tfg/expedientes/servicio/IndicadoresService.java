package ar.tfg.expedientes.servicio;

import ar.tfg.expedientes.dominio.EstadoExpediente;
import ar.tfg.expedientes.dominio.EventoTrazabilidad;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.VersionDocumento;
import ar.tfg.expedientes.repositorio.EventoRepository;
import ar.tfg.expedientes.repositorio.ExpedienteRepository;
import ar.tfg.expedientes.repositorio.VersionDocumentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cálculo de los indicadores de seguimiento (CU12) a partir de los expedientes y de los
 * eventos de trazabilidad del período, sin carga adicional de datos.
 */
@Service
public class IndicadoresService {

    private final ExpedienteRepository expedientes;
    private final VersionDocumentoRepository versiones;
    private final EventoRepository eventos;

    public IndicadoresService(ExpedienteRepository expedientes, VersionDocumentoRepository versiones,
                              EventoRepository eventos) {
        this.expedientes = expedientes;
        this.versiones = versiones;
        this.eventos = eventos;
    }

    /**
     * @param gestorId gestor cuyos expedientes asignados se consideran, o {@code null} para
     *                 considerar todos los expedientes (consulta del administrador).
     */
    @Transactional(readOnly = true)
    public Indicadores calcular(LocalDate desde, LocalDate hasta, String pais, Long gestorId) {
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        List<Expediente> delPeriodo = expedientes.buscarIniciadosHasta(hasta).stream()
                .filter(e -> !e.getFechaInicio().isBefore(desde))
                .filter(e -> incluido(e, pais, gestorId))
                .toList();
        int requisitos = delPeriodo.stream().mapToInt(e -> e.getRequisitos().size()).sum();
        int faltantes = delPeriodo.stream().mapToInt(e -> e.requisitosSinDocumento().size()).sum();

        long inconsistencias = versiones.buscarCargadasEntre(inicio, fin).stream()
                .filter(v -> incluido(v.getDocumento().getExpediente(), pais, gestorId))
                .map(VersionDocumento::tipoResultado)
                .filter("INCONSISTENTE"::equals)
                .count();

        List<EventoTrazabilidad> delPeriodoEventos = eventos.buscarPorPeriodo(inicio, fin).stream()
                .filter(ev -> ev.getExpediente() != null && incluido(ev.getExpediente(), pais, gestorId))
                .toList();

        List<Duration> tiempos = delPeriodoEventos.stream()
                .filter(ev -> esRevision(ev.getAccion()))
                .map(this::tiempoDeRevision)
                .flatMap(Optional::stream)
                .toList();
        Double promedioHoras = tiempos.isEmpty() ? null
                : tiempos.stream().mapToLong(Duration::toMinutes).average().orElse(0) / 60.0;

        Map<String, Long> cambios = new LinkedHashMap<>();
        EstadoExpediente.secuencia().forEach(e -> cambios.put(e.getNombre(), 0L));
        delPeriodoEventos.stream()
                .filter(ev -> AccionTrazabilidad.CAMBIO_ESTADO.name().equals(ev.getAccion()))
                .map(ev -> estadoDestino(ev.getDetalle()))
                .forEach(destino -> cambios.merge(destino, 1L, Long::sum));

        return new Indicadores(delPeriodo.size(), requisitos, faltantes, inconsistencias,
                tiempos.size(), promedioHoras, cambios);
    }

    /** Obtiene el estado destino del detalle "De X a Y." de un cambio de estado. */
    static String estadoDestino(String detalle) {
        int inicio = detalle.indexOf(" a ") + 3;
        int fin = detalle.indexOf('.', inicio);
        return fin < 0 ? detalle.substring(inicio) : detalle.substring(inicio, fin);
    }

    private static boolean esRevision(String accion) {
        return AccionTrazabilidad.APROBACION_DOCUMENTO.name().equals(accion)
                || AccionTrazabilidad.OBSERVACION.name().equals(accion);
    }

    /**
     * Busca el evento de carga de la misma versión del documento (identificada por la
     * etiqueta "documento – persona (vN)" que comparten ambos eventos) y mide el tiempo.
     */
    private Optional<Duration> tiempoDeRevision(EventoTrazabilidad revision) {
        String clave = claveVersion(revision.getDetalle());
        return eventos.buscarPorExpedienteYAccion(revision.getExpediente().getId(),
                        AccionTrazabilidad.CARGA_DOCUMENTO.name()).stream()
                .filter(carga -> Objects.equals(claveVersion(carga.getDetalle()), clave))
                .filter(carga -> !carga.getFechaHora().isAfter(revision.getFechaHora()))
                .reduce((primero, segundo) -> segundo)
                .map(carga -> Duration.between(carga.getFechaHora(), revision.getFechaHora()));
    }

    private static final Pattern CLAVE_VERSION = Pattern.compile("^(.*?\\(v\\d+\\))");

    static String claveVersion(String detalle) {
        Matcher coincidencia = CLAVE_VERSION.matcher(detalle);
        return coincidencia.find() ? coincidencia.group(1) : detalle;
    }

    /** El expediente pertenece al país filtrado (si se indicó) y al gestor (si se indicó). */
    private static boolean incluido(Expediente expediente, String pais, Long gestorId) {
        boolean pasaPais = pais == null || pais.isBlank() || expediente.getPais().equals(pais);
        boolean pasaGestor = gestorId == null || expediente.getGestor().getId().equals(gestorId);
        return pasaPais && pasaGestor;
    }
}
