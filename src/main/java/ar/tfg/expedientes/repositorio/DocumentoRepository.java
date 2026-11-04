package ar.tfg.expedientes.repositorio;

import ar.tfg.expedientes.dominio.Documento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    default Documento guardar(Documento documento) {
        return save(documento);
    }
}
