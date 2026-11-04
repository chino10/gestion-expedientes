package ar.tfg.expedientes.web;

import ar.tfg.expedientes.config.PropiedadesApp;
import ar.tfg.expedientes.dominio.Cliente;
import ar.tfg.expedientes.dominio.EstadoExpediente;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Presentado;
import ar.tfg.expedientes.dominio.ReglaDeNegocioException;
import ar.tfg.expedientes.dominio.Requisito;
import ar.tfg.expedientes.dominio.Resuelto;
import ar.tfg.expedientes.dominio.Rol;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.repositorio.ClienteRepository;
import ar.tfg.expedientes.repositorio.EventoRepository;
import ar.tfg.expedientes.repositorio.ExpedienteRepository;
import ar.tfg.expedientes.repositorio.RequisitoRepository;
import ar.tfg.expedientes.repositorio.UsuarioRepository;
import ar.tfg.expedientes.seguridad.TokenActivacion;
import ar.tfg.expedientes.seguridad.UsuarioAutenticado;
import ar.tfg.expedientes.servicio.AccionTrazabilidad;
import ar.tfg.expedientes.servicio.NotificacionService;
import ar.tfg.expedientes.servicio.NumeradorExpedientes;
import ar.tfg.expedientes.servicio.TrazabilidadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registro de expedientes (CU04), consulta (CU11) y cambio de estado (CU09).
 */
