package ar.tfg.expedientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

/**
 * Punto de entrada del prototipo "Sistema web de gestión trazable de expedientes
 * de solicitud de ciudadanía europea".
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GestionExpedientesApplication {

    /** Zona horaria del estudio, usada en las fechas y horas que registra el sistema. */
    public static final String ZONA_HORARIA = "America/Argentina/Buenos_Aires";

    public static void main(String[] args) {
        // Se fija con el identificador canónico de la base IANA: algunos servidores PostgreSQL
        // rechazan alias antiguos como "America/Buenos_Aires" al abrir la conexión.
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
        SpringApplication.run(GestionExpedientesApplication.class, args);
    }
}
