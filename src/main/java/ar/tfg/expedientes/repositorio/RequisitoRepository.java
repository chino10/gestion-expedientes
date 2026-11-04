package ar.tfg.expedientes.repositorio;

import ar.tfg.expedientes.dominio.Requisito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface RequisitoRepository extends JpaRepository<Requisito, Long> {

    /** Requisitos de un país y vía vigentes a una fecha. */
    @Query("""
            select r from Requisito r
            where r.pais = ?1 and r.via = ?2 and r.vigenteDesde <= ?3
              and (r.vigenteHasta is null or r.vigenteHasta >= ?3)
            order by r.id""")
    List<Requisito> buscarVigentes(String pais, String via, LocalDate fecha);

    /** Combinaciones de país y vía con requisitos vigentes, en formato "país|vía". */
    @Query("""
            select distinct concat(r.pais, '|', r.via) from Requisito r
            where r.vigenteDesde <= ?1 and (r.vigenteHasta is null or r.vigenteHasta >= ?1)""")
    List<String> buscarPaisesYViasVigentes(LocalDate fecha);
}
