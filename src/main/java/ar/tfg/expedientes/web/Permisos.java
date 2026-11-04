package ar.tfg.expedientes.web;

import ar.tfg.expedientes.dominio.Expediente;
import ar.tfg.expedientes.dominio.Rol;
import ar.tfg.expedientes.dominio.Usuario;
import ar.tfg.expedientes.repositorio.UsuarioRepository;
import ar.tfg.expedientes.seguridad.UsuarioAutenticado;
import ar.tfg.expedientes.servicio.TrazabilidadService;
import org.springframework.stereotype.Component;

/**
 * Controles de acceso sobre cada expediente (Tabla 20 y RNF02): el cliente solo accede a sus
 * expedientes y el gestor, a los que tiene asignados.
 */
@Component
public class Permisos {

    private final UsuarioRepository usuarios;
    private final TrazabilidadService trazabilidad;

    public Permisos(UsuarioRepository usuarios, TrazabilidadService trazabilidad) {
        this.usuarios = usuarios;
        this.trazabilidad = trazabilidad;
    }

    public Usuario usuario(UsuarioAutenticado autenticado) {
        return usuarios.findById(autenticado.getId()).orElseThrow(AccesoDenegadoException::new);
    }

    public boolean puedeConsultar(Usuario usuario, Expediente expediente) {
        return switch (usuario.getRol()) {
            case ADMINISTRADOR, ADMINISTRATIVO -> true;
            case GESTOR -> expediente.getGestor().getId().equals(usuario.getId());
            case CLIENTE -> expediente.getCliente().getUsuario().getId().equals(usuario.getId());
        };
    }

    public boolean puedeCargar(Usuario usuario, Expediente expediente) {
        boolean rolHabilitado = usuario.getRol() == Rol.ADMINISTRATIVO
                || (usuario.getRol() == Rol.CLIENTE && puedeConsultar(usuario, expediente));
        return rolHabilitado && expediente.permiteCarga();
    }

    public boolean puedeRevisar(Usuario usuario, Expediente expediente) {
        return usuario.getRol() == Rol.GESTOR && expediente.getGestor().getId().equals(usuario.getId());
    }

    /** Verifica el permiso de consulta y, si no lo tiene, registra el intento (CU11, curso 2a). */
    public void exigirConsulta(Usuario usuario, Expediente expediente) {
        if (!puedeConsultar(usuario, expediente)) {
            denegar(usuario, expediente);
        }
    }

    public void exigirRevision(Usuario usuario, Expediente expediente) {
        if (!puedeRevisar(usuario, expediente)) {
            denegar(usuario, expediente);
        }
    }

    public void exigirCarga(Usuario usuario, Expediente expediente) {
        if (!puedeConsultar(usuario, expediente) && usuario.getRol() != Rol.ADMINISTRATIVO) {
            denegar(usuario, expediente);
        }
    }

    private void denegar(Usuario usuario, Expediente expediente) {
        trazabilidad.registrarIntentoDenegado(usuario, expediente,
                "Intento de acceso sin permiso al expediente " + expediente.getNumero() + ".");
        throw new AccesoDenegadoException();
    }
}
