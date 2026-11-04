package ar.tfg.expedientes.config;

import ar.tfg.expedientes.dominio.Cliente;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Requisito;
import ar.tfg.expedientes.dominio.Rol;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.repositorio.ClienteRepository;
import ar.tfg.expedientes.repositorio.ExpedienteRepository;
import ar.tfg.expedientes.repositorio.RequisitoRepository;
import ar.tfg.expedientes.repositorio.UsuarioRepository;
import ar.tfg.expedientes.servicio.AccionTrazabilidad;
import ar.tfg.expedientes.servicio.TrazabilidadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga datos de ejemplo para la demostración cuando la base de datos está vacía:
 * un usuario por rol, los requisitos de una vía de Italia y una de España, y un expediente
 * en estado Iniciado. Se desactiva con app.datos-ejemplo=false.
 * Las cuentas de ejemplo usan una contraseña común solo para la demostración; en el sistema
 * cada usuario define la suya mediante el enlace de activación.
 */
@Component
public class DatosDeEjemplo implements ApplicationRunner {

    public static final String CLAVE_DEMO = "Prototipo-Expedientes-2026";
    public static final String VIA_ITALIA = "Descendencia (iure sanguinis)";
    public static final String VIA_ESPANA = "Opción (art. 20 del Código Civil)";

    private static final Logger LOG = LoggerFactory.getLogger(DatosDeEjemplo.class);

    private final PropiedadesApp propiedades;
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final RequisitoRepository requisitos;
    private final ExpedienteRepository expedientes;
    private final TrazabilidadService trazabilidad;
    private final PasswordEncoder codificador;
    private final Clock reloj;

    public DatosDeEjemplo(PropiedadesApp propiedades, UsuarioRepository usuarios, ClienteRepository clientes,
                          RequisitoRepository requisitos, ExpedienteRepository expedientes,
                          TrazabilidadService trazabilidad, PasswordEncoder codificador, Clock reloj) {
        this.propiedades = propiedades;
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.requisitos = requisitos;
        this.expedientes = expedientes;
        this.trazabilidad = trazabilidad;
        this.codificador = codificador;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (!propiedades.datosEjemplo() || usuarios.count() > 0) {
            return;
        }
        String hash = codificador.encode(CLAVE_DEMO);
        usuarios.guardar(new Usuario("Administrador del estudio", "admin@estudio.test", hash, Rol.ADMINISTRADOR));
        Usuario administrativo = usuarios.guardar(new Usuario("Gómez, Luis", "lgomez@estudio.test", hash, Rol.ADMINISTRATIVO));
        Usuario gestor = usuarios.guardar(new Usuario("López, Marta", "mlopez@estudio.test", hash, Rol.GESTOR));
        usuarios.guardar(new Usuario("Fernández, Pablo", "pfernandez@estudio.test", hash, Rol.GESTOR));
        Usuario usuarioCliente = usuarios.guardar(new Usuario("Pérez, Ana", "ana.perez@correo.test", hash, Rol.CLIENTE));
        Cliente cliente = clientes.guardar(new Cliente(usuarioCliente, "Pérez", "Ana", "30111222", "11 5555-0101"));

        // Requisitos de ejemplo: documentación emitida en Argentina con apostilla y traducción;
        // el acta de nacimiento del ascendiente, emitida en Italia, no las requiere.
        LocalDate desdeItalia = LocalDate.of(2025, 3, 28);
        List<Requisito> italia = List.of(
                new Requisito("Italia", VIA_ITALIA, "Acta de nacimiento", "Solicitante", true, true, desdeItalia),
                new Requisito("Italia", VIA_ITALIA, "Acta de nacimiento", "Progenitor", true, true, desdeItalia),
                new Requisito("Italia", VIA_ITALIA, "Acta de matrimonio", "Progenitor", true, true, desdeItalia),
                new Requisito("Italia", VIA_ITALIA, "Acta de nacimiento", "Ascendiente", false, false, desdeItalia),
                new Requisito("Italia", VIA_ITALIA, "Acta de matrimonio", "Ascendiente", true, true, desdeItalia),
                new Requisito("Italia", VIA_ITALIA, "Acta de defunción", "Ascendiente", true, true, desdeItalia),
                new Requisito("Italia", VIA_ITALIA, "Certificado de no naturalización", "Ascendiente", true, true, desdeItalia));
        LocalDate desdeEspana = LocalDate.of(2025, 10, 23);
        List<Requisito> espana = List.of(
                new Requisito("España", VIA_ESPANA, "Acta de nacimiento", "Solicitante", true, false, desdeEspana),
                new Requisito("España", VIA_ESPANA, "Certificación literal de nacimiento", "Progenitor", false, false, desdeEspana));
        requisitos.saveAll(italia);
        requisitos.saveAll(espana);

        LocalDate hoy = LocalDate.now(reloj);
        Expediente expediente = new Expediente(hoy.getYear() + "-0001", cliente, gestor, "Italia", VIA_ITALIA,
                "Pérez, Giuseppe", hoy);
        expediente.asociarRequisitos(requisitos.buscarVigentes("Italia", VIA_ITALIA, hoy));
        expedientes.guardar(expediente);
        trazabilidad.registrarEvento(administrativo, expediente, AccionTrazabilidad.ALTA_EXPEDIENTE,
                "Expediente " + expediente.getNumero() + " (" + expediente.getPais() + ", " + expediente.getVia()
                        + "), gestor asignado: " + gestor.getNombre() + ".");
        LOG.info("Datos de ejemplo cargados. Contraseña de las cuentas de demostración: {}", CLAVE_DEMO);
    }
}