@Controller
public class ExpedienteController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExpedienteRepository expedientes;
    private final ClienteRepository clientes;
    private final RequisitoRepository requisitos;
    private final UsuarioRepository usuarios;
    private final EventoRepository eventos;
    private final TrazabilidadService trazabilidad;
    private final NotificacionService notificaciones;
    private final NumeradorExpedientes numerador;
    private final TokenActivacion tokens;
    private final PasswordEncoder codificador;
    private final Permisos permisos;
    private final PropiedadesApp propiedades;
    private final Clock reloj;

    public ExpedienteController(ExpedienteRepository expedientes, ClienteRepository clientes,
                                RequisitoRepository requisitos, UsuarioRepository usuarios,
                                EventoRepository eventos, TrazabilidadService trazabilidad,
                                NotificacionService notificaciones, NumeradorExpedientes numerador,
                                TokenActivacion tokens, PasswordEncoder codificador, Permisos permisos,
                                PropiedadesApp propiedades, Clock reloj) {
        this.expedientes = expedientes;
        this.clientes = clientes;
        this.requisitos = requisitos;
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.trazabilidad = trazabilidad;
        this.notificaciones = notificaciones;
        this.numerador = numerador;
        this.tokens = tokens;
        this.codificador = codificador;
        this.permisos = permisos;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    // ----------------------------------------------------------------- CU04 Registrar expediente

    @GetMapping("/expedientes/nuevo")
    public String nuevo(Model modelo) {
        modelo.addAttribute("formulario", new FormularioExpediente());
        cargarOpciones(modelo, null, false);
        return "expediente-nuevo";
    }

    /** Pasos 2 y 3: busca al cliente por su documento de identidad. */
    @GetMapping("/clientes")
    public String buscarCliente(@RequestParam String documento, Model modelo) {
        FormularioExpediente formulario = new FormularioExpediente();
        formulario.setDocumento(documento);
        modelo.addAttribute("formulario", formulario);
        cargarOpciones(modelo, clientes.buscarPorDocumento(documento.trim()).orElse(null), true);
        return "expediente-nuevo";
    }

    @PostMapping("/expedientes")
    @Transactional
    public String registrar(@ModelAttribute("formulario") FormularioExpediente formulario,
                            @AuthenticationPrincipal UsuarioAutenticado actual,
                            Model modelo, RedirectAttributes redireccion) {
        Usuario administrativo = permisos.usuario(actual);
        LocalDate hoy = LocalDate.now(reloj);
        Cliente cliente = formulario.getDocumento() == null ? null
                : clientes.buscarPorDocumento(formulario.getDocumento()).orElse(null);
        boolean clienteNuevo = cliente == null;

        List<String> errores = new ArrayList<>(formulario.validar(clienteNuevo));
        if (clienteNuevo && formulario.getEmail() != null
                && usuarios.buscarPorEmail(formulario.getEmail().trim()).isPresent()) {
            errores.add("El correo electrónico ya está registrado para otro usuario.");
        }
        Usuario gestor = formulario.getGestorId() == null ? null
                : usuarios.findById(formulario.getGestorId())
                .filter(u -> u.getRol() == Rol.GESTOR && u.isActivo()).orElse(null);
        if (formulario.getGestorId() != null && gestor == null) {
            errores.add("El gestor seleccionado no es válido.");
        }
        List<Requisito> vigentes = errores.isEmpty()
                ? requisitos.buscarVigentes(formulario.pais(), formulario.via(), hoy) : List.of();
        if (errores.isEmpty() && vigentes.isEmpty()) {
            errores.add("No hay requisitos vigentes para el país y la vía seleccionados.");
        }
        if (!errores.isEmpty()) {
            modelo.addAttribute("errores", errores);
            cargarOpciones(modelo, cliente, true);
            return "expediente-nuevo";
        }

        String enlaceActivacion = null;
        if (clienteNuevo) {
            // Curso alternativo 3a: se registra el cliente y se crea su usuario con rol Cliente.
            Usuario usuarioCliente = usuarios.guardar(new Usuario(
                    formulario.getApellido().trim() + ", " + formulario.getNombre().trim(),
                    formulario.getEmail(), codificador.encode(UUID.randomUUID().toString()), Rol.CLIENTE));
            cliente = clientes.guardar(new Cliente(usuarioCliente, formulario.getApellido(),
                    formulario.getNombre(), formulario.getDocumento(), formulario.getTelefono()));
            trazabilidad.registrarEvento(administrativo, null, AccionTrazabilidad.ALTA_CLIENTE,
                    "Cliente " + cliente.nombreCompleto() + ", documento " + cliente.getDocumentoIdentidad() + ".");
            enlaceActivacion = propiedades.urlBase() + "/activar?token=" + tokens.generar(usuarioCliente);
        }

        Expediente expediente = new Expediente(numerador.siguiente(hoy), cliente, gestor,
                formulario.pais(), formulario.via(), formulario.getAscendiente(), hoy);
        expediente.asociarRequisitos(vigentes);
        expedientes.guardar(expediente);
        trazabilidad.registrarEvento(administrativo, expediente, AccionTrazabilidad.ALTA_EXPEDIENTE,
                "Expediente " + expediente.getNumero() + " (" + expediente.getPais() + ", " + expediente.getVia()
                        + "), gestor asignado: " + gestor.getNombre() + ".");
        notificaciones.enviarAcceso(cliente, expediente, expediente.getRequisitos(), enlaceActivacion);

        redireccion.addFlashAttribute("exito", "Expediente N.º " + expediente.getNumero()
                + " registrado en estado Iniciado. Se envió al cliente el acceso y el listado de requisitos.");
        return "redirect:/expedientes/" + expediente.getId();
    }

    private void cargarOpciones(Model modelo, Cliente cliente, boolean busquedaRealizada) {
        modelo.addAttribute("cliente", cliente);
        modelo.addAttribute("busquedaRealizada", busquedaRealizada);
        modelo.addAttribute("paisesYVias", requisitos.buscarPaisesYViasVigentes(LocalDate.now(reloj))
                .stream().sorted().toList());
        modelo.addAttribute("gestores", usuarios.buscarActivosPorRol(Rol.GESTOR));
    }

    // ----------------------------------------------------------------- CU11 Consultar expediente

    @GetMapping("/expedientes/{id}")
    @Transactional(readOnly = true)
    public String consultar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual, Model modelo) {
        Usuario usuario = permisos.usuario(actual);
        Expediente expediente = expedientes.findById(id).orElseThrow();
        permisos.exigirConsulta(usuario, expediente);
        modelo.addAttribute("expediente", expediente);
        modelo.addAttribute("filas", FilaRequisito.de(expediente));
        modelo.addAttribute("secuencia", EstadoExpediente.secuencia());
        modelo.addAttribute("observaciones", expediente.observacionesPendientes());
        modelo.addAttribute("faltantes", expediente.requisitosSinDocumento().size());
        modelo.addAttribute("historial", EventoVista.de(eventos.buscarPorExpediente(id)));
        modelo.addAttribute("puedeCargar", permisos.puedeCargar(usuario, expediente));
        modelo.addAttribute("puedeRevisar", permisos.puedeRevisar(usuario, expediente));
        return "expediente";
    }

    // ----------------------------------------------------------------- CU09 Cambiar estado

    @PostMapping("/expedientes/{id}/estado")
    @Transactional
    public String cambiarEstado(@PathVariable Long id, @RequestParam String destino,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaTurno,
                                @RequestParam(required = false) String resolucion,
                                @AuthenticationPrincipal UsuarioAutenticado actual,
                                RedirectAttributes redireccion) {
        Usuario gestor = permisos.usuario(actual);
        Expediente expediente = expedientes.findById(id).orElseThrow();
        permisos.exigirRevision(gestor, expediente);
        EstadoExpediente origen = expediente.getEstado();
        EstadoExpediente nuevo = EstadoExpediente.desdeCodigo(destino);
        try {
            if (nuevo instanceof Presentado) {
                expediente.registrarTurno(fechaTurno);
            }
            if (nuevo instanceof Resuelto) {
                expediente.registrarResolucion(resolucion);
            }
            expediente.avanzarA(nuevo);
        } catch (ReglaDeNegocioException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            redireccion.addFlashAttribute("error", e.getMessage());
            return "redirect:/expedientes/" + id + "/revision";
        }
        expedientes.guardar(expediente);
        String detalle = "De " + origen.getNombre() + " a " + nuevo.getNombre() + ".";
        if (nuevo instanceof Presentado) {
            detalle += " Turno consular: " + expediente.getFechaTurno().format(FECHA) + ".";
        }
        if (nuevo instanceof Resuelto) {
            detalle += " Resolución: " + expediente.getResolucion();
        }
        trazabilidad.registrarEvento(gestor, expediente, AccionTrazabilidad.CAMBIO_ESTADO, detalle);
        notificaciones.notificarCambioEstado(expediente);
        redireccion.addFlashAttribute("exito", "El expediente pasó a " + nuevo.getNombre() + ".");
        return "redirect:/expedientes/" + id + "/revision";
    }
}
