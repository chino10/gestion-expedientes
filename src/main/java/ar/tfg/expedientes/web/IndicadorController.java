package ar.tfg.expedientes.web;

import ar.tfg.expedientes.dominio.Rol;
import ar.tfg.expedientes.repositorio.RequisitoRepository;
import ar.tfg.expedientes.seguridad.UsuarioAutenticado;
import ar.tfg.expedientes.servicio.IndicadoresService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Consulta de indicadores de seguimiento (CU12). El gestor consulta los indicadores de sus
 * expedientes asignados y el administrador, los de todos los expedientes.
 */
@Controller
public class IndicadorController {

    private final IndicadoresService indicadores;
    private final RequisitoRepository requisitos;
    private final Clock reloj;

    public IndicadorController(IndicadoresService indicadores, RequisitoRepository requisitos, Clock reloj) {
        this.indicadores = indicadores;
        this.requisitos = requisitos;
        this.reloj = reloj;
    }

    @GetMapping("/indicadores")
    public String consultarIndicadores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String pais,
            @AuthenticationPrincipal UsuarioAutenticado actual, Model modelo) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate fin = hasta == null ? hoy : hasta;
        LocalDate inicio = desde == null ? fin.withDayOfMonth(1) : desde;
        if (inicio.isAfter(fin)) {
            modelo.addAttribute("error", "La fecha inicial no puede ser posterior a la final.");
            inicio = fin;
        }
        modelo.addAttribute("desde", inicio);
        modelo.addAttribute("hasta", fin);
        modelo.addAttribute("pais", pais == null ? "" : pais);
        modelo.addAttribute("paises", requisitos.buscarPaisesYViasVigentes(hoy).stream()
                .map(pv -> pv.substring(0, pv.indexOf('|'))).distinct().sorted().toList());
        Long gestorId = actual.getRol() == Rol.GESTOR ? actual.getId() : null;
        modelo.addAttribute("alcance", gestorId == null ? "Todos los expedientes"
                : "Expedientes asignados a " + actual.getNombre());
        modelo.addAttribute("indicadores", indicadores.calcular(inicio, fin, pais, gestorId));
        return "indicadores";
    }
}
