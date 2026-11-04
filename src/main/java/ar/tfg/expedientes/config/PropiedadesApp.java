package ar.tfg.expedientes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros propios de la aplicación, definidos en application.properties con el prefijo "app".
 */
@ConfigurationProperties(prefix = "app")
public record PropiedadesApp(
        String almacenamiento,
        int bcryptFuerza,
        int maxIntentosFallidos,
        String tokenSecreto,
        String urlBase,
        boolean datosEjemplo) {
}
