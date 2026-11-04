package ar.tfg.expedientes;

import ar.tfg.expedientes.config.DatosDeEjemplo;
import ar.tfg.expedientes.dominio.Documento;
import ar.tfg.expedientes.dominio.EstadoRevision;
import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Requisito;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.dominio.VersionDocumento;
import ar.tfg.expedientes.repositorio.EventoRepository;
import ar.tfg.expedientes.repositorio.ExpedienteRepository;
import ar.tfg.expedientes.repositorio.UsuarioRepository;
import ar.tfg.expedientes.seguridad.TokenActivacion;
import ar.tfg.expedientes.seguridad.UsuarioAutenticado;
import ar.tfg.expedientes.servicio.Indicadores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Prueba de punta a punta del núcleo del prototipo, desde el alta del expediente hasta el
 * registro de la resolución, sobre una base de datos en memoria con los datos de ejemplo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoExpedienteTest {

    private static final String VIA = DatosDeEjemplo.VIA_ITALIA;
    private static final byte[] PDF = "%PDF-1.4\n%prueba\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private ExpedienteRepository expedientes;
    @Autowired
    private EventoRepository eventos;
    @Autowired
    private TokenActivacion tokens;
    @Autowired
    private TransactionTemplate transaccion;

    private RequestPostProcessor como(String email) {
        Usuario usuario = usuarios.buscarPorEmail(email).orElseThrow();
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private void enTransaccion(Long expedienteId, Consumer<Expediente> accion) {
        transaccion.executeWithoutResult(s -> accion.accept(expedientes.findById(expedienteId).orElseThrow()));
    }

    private Long registrarExpediente(String documento, String apellido, String nombre, String email) throws Exception {
        Long gestor = usuarios.buscarPorEmail("mlopez@estudio.test").orElseThrow().getId();
        MvcResult resultado = mvc.perform(post("/expedientes").with(csrf()).with(como("lgomez@estudio.test"))
                        .param("documento", documento).param("apellido", apellido).param("nombre", nombre)
                        .param("email", email).param("telefono", "11 5555-0000")
                        .param("ascendiente", "Rossi, Mario").param("paisVia", "Italia|" + VIA)
                        .param("gestorId", gestor.toString()))
                .andExpect(status().is3xxRedirection()).andReturn();
        String destino = resultado.getResponse().getRedirectedUrl();
        return Long.valueOf(destino.substring(destino.lastIndexOf('/') + 1));
    }

    private void cargar(Long expedienteId, Long requisitoId, String usuario, String apellido, String nombre,
                        boolean apostilla, boolean traduccion) throws Exception {
        mvc.perform(multipart("/expedientes/{id}/documentos", expedienteId)
                        .file(new MockMultipartFile("archivo", "acta.pdf", "application/pdf", PDF))
                        .param("requisito", requisitoId.toString()).param("apellido", apellido).param("nombre", nombre)
                        .param("apostilla", String.valueOf(apostilla)).param("traduccion", String.valueOf(traduccion))
                        .with(csrf()).with(como(usuario)))
                .andExpect(status().is3xxRedirection());
    }

    private void cambiarEstado(Long id, String destino, String... extra) throws Exception {
        var solicitud = post("/expedientes/{id}/estado", id).with(csrf()).with(como("mlopez@estudio.test"))
                .param("destino", destino);
        for (int i = 0; i < extra.length; i += 2) {
            solicitud.param(extra[i], extra[i + 1]);
        }
        mvc.perform(solicitud).andExpect(status().is3xxRedirection());
    }

    private String estado(Long id) {
        return transaccion.execute(s -> expedientes.findById(id).orElseThrow().getEstado().getCodigo());
    }

    // ------------------------------------------------------------------------------- CU01

    @Test
    void inicioDeSesionYBloqueoPorIntentosFallidos() throws Exception {
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("clave")
                        .user("admin@estudio.test").password(DatosDeEjemplo.CLAVE_DEMO))
                .andExpect(redirectedUrl("/"));
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("clave")
                        .user("noexiste@estudio.test").password("cualquier-clave-larga"))
                .andExpect(redirectedUrl("/login?error"));

        for (int intento = 1; intento <= 4; intento++) {
            mvc.perform(formLogin("/login").userParameter("email").passwordParam("clave")
                            .user("pfernandez@estudio.test").password("clave-equivocada-numero-" + intento))
                    .andExpect(redirectedUrl("/login?error"));
        }
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("clave")
                        .user("pfernandez@estudio.test").password("clave-equivocada-numero-5"))
                .andExpect(redirectedUrl("/login?bloqueada"));
        // Con la cuenta bloqueada, ni siquiera la contraseña correcta permite ingresar.
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("clave")
                        .user("pfernandez@estudio.test").password(DatosDeEjemplo.CLAVE_DEMO))
                .andExpect(redirectedUrl("/login?bloqueada"));
        assertThat(usuarios.buscarPorEmail("pfernandez@estudio.test").orElseThrow().getIntentosFallidos()).isEqualTo(5);
        assertThat(eventos.buscarPorPeriodo(LocalDate.now().minusDays(1).atStartOfDay(), LocalDate.now().plusDays(2).atStartOfDay()))
                .anyMatch(ev -> ev.getAccion().equals("BLOQUEO_CUENTA"));
    }

    // ------------------------------------------------------------------------------- CU04 a CU12

    @Test
    void flujoCompletoDelExpediente() throws Exception {
        // CU04: alta del expediente con un cliente nuevo.
        Long id = registrarExpediente("40111222", "Rossi", "Lucía", "lucia.rossi@correo.test");
        List<Long> requisitos = transaccion.execute(s ->
                expedientes.findById(id).orElseThrow().getRequisitos().stream().map(Requisito::getId).toList());
        assertThat(requisitos).hasSize(7);
        assertThat(estado(id)).isEqualTo("INICIADO");
        assertThat(eventos.buscarPorExpediente(id)).extracting("accion").containsExactly("ALTA_EXPEDIENTE");

        // Activación de la cuenta del cliente con el enlace de un solo uso.
        Usuario cliente = usuarios.buscarPorEmail("lucia.rossi@correo.test").orElseThrow();
        String token = tokens.generar(cliente);
        mvc.perform(post("/activar").with(csrf()).param("token", token)
                        .param("clave", "mi frase secreta del expediente").param("confirmacion", "mi frase secreta del expediente"))
                .andExpect(redirectedUrl("/login?activada"));
        assertThat(tokens.verificar(token)).isEmpty();
        mvc.perform(formLogin("/login").userParameter("email").passwordParam("clave")
                        .user("lucia.rossi@correo.test").password("mi frase secreta del expediente"))
                .andExpect(redirectedUrl("/"));

        // CU05 y CU06: archivo con formato no permitido.
        mvc.perform(multipart("/expedientes/{id}/documentos", id)
                        .file(new MockMultipartFile("archivo", "acta.txt", "text/plain", "texto".getBytes()))
                        .param("requisito", requisitos.get(0).toString()).param("apellido", "Rossi").param("nombre", "Lucía")
                        .with(csrf()).with(como("lucia.rossi@correo.test")))
                .andExpect(flash().attribute("error", "Formato no permitido. Se admiten archivos PDF, JPG o PNG."));

        // Acta del solicitante con un nombre distinto del registrado: inconsistente.
        cargar(id, requisitos.get(0), "lucia.rossi@correo.test", "Rossi", "Lucia Maria", true, true);
        enTransaccion(id, e -> {
            VersionDocumento v = e.getDocumentos().get(0).versionVigente();
            assertThat(v.tipoResultado()).isEqualTo("INCONSISTENTE");
            assertThat(v.detalleResultado()).contains("no coinciden");
        });

        // CU07/CU08: no se revisa antes de pasar a En revisión.
        Long documento = transaccion.execute(s -> expedientes.findById(id).orElseThrow().getDocumentos().get(0).getId());
        mvc.perform(post("/documentos/{id}/aprobar", documento).with(csrf()).with(como("mlopez@estudio.test")))
                .andExpect(flash().attribute("error", "Para revisar documentos el expediente debe estar En revisión."));

        // CU09: Iniciado -> En revisión; no se puede saltar a Completo con requisitos pendientes.
        cambiarEstado(id, "EN_REVISION");
        assertThat(estado(id)).isEqualTo("EN_REVISION");
        cambiarEstado(id, "COMPLETO");
        assertThat(estado(id)).isEqualTo("EN_REVISION");

        // CU08: observación y nueva versión del cliente, que la resuelve.
        mvc.perform(post("/documentos/{id}/observaciones", documento).param("texto", "El nombre no coincide con el DNI.")
                        .with(csrf()).with(como("mlopez@estudio.test")))
                .andExpect(status().is3xxRedirection());
        enTransaccion(id, e -> {
            assertThat(e.observacionesPendientes()).hasSize(1);
            assertThat(e.getDocumentos().get(0).getEstadoRevision()).isEqualTo(EstadoRevision.OBSERVADO);
        });
        cargar(id, requisitos.get(0), "lucia.rossi@correo.test", "Rossi", "Lucía", true, true);
        enTransaccion(id, e -> {
            Documento d = e.getDocumentos().get(0);
            assertThat(d.getVersiones()).hasSize(2);
            assertThat(d.versionVigente().tipoResultado()).isEqualTo("VALIDADO");
            assertThat(e.observacionesPendientes()).isEmpty();
        });

        // El administrativo carga el resto; el acta del ascendiente se compara con el ascendiente.
        for (int i = 1; i < requisitos.size(); i++) {
            boolean ascendiente = i >= 3;
            cargar(id, requisitos.get(i), "lgomez@estudio.test", ascendiente ? "Rossi" : "Bianchi",
                    ascendiente ? "Mario" : "Carla", true, true);
        }
        enTransaccion(id, e -> assertThat(e.getDocumentos()).allMatch(d -> d.versionVigente().tipoResultado().equals("VALIDADO")));

        // CU07: el gestor aprueba todos los documentos y el expediente pasa a Completo.
        List<Long> documentos = transaccion.execute(s ->
                expedientes.findById(id).orElseThrow().getDocumentos().stream().map(Documento::getId).toList());
        for (Long d : documentos) {
            mvc.perform(post("/documentos/{id}/aprobar", d).with(csrf()).with(como("mlopez@estudio.test")))
                    .andExpect(status().is3xxRedirection());
        }
        cambiarEstado(id, "COMPLETO");
        assertThat(estado(id)).isEqualTo("COMPLETO");

        // Presentado exige la fecha del turno y Resuelto, la resolución.
        cambiarEstado(id, "PRESENTADO");
        assertThat(estado(id)).isEqualTo("COMPLETO");
        cambiarEstado(id, "PRESENTADO", "fechaTurno", LocalDate.now().plusDays(30).toString());
        cambiarEstado(id, "RESUELTO", "resolucion", "Ciudadanía reconocida.");
        assertThat(estado(id)).isEqualTo("RESUELTO");

        // CU11: la consulta muestra el historial; CU10: los eventos quedaron registrados.
        mvc.perform(get("/expedientes/{id}", id).with(como("lucia.rossi@correo.test")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ciudadanía reconocida.")));
        assertThat(eventos.buscarPorExpediente(id)).extracting("accion")
                .contains("ALTA_EXPEDIENTE", "CARGA_DOCUMENTO", "OBSERVACION", "APROBACION_DOCUMENTO", "CAMBIO_ESTADO");

        // CU12: indicadores del período.
        MvcResult indicadores = mvc.perform(get("/indicadores").with(como("mlopez@estudio.test")))
                .andExpect(status().isOk()).andReturn();
        Indicadores valores = (Indicadores) indicadores.getModelAndView().getModel().get("indicadores");
        assertThat(valores.inconsistencias()).isGreaterThanOrEqualTo(1);
        assertThat(valores.revisiones()).isGreaterThanOrEqualTo(8);
        assertThat(valores.cambiosDeEstado().get("Resuelto")).isEqualTo(1L);

        // CU12, comentarios: el gestor solo ve sus expedientes asignados; el administrador, todos.
        Indicadores otroGestor = (Indicadores) mvc.perform(get("/indicadores").with(como("pfernandez@estudio.test")))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("indicadores");
        assertThat(otroGestor.revisiones()).isZero();
        assertThat(otroGestor.cambiosDeEstado().get("Resuelto")).isZero();
        Indicadores administrador = (Indicadores) mvc.perform(get("/indicadores").with(como("admin@estudio.test")))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("indicadores");
        assertThat(administrador.cambiosDeEstado().get("Resuelto")).isEqualTo(1L);
        assertThat(administrador.expedientes()).isGreaterThanOrEqualTo(valores.expedientes());
    }

    // ------------------------------------------------------------------------------- Permisos

    @Test
    void elClienteSoloAccedeASusExpedientes() throws Exception {
        Long ajeno = registrarExpediente("40999888", "Bruno", "Sofía", "sofia.bruno@correo.test");
        mvc.perform(get("/expedientes/{id}", ajeno).with(como("ana.perez@correo.test")))
                .andExpect(status().isForbidden());
        assertThat(eventos.buscarPorExpediente(ajeno)).extracting("accion").contains("ACCESO_DENEGADO");
        mvc.perform(get("/expedientes/nuevo").with(como("ana.perez@correo.test")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/").with(como("ana.perez@correo.test"))).andExpect(status().isOk());
    }

    @Test
    void lasPantallasPrincipalesSeMuestran() throws Exception {
        Long id = transaccion.execute(s -> expedientes.buscarTodos().stream()
                .filter(e -> e.getCliente().getDocumentoIdentidad().equals("30111222")).findFirst().orElseThrow().getId());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/expedientes/nuevo").with(como("lgomez@estudio.test"))).andExpect(status().isOk());
        mvc.perform(get("/clientes").param("documento", "30111222").with(como("lgomez@estudio.test")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pérez, Ana")));
        mvc.perform(get("/expedientes/{id}/documentos/nuevo", id).with(como("ana.perez@correo.test"))).andExpect(status().isOk());
        mvc.perform(get("/expedientes/{id}/revision", id).with(como("mlopez@estudio.test"))).andExpect(status().isOk());
        mvc.perform(get("/expedientes/{id}", id).with(como("admin@estudio.test"))).andExpect(status().isOk());
        mvc.perform(get("/activar").param("token", "invalido")).andExpect(status().isOk());
    }
}
