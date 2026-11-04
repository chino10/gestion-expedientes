package ar.tfg.expedientes.seguridad;

import ar.tfg.expedientes.config.PropiedadesApp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Configuración de seguridad: autenticación por formulario, autorización por rol según la
 * Tabla 20 y cifrado de contraseñas con BCrypt (política 6).
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    @Bean
    public PasswordEncoder codificadorContrasenas(PropiedadesApp propiedades) {
        int fuerza = Math.max(10, propiedades.bcryptFuerza()); // OWASP: factor de trabajo mínimo 10
        return new BCryptPasswordEncoder(fuerza);
    }

    /**
     * {@link ServicioAutenticacion} es el único AuthenticationProvider del contexto, por lo que Spring Security
     * lo registra en el AuthenticationManager global. No se agrega también a la cadena: si estuviera en los dos
     * administradores, cada intento fallido se contaría dos veces.
     */
    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/login", "/activar", "/css/**", "/error").permitAll()
                        .requestMatchers("/clientes", "/expedientes/nuevo").hasRole("ADMINISTRATIVO")
                        .requestMatchers(HttpMethod.POST, "/expedientes").hasRole("ADMINISTRATIVO")
                        .requestMatchers("/expedientes/*/revision", "/expedientes/*/estado", "/documentos/**")
                        .hasRole("GESTOR")
                        .requestMatchers("/indicadores").hasAnyRole("GESTOR", "ADMINISTRADOR")
                        .anyRequest().authenticated())
                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("clave")
                        .defaultSuccessUrl("/", true)
                        .failureHandler(manejadorDeFallas())
                        .permitAll())
                .logout(salida -> salida
                        .logoutUrl("/salir")
                        .logoutSuccessUrl("/login?salida")
                        .deleteCookies("JSESSIONID"))
                // Política 7: nuevo identificador de sesión al iniciar sesión.
                .sessionManagement(sesion -> sesion.sessionFixation(fijacion -> fijacion.changeSessionId()))
                .exceptionHandling(excepciones -> excepciones.accessDeniedPage("/acceso-denegado"));
        return http.build();
    }

    private AuthenticationFailureHandler manejadorDeFallas() {
        return (solicitud, respuesta, excepcion) -> respuesta.sendRedirect(
                solicitud.getContextPath() + (excepcion instanceof LockedException ? "/login?bloqueada" : "/login?error"));
    }
}
