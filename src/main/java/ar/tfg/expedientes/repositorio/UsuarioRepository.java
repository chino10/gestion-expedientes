package ar.tfg.expedientes.repositorio;

import ar.tfg.expedientes.dominio.Rol;
import ar.tfg.expedientes.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("select u from Usuario u where lower(u.email) = lower(?1)")
    Optional<Usuario> buscarPorEmail(String email);

    @Query("select u from Usuario u where u.rol = ?1 and u.activo = true order by u.nombre")
    List<Usuario> buscarActivosPorRol(Rol rol);

    default Usuario guardar(Usuario usuario) {
        return save(usuario);
    }
}
