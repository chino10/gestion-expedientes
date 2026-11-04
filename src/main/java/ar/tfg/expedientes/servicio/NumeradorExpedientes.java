package ar.tfg.expedientes.servicio;

import ar.tfg.expedientes.repositorio.ExpedienteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Genera el número de expediente con el formato AAAA-NNNN (CU04, paso 6). */
@Service
public class NumeradorExpedientes {

    private final ExpedienteRepository expedientes;

    public NumeradorExpedientes(ExpedienteRepository expedientes) {
        this.expedientes = expedientes;
    }

    public String siguiente(LocalDate fecha) {
        String anio = String.valueOf(fecha.getYear());
        String ultimo = expedientes.ultimoNumeroDelAnio(anio);
        int siguiente = ultimo == null ? 1 : Integer.parseInt(ultimo.substring(anio.length() + 1)) + 1;
        return String.format("%s-%04d", anio, siguiente);
    }
}
