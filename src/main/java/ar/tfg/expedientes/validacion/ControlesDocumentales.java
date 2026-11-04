package ar.tfg.expedientes.validacion;

import ar.tfg.expedientes.dominio.Documento;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Requisito;
import ar.tfg.expedientes.dominio.VersionDocumento;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controles que comparten las reglas de los distintos países: existencia del documento,
 * apostilla, traducción y coincidencia del apellido y el nombre declarados (CU06, pasos 3 a 5).
 */
final class ControlesDocumentales {

    static final String PERSONA_SOLICITANTE = "Solicitante";
    static final String PERSONA_ASCENDIENTE = "Ascendiente";

    private ControlesDocumentales() {
    }

    /**
     * Aplica los controles a un requisito. El mensaje de traducción faltante lo define cada país.
     */
    static Hallazgo controlar(Expediente expediente, Requisito requisito, String mensajeTraduccion) {
        Optional<Documento> documento = expediente.documentoDe(requisito);
        if (documento.isEmpty() || documento.get().versionVigente() == null) {
            return new Hallazgo(requisito, ResultadoRequisito.FALTANTE, "Falta cargar el documento.");
        }
        VersionDocumento version = documento.get().versionVigente();
        List<String> problemas = new ArrayList<>();
        if (requisito.isRequiereApostilla() && !version.isDeclaraApostilla()) {
            problemas.add("Falta la apostilla de La Haya.");
        }
        if (requisito.isRequiereTraduccion() && !version.isDeclaraTraduccion()) {
            problemas.add(mensajeTraduccion);
        }
        verificarIdentidad(expediente, requisito, version).ifPresent(problemas::add);
        return problemas.isEmpty()
                ? new Hallazgo(requisito, ResultadoRequisito.VALIDADO, "Sin inconsistencias.")
                : new Hallazgo(requisito, ResultadoRequisito.INCONSISTENTE, String.join(" ", problemas));
    }

    /**
     * Compara el apellido y el nombre declarados con los registrados para la persona:
     * el cliente para el solicitante, el ascendiente del expediente para el ascendiente
     * y, para las demás personas, los declarados en sus otros documentos.
     */
    static Optional<String> verificarIdentidad(Expediente expediente, Requisito requisito, VersionDocumento version) {
        String declarado = version.getApellidoDeclarado() + " " + version.getNombreDeclarado();
        String persona = requisito.getPersona();
        if (PERSONA_SOLICITANTE.equalsIgnoreCase(persona)) {
            String registrado = expediente.getCliente().getApellido() + " " + expediente.getCliente().getNombre();
            return mismasPalabras(declarado, registrado) ? Optional.empty()
                    : Optional.of(diferencia(version, persona, expediente.getCliente().nombreCompleto()));
        }
        if (PERSONA_ASCENDIENTE.equalsIgnoreCase(persona)) {
            return mismasPalabras(declarado, expediente.getAscendiente()) ? Optional.empty()
                    : Optional.of(diferencia(version, persona, expediente.getAscendiente()));
        }
        return expediente.getDocumentos().stream()
                .filter(d -> d.getRequisito().getPersona().equalsIgnoreCase(persona))
                .map(Documento::versionVigente)
                .filter(v -> v != null && v != version)
                .filter(v -> !mismasPalabras(declarado, v.getApellidoDeclarado() + " " + v.getNombreDeclarado()))
                .findFirst()
                .map(v -> diferencia(version, persona, v.getApellidoDeclarado() + ", " + v.getNombreDeclarado()
                        + " en " + v.getDocumento().getRequisito().getDocumento().toLowerCase()));
    }

    private static String diferencia(VersionDocumento version, String persona, String registrado) {
        return "El apellido y el nombre declarados (" + version.getApellidoDeclarado() + ", "
                + version.getNombreDeclarado() + ") no coinciden con los registrados para "
                + persona.toLowerCase() + " (" + registrado + ").";
    }

    /** Compara dos nombres sin tener en cuenta mayúsculas, acentos, comas ni el orden de las palabras. */
    static boolean mismasPalabras(String a, String b) {
        return palabras(a).equals(palabras(b));
    }

    private static Set<String> palabras(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return Arrays.stream(sinAcentos.toLowerCase().split("[\\s,]+"))
                .filter(p -> !p.isBlank())
                .collect(Collectors.toSet());
    }
}
