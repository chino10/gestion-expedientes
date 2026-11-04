package ar.tfg.expedientes.validacion;

import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.VersionDocumento;
import ar.tfg.expedientes.servicio.NotificacionService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Validación preliminar de la documentación (CU06). Selecciona, según el país del
 * expediente, la implementación de {@link ReglaValidacionPais} que corresponde.
 * El resultado es preliminar: la validación final queda a cargo del gestor y del organismo.
 */
@Component
public class ValidadorDocumental {

    private final Map<String, ReglaValidacionPais> reglas;
    private final NotificacionService notificaciones;

    public ValidadorDocumental(List<ReglaValidacionPais> reglas, NotificacionService notificaciones) {
        this.reglas = reglas.stream().collect(Collectors.toMap(ReglaValidacionPais::pais, Function.identity()));
        this.notificaciones = notificaciones;
    }

    public List<Hallazgo> validar(Expediente expediente) {
        Optional<ReglaValidacionPais> regla = obtenerRegla(expediente.getPais());
        if (regla.isEmpty()) {
            // Curso alternativo 2a: el país no tiene reglas configuradas.
            expediente.getDocumentos().stream()
                    .map(d -> d.versionVigente())
                    .filter(v -> v != null)
                    .forEach(v -> v.registrarResultado("PENDIENTE: no hay reglas de validación para "
                            + expediente.getPais() + "."));
            notificaciones.notificarPaisSinReglas(expediente);
            return expediente.getRequisitos().stream()
                    .map(r -> new Hallazgo(r, ResultadoRequisito.PENDIENTE, "Pendiente de validación."))
                    .toList();
        }
        List<Hallazgo> hallazgos = regla.get().validar(expediente);
        for (Hallazgo hallazgo : hallazgos) {
            expediente.documentoDe(hallazgo.requisito()).ifPresent(d -> {
                VersionDocumento vigente = d.versionVigente();
                if (vigente != null) {
                    vigente.registrarResultado(hallazgo.resultado() + ": " + hallazgo.mensaje());
                }
            });
        }
        return hallazgos;
    }

    private Optional<ReglaValidacionPais> obtenerRegla(String pais) {
        return Optional.ofNullable(reglas.get(pais));
    }
}
