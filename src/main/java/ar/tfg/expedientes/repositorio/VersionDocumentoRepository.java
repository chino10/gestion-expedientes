package ar.tfg.expedientes.repositorio;

import ar.tfg.expedientes.dominio.VersionDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface VersionDocumentoRepository extends JpaRepository<VersionDocumento, Long> {

    @Query("select v from VersionDocumento v where v.fechaCarga >= ?1 and v.fechaCarga < ?2")
    List<VersionDocumento> buscarCargadasEntre(LocalDateTime desde, LocalDateTime hasta);
}
