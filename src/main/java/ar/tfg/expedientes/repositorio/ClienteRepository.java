package ar.tfg.expedientes.repositorio;

import ar.tfg.expedientes.dominio.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("select c from Cliente c where c.documentoIdentidad = ?1")
    Optional<Cliente> buscarPorDocumento(String documentoIdentidad);

    default Cliente guardar(Cliente cliente) {
        return save(cliente);
    }
}
