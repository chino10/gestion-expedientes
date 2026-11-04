package ar.tfg.expedientes.repositorio;

import ar.tfg.expedientes.dominio.EventoTrazabilidad;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de eventos de trazabilidad. Extiende {@link Repository} y no JpaRepository
 * para no exponer operaciones de eliminación (RNF03).
 */
public interface EventoRepository extends Repository<EventoTrazabilidad, Long> {

    EventoTrazabilidad save(EventoTrazabilidad evento);

    @Query("select ev from EventoTrazabilidad ev where ev.expediente.id = ?1 order by ev.fechaHora, ev.id")
    List<EventoTrazabilidad> buscarPorExpediente(Long expedienteId);

    @Query("select ev from EventoTrazabilidad ev where ev.fechaHora >= ?1 and ev.fechaHora < ?2 order by ev.fechaHora, ev.id")
    List<EventoTrazabilidad> buscarPorPeriodo(LocalDateTime desde, LocalDateTime hasta);

    @Query("select ev from EventoTrazabilidad ev where ev.expediente.id = ?1 and ev.accion = ?2 order by ev.fechaHora, ev.id")
    List<EventoTrazabilidad> buscarPorExpedienteYAccion(Long expedienteId, String accion);

    default EventoTrazabilidad guardar(EventoTrazabilidad evento) {
        return save(evento);
    }
}
