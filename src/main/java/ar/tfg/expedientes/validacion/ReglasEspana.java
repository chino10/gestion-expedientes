package ar.tfg.expedientes.validacion;

import ar.tfg.expedientes.dominio.Expediente;
import org.springframework.stereotype.Component;

import java.util.List;

/** Reglas de validación documental para expedientes de nacionalidad española. */
@Component
public class ReglasEspana implements ReglaValidacionPais {

    @Override
    public String pais() {
        return "España";
    }

    @Override
    public List<Hallazgo> validar(Expediente expediente) {
        return expediente.getRequisitos().stream()
                .map(r -> ControlesDocumentales.controlar(expediente, r, "Falta la traducción al español."))
                .toList();
    }
}
