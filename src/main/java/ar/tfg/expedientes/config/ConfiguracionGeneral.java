package ar.tfg.expedientes.config;

import ar.tfg.expedientes.GestionExpedientesApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ConfiguracionGeneral {

    /** Reloj del servidor en la hora de Argentina, usado para fechas de carga y eventos. */
    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of(GestionExpedientesApplication.ZONA_HORARIA));
    }
}
