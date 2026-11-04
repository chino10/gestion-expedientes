package ar.tfg.expedientes.repositorio;

import ar.tfg.expedientes.dominio.Expediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {

    @Query("select e from Expediente e order by e.numero desc")
    List<Expediente> buscarTodos();

    @Query("select e from Expediente e where e.cliente.usuario.id = ?1 order by e.numero desc")
    List<Expediente> buscarPorUsuarioCliente(Long usuarioId);

    @Query("select e from Expediente e where e.gestor.id = ?1 order by e.numero desc")
    List<Expediente> buscarPorGestor(Long gestorId);

    @Query("select max(e.numero) from Expediente e where e.numero like concat(?1, '-%')")
    String ultimoNumeroDelAnio(String anio);

    /** Expedientes iniciados hasta el fin del período y no resueltos antes de su inicio. */
    @Query("select e from Expediente e where e.fechaInicio <= ?1 order by e.numero")
    List<Expediente> buscarIniciadosHasta(LocalDate fecha);

    default Expediente guardar(Expediente expediente) {
        return save(expediente);
    }
}
